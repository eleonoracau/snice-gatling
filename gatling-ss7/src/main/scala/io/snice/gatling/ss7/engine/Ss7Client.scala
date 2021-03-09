package io.snice.gatling.ss7.engine

import java.net.InetAddress

import com.typesafe.scalalogging.StrictLogging
import io.gatling.core.Predef.Status
import io.snice.gatling.ss7.protocol.Ss7Config
import io.snice.gatling.ss7.request.Ss7RequestDef
import org.restcomm.protocols.ss7.map.MAPParameterFactoryImpl
import org.restcomm.protocols.ss7.map.api._
import org.restcomm.protocols.ss7.map.api.primitives.AddressNature.international_number
import org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.{ISDN, land_mobile}
import org.restcomm.protocols.ss7.map.api.primitives._
import org.restcomm.protocols.ss7.map.api.service.mobility.MAPDialogMobility
import org.restcomm.protocols.ss7.map.api.service.mobility.authentication.RequestingNodeType
import org.restcomm.protocols.ss7.map.primitives.{GSNAddressImpl, ISDNAddressStringImpl, LMSIImpl, PlmnIdImpl}
import org.restcomm.protocols.ss7.map.service.mobility.locationManagement.{SGSNCapabilityImpl, VLRCapabilityImpl}
import org.restcomm.protocols.ss7.sccp.parameter.SccpAddress

object Ss7Client {

  def apply(config: Ss7Config): Ss7Client = {
    val engine = new Ss7Engine(config)

    val sgsnStack = engine.initializeStack(config.LOCAL_SGSN_SSN)
    val vlrStack = engine.initializeStack(config.LOCAL_VLR_SSN)
    val ss7Client = new Ss7Client(sgsnStack, vlrStack, config)
//    ss7Client.start()
    ss7Client
  }

}

class Ss7Client(sgsnStack: MAPStack,
                vlrStack: MAPStack,
                config: Ss7Config)
  extends StrictLogging {

  private val SGSN_MAP_PROVIDER = getMapProvider(sgsnStack)
  private val VLR_MAP_PROVIDER = getMapProvider(vlrStack)

  private var mapProvider = SGSN_MAP_PROVIDER
  private var sccpAddress: SccpAddress = config.LOCAL_ADDRESS_SGSN

  private val mapParameterFactory = new MAPParameterFactoryImpl
  private val callbacks = Ss7Callbacks

  def sendEmptyV1Request(): Unit = {
    mapProvider.getMAPServiceSms.acivate()

    //todo: this should be moved to the RequestBuilder where the params for the request are created
    val appCnt = MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgAlertContext, MAPApplicationContextVersion.version1)
    val orginReference = mapParameterFactory.createAddressString(international_number, ISDN, config.ORIGIN)
    val destReference = mapParameterFactory.createAddressString(international_number, land_mobile, config.DEST)
    val clientDialogSms = mapProvider.getMAPServiceSms.createNewDialog(appCnt, config.LOCAL_ADDRESS_SGSN, orginReference, config.REMOTE_ADDRESS, destReference)

    clientDialogSms.send()
  }

  def sendRequest(imsiStr: String, reqDef: Ss7RequestDef, callback: (Status, Long) => Unit): Unit = {

    val appCtx = reqDef.mapRequestType.mapApplicationCtx
    switchMapProviderIfNecessary(appCtx.getApplicationContextName)
    // First create Dialog
    val clientDialogMobility = mapProvider.getMAPServiceMobility.createNewDialog(appCtx, sccpAddress, null, config.REMOTE_ADDRESS, null)
    val imsi = mapParameterFactory.createIMSI(imsiStr)

    // add callback
    val transactionId = clientDialogMobility.getLocalDialogId
    val invokeId = addRequest(imsi, reqDef, clientDialogMobility, appCtx.getApplicationContextName)
    val requestId = new RequestId(transactionId, invokeId)
    callbacks.addCallback(requestId, callback)

    clientDialogMobility.send()
  }

  /**
   * Switch to SGSN MAP provider before sending Update-GPRS-LOCATION
   * and to VLR MAP provider before sending UPDATE-LOCATION
   */
  def switchMapProviderIfNecessary(appCtxName: MAPApplicationContextName): Unit = {
    appCtxName match {
      case MAPApplicationContextName.gprsLocationUpdateContext => {
        mapProvider = SGSN_MAP_PROVIDER
        sccpAddress = config.LOCAL_ADDRESS_SGSN
      }
      case MAPApplicationContextName.networkLocUpContext => {
        mapProvider = VLR_MAP_PROVIDER
        sccpAddress = config.LOCAL_ADDRESS_VLR
      }
      case default =>
    }
  }

  def addRequest(imsi: IMSI, reqDef: Ss7RequestDef,
                 clientDialogMobility: MAPDialogMobility, appCtxName: MAPApplicationContextName): Long = {
    val invokeId = appCtxName match {
      case MAPApplicationContextName.msPurgingContext => addPurgeMsRequest(imsi, clientDialogMobility)
      case MAPApplicationContextName.infoRetrievalContext => addAuthenticationInfoRequest(imsi, reqDef, clientDialogMobility)
      case MAPApplicationContextName.networkLocUpContext => addUpdateLocationRequest(imsi, clientDialogMobility)
      case MAPApplicationContextName.gprsLocationUpdateContext => addGprsUpdateLocationRequest(imsi, clientDialogMobility)
    }
    invokeId
  }

  def addPurgeMsRequest(imsi: IMSI, clientDialogMobility: MAPDialogMobility): Long = {
    val sgsnNumber = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "22228")
    clientDialogMobility.addPurgeMSRequest(imsi, null, sgsnNumber, null)
  }

  def addAuthenticationInfoRequest(imsi: IMSI, reqDef: Ss7RequestDef, clientDialogMobility: MAPDialogMobility): Long = {
    val numberOfRequestedVectors = reqDef.getNumberOfRequestedVectorsForAir().getOrElse(1)
    val segmentationProhibited = false
    val immediateResponsePreferred = false
    val reSynchronisationInfo = null
    val extensionContainer = null
    val plmnId = new PlmnIdImpl(1, 1)
    val additionalVectorsAreForEPS = true

    clientDialogMobility.addSendAuthenticationInfoRequest(imsi, numberOfRequestedVectors, segmentationProhibited, immediateResponsePreferred,
      reSynchronisationInfo, extensionContainer, RequestingNodeType.mmeSgsn, plmnId, numberOfRequestedVectors, additionalVectorsAreForEPS)
  }

  def addUpdateLocationRequest(imsi: IMSI, clientDialogMobility: MAPDialogMobility): Long = {
    val mscNumber = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, config.LOCAL_GT)
    val roamingNumber = null
    val vlrNumber = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, config.LOCAL_GT)
    val lmsi = new LMSIImpl(config.LMSI_STR)
    val mapExtensionContainer = null
    val vlrCapability = new VLRCapabilityImpl()
    val informPreviousNetworkEntity = false
    val csLCSNotSupportedByUE = false
    val gsnAddress = null

    val addInfo = null
    val pagingArea = null
    val skipSubscriberDataUpdate = false
    val restorationIndicator = false

    clientDialogMobility.addUpdateLocationRequest(imsi, mscNumber, roamingNumber, vlrNumber, lmsi, mapExtensionContainer, vlrCapability,
      informPreviousNetworkEntity, csLCSNotSupportedByUE, gsnAddress, addInfo, pagingArea, skipSubscriberDataUpdate, restorationIndicator)
  }

  def addGprsUpdateLocationRequest(imsi: IMSI, clientDialogMobility: MAPDialogMobility): Long = {
    val sgsnNumber = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, config.LOCAL_GT)
    val sgsnAddress = new GSNAddressImpl(GSNAddressAddressType.IPv4, InetAddress.getByName(config.LOCAL_IP).getAddress)
    val mapExtensionContainer = null
    val sgsnCapability = new SGSNCapabilityImpl()
    val informPreviousNetworkEntity = false
    val psLCSNotSupportedByUE = false
    val vGmlcAddress = null
    val addInfo = null
    val epsInfo = null
    val servingNodeTypeIndicator = false
    val skipSubscriberDataUpdate = false
    val usedRATType = null
    val gprsSubscriptionDataNotNeeded = true
    val nodeTypeIndicator = false
    val areaRestricted = false
    val ueReachableIndicator = true
    val epsSubscriptionDataNotNeeded = false
    val uesrvccCapability = null

    clientDialogMobility.addUpdateGprsLocationRequest(imsi, sgsnNumber, sgsnAddress, mapExtensionContainer, sgsnCapability, informPreviousNetworkEntity,
      psLCSNotSupportedByUE, vGmlcAddress, addInfo, epsInfo, servingNodeTypeIndicator, skipSubscriberDataUpdate, usedRATType, gprsSubscriptionDataNotNeeded,
      nodeTypeIndicator, areaRestricted, ueReachableIndicator, epsSubscriptionDataNotNeeded, uesrvccCapability)
  }

  def getMapProvider(mapStack: MAPStack): MAPProvider = {
    val mp = mapStack.getMAPProvider
    mp.addMAPDialogListener(Ss7Listener)
    mp.getMAPServiceMobility.addMAPServiceListener(Ss7Listener)
    //    mp.getMAPServiceSupplementary.addMAPServiceListener(this)
    //    mp.getMAPServiceSms.addMAPServiceListener(this)
    //    mp.getMAPServiceLsm.addMAPServiceListener(this)
    //    mp.getMAPServiceCallHandling.addMAPServiceListener(this)
    //    mp.getMAPServiceOam.addMAPServiceListener(this)
    //    mp.getMAPServicePdpContextActivation.addMAPServiceListener(this)
    mp.getMAPServiceMobility.acivate()
    //    mp.getMAPServiceSupplementary.acivate()
    //    mp.getMAPServiceSms.acivate()
    //    mp.getMAPServiceLsm.acivate()
    //    mp.getMAPServiceCallHandling.acivate()
    //    mp.getMAPServiceOam.acivate()
    //    mp.getMAPServicePdpContextActivation.acivate()
    mp
  }
}
