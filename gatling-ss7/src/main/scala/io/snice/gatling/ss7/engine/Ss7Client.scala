package io.snice.gatling.ss7.engine

import java.lang
import java.util.concurrent.ConcurrentHashMap

import com.typesafe.scalalogging.StrictLogging
import io.gatling.commons.stats.{KO, OK}
import io.gatling.core.Predef.{Status, clock, value2Expression}
import io.snice.gatling.ss7.protocol.Ss7Config
import io.snice.gatling.ss7.request.AdditionalParameterName.{AdditionalParameterName, AirNumberOfVectors}
import io.snice.gatling.ss7.request.Ss7RequestDef
import org.restcomm.protocols.ss7.map.api._
import org.restcomm.protocols.ss7.map.api.dialog._
import org.restcomm.protocols.ss7.map.api.errors.MAPErrorMessage
import org.restcomm.protocols.ss7.map.api.primitives.AddressNature.international_number
import org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.{ISDN, land_mobile}
import org.restcomm.protocols.ss7.map.api.primitives._
import org.restcomm.protocols.ss7.map.api.service.mobility.{MAPDialogMobility, MAPServiceMobilityListener, MobilityMessage}
import org.restcomm.protocols.ss7.map.api.service.mobility.authentication.{AccessType, AuthenticationFailureReportRequest, AuthenticationFailureReportResponse, FailureCause, RequestingNodeType, SendAuthenticationInfoRequest, SendAuthenticationInfoResponse}
import org.restcomm.protocols.ss7.map.api.service.mobility.faultRecovery.{ForwardCheckSSIndicationRequest, ResetRequest, RestoreDataRequest, RestoreDataResponse}
import org.restcomm.protocols.ss7.map.api.service.mobility.imei.{CheckImeiRequest, CheckImeiResponse}
import org.restcomm.protocols.ss7.map.api.service.mobility.locationManagement._
import org.restcomm.protocols.ss7.map.api.service.mobility.oam.{ActivateTraceModeRequest_Mobility, ActivateTraceModeResponse_Mobility}
import org.restcomm.protocols.ss7.map.api.service.mobility.subscriberInformation._
import org.restcomm.protocols.ss7.map.api.service.mobility.subscriberManagement.{DeleteSubscriberDataRequest, DeleteSubscriberDataResponse, InsertSubscriberDataRequest, InsertSubscriberDataResponse}
import org.restcomm.protocols.ss7.map.primitives.{ISDNAddressStringImpl, PlmnIdImpl}
import org.restcomm.protocols.ss7.tcap.asn.ApplicationContextName
import org.restcomm.protocols.ss7.tcap.asn.comp.Problem

import scala.collection.mutable
import scala.util.Try

object Ss7Client {

  def apply(config: Ss7Config): Ss7Client = {
    val engine = new Ss7Engine(config)
    val actualStack = engine.initializeStack(config.IP_CHANNEL_TYPE)
    val ss7Client = new Ss7Client(actualStack, config)
//    ss7Client.start()
    ss7Client
  }

}

class Ss7Client(mapStack: MAPStack,
                config: Ss7Config)
  extends MAPDialogListener
    with MAPServiceMobilityListener
    with StrictLogging {

  private lazy val mapProvider = {
    val mp = mapStack.getMAPProvider
    mp.addMAPDialogListener(this)
    mp.getMAPServiceMobility.addMAPServiceListener(this)
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

  class RequestId(val transactionId: Long, val invokeId: Long) {
    def canEqual(other: Any): Boolean = other.isInstanceOf[RequestId]

    override def equals(other: Any): Boolean = other match {
      case that: RequestId =>
        (that canEqual this) &&
          transactionId == that.transactionId &&
          invokeId == that.invokeId
      case _ => false
    }

    override def hashCode(): Int = {
      val state = Seq(transactionId, invokeId)
      state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
    }
  }

  private lazy val mapParameterFactory = mapProvider.getMAPParameterFactory

  private var responseCallbacks = new mutable.HashMap[RequestId, (Status, Long) => Unit]()

  def addCallback(requestId: RequestId, handler: (Status, Long) => Unit): Unit = responseCallbacks += (requestId -> handler)

  def sendEmptyV1Request(): Unit = {
    mapProvider.getMAPServiceSms.acivate()

    //todo: this should be moved to the RequestBuilder where the params for the request are created
    val appCnt = MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgAlertContext, MAPApplicationContextVersion.version1)
    val orginReference = mapParameterFactory.createAddressString(international_number, ISDN, config.ORIGIN)
    val destReference = mapParameterFactory.createAddressString(international_number, land_mobile, config.DEST)
    val clientDialogSms = mapProvider.getMAPServiceSms.createNewDialog(appCnt, config.LOCAL_ADDRESS, orginReference, config.REMOTE_ADDRESS, destReference)

    clientDialogSms.send()
  }

  def sendRequest(imsiStr: String, reqDef: Ss7RequestDef, callback: (Status, Long) => Unit): Unit = {

    val appCtx = reqDef.mapRequestType.mapApplicationCtx
    // First create Dialog
    val clientDialogMobility = mapProvider.getMAPServiceMobility.createNewDialog(appCtx, config.LOCAL_ADDRESS, null, config.REMOTE_ADDRESS, null)
    val imsi = mapParameterFactory.createIMSI(imsiStr)

    // add callback
    val transactionId = clientDialogMobility.getLocalDialogId
    val invokeId = addRequest(imsi, reqDef, clientDialogMobility, appCtx.getApplicationContextName);
    val requestId = new RequestId(transactionId, invokeId)
    addCallback(requestId, callback)

    clientDialogMobility.send()
  }

  def addRequest(imsi: IMSI, reqDef: Ss7RequestDef,
                 clientDialogMobility: MAPDialogMobility, appCtxName: MAPApplicationContextName): Long = {
    val invokeId = appCtxName match {
      case MAPApplicationContextName.msPurgingContext => addPurgeMsRequest(imsi, clientDialogMobility)
      case MAPApplicationContextName.infoRetrievalContext => addAuthenticationInfoRequest(imsi, reqDef, clientDialogMobility)
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

  def handleResponse(requestId: RequestId, status: Status): Unit = {
    val timeEnd = clock.nowMillis
    val callback = responseCallbacks.getOrElse(requestId, (status:Status, timeNow: Long) => logger.warn(s"No request stored for response with transaction ID ${requestId.transactionId} and invoke ID ${requestId.invokeId}"))
    callback.apply(status, timeEnd)
    responseCallbacks.remove(requestId)
  }

  def successResponseHandler[T <: MobilityMessage](response: T): Unit = {
    val requestId = new RequestId(response.getMAPDialog.getLocalDialogId, response.getInvokeId)
    handleResponse(requestId, OK)
  }

  override def onErrorComponent(mapDialog: MAPDialog, aLong: lang.Long, mapErrorMessage: MAPErrorMessage): Unit = {
    logger.debug(s"Map Error Code: ${mapErrorMessage.getErrorCode}")
    val requestId = new RequestId(mapDialog.getLocalDialogId, aLong)
    handleResponse(requestId, KO)
  }

  override def onCancelLocationResponse(cla: CancelLocationResponse): Unit = {
    logger.debug("onCancelLocationResponse")
    successResponseHandler(cla);
  }

  override def onPurgeMSResponse(purgeMSResponse: PurgeMSResponse): Unit = {
    logger.debug("onPurgeMSResponse")
    successResponseHandler(purgeMSResponse)
  }

  override def onUpdateLocationResponse(updateLocationResponse: UpdateLocationResponse): Unit = {
    logger.debug("onUpdateLocationResponse")
    successResponseHandler(updateLocationResponse)
  }

  override def onSendIdentificationResponse(sendIdentificationResponse: SendIdentificationResponse): Unit = {
    successResponseHandler(sendIdentificationResponse)
  }

  override def onAuthenticationFailureReportResponse(authenticationFailureReportResponse: AuthenticationFailureReportResponse): Unit = {
    successResponseHandler(authenticationFailureReportResponse)
  }

  override def onSendAuthenticationInfoResponse(sendAuthenticationInfoResponse: SendAuthenticationInfoResponse): Unit = {
    successResponseHandler(sendAuthenticationInfoResponse);
  }


  override def onDialogDelimiter(mapDialog: MAPDialog): Unit = logger.info("onDialogDelimiter")

  override def onMAPMessage(mapMessage: MAPMessage): Unit = logger.info("onMAPMessage")

  override def onDialogRequest(mapDialog: MAPDialog, addressString: AddressString, addressString1: AddressString, mapExtensionContainer: MAPExtensionContainer): Unit = logger.info("onDialogRequest")

  override def onDialogRequestEricsson(mapDialog: MAPDialog, addressString: AddressString, addressString1: AddressString, addressString2: AddressString, addressString3: AddressString): Unit = logger.info("onDialogRequestEricsson")

  override def onDialogAccept(mapDialog: MAPDialog, mapExtensionContainer: MAPExtensionContainer): Unit = logger.info("onDialogAccept")

  override def onDialogReject(mapDialog: MAPDialog, mapRefuseReason: MAPRefuseReason, applicationContextName: ApplicationContextName, mapExtensionContainer: MAPExtensionContainer): Unit = logger.info("onDialogReject")

  override def onDialogUserAbort(mapDialog: MAPDialog, mapUserAbortChoice: MAPUserAbortChoice, mapExtensionContainer: MAPExtensionContainer): Unit = logger.info("onDialogUserAbort")

  override def onDialogProviderAbort(mapDialog: MAPDialog, mapAbortProviderReason: MAPAbortProviderReason, mapAbortSource: MAPAbortSource, mapExtensionContainer: MAPExtensionContainer): Unit = logger.info("onDialogProviderAbort")

  override def onDialogClose(mapDialog: MAPDialog): Unit = logger.info(s"onDialogClose. LocalDialogId: ${mapDialog.getLocalDialogId}")

  override def onDialogNotice(mapDialog: MAPDialog, mapNoticeProblemDiagnostic: MAPNoticeProblemDiagnostic): Unit = logger.info("onDialogNotice")

  override def onDialogRelease(mapDialog: MAPDialog): Unit = logger.info(s"onDialogRelease. LocalDialogId: ${mapDialog.getLocalDialogId}")

  override def onDialogTimeout(mapDialog: MAPDialog): Unit = logger.info(s"onDialogTimeout. LocalDialogId: ${mapDialog.getLocalDialogId}")

  override def onUpdateLocationRequest(updateLocationRequest: UpdateLocationRequest): Unit = ???

  override def onCancelLocationRequest(cancelLocationRequest: CancelLocationRequest): Unit = ???

  override def onSendIdentificationRequest(sendIdentificationRequest: SendIdentificationRequest): Unit = ???

  override def onUpdateGprsLocationRequest(updateGprsLocationRequest: UpdateGprsLocationRequest): Unit = ???

  override def onUpdateGprsLocationResponse(updateGprsLocationResponse: UpdateGprsLocationResponse): Unit = ???

  override def onPurgeMSRequest(purgeMSRequest: PurgeMSRequest): Unit = ???

  override def onSendAuthenticationInfoRequest(sendAuthenticationInfoRequest: SendAuthenticationInfoRequest): Unit = ???

  override def onAuthenticationFailureReportRequest(authenticationFailureReportRequest: AuthenticationFailureReportRequest): Unit = ???

  override def onResetRequest(resetRequest: ResetRequest): Unit = ???

  override def onForwardCheckSSIndicationRequest(forwardCheckSSIndicationRequest: ForwardCheckSSIndicationRequest): Unit = ???

  override def onRestoreDataRequest(restoreDataRequest: RestoreDataRequest): Unit = ???

  override def onRestoreDataResponse(restoreDataResponse: RestoreDataResponse): Unit = ???

  override def onAnyTimeInterrogationRequest(anyTimeInterrogationRequest: AnyTimeInterrogationRequest): Unit = ???

  override def onAnyTimeInterrogationResponse(anyTimeInterrogationResponse: AnyTimeInterrogationResponse): Unit = ???

  override def onAnyTimeSubscriptionInterrogationRequest(anyTimeSubscriptionInterrogationRequest: AnyTimeSubscriptionInterrogationRequest): Unit = ???

  override def onAnyTimeSubscriptionInterrogationResponse(anyTimeSubscriptionInterrogationResponse: AnyTimeSubscriptionInterrogationResponse): Unit = ???

  override def onProvideSubscriberInfoRequest(provideSubscriberInfoRequest: ProvideSubscriberInfoRequest): Unit = ???

  override def onProvideSubscriberInfoResponse(provideSubscriberInfoResponse: ProvideSubscriberInfoResponse): Unit = ???

  override def onInsertSubscriberDataRequest(insertSubscriberDataRequest: InsertSubscriberDataRequest): Unit = ???

  override def onInsertSubscriberDataResponse(insertSubscriberDataResponse: InsertSubscriberDataResponse): Unit = ???

  override def onDeleteSubscriberDataRequest(deleteSubscriberDataRequest: DeleteSubscriberDataRequest): Unit = ???

  override def onDeleteSubscriberDataResponse(deleteSubscriberDataResponse: DeleteSubscriberDataResponse): Unit = ???

  override def onCheckImeiRequest(checkImeiRequest: CheckImeiRequest): Unit = ???

  override def onCheckImeiResponse(checkImeiResponse: CheckImeiResponse): Unit = ???

  override def onActivateTraceModeRequest_Mobility(activateTraceModeRequest_mobility: ActivateTraceModeRequest_Mobility): Unit = ???

  override def onActivateTraceModeResponse_Mobility(activateTraceModeResponse_mobility: ActivateTraceModeResponse_Mobility): Unit = ???

  override def onRejectComponent(mapDialog: MAPDialog, aLong: lang.Long, problem: Problem, b: Boolean): Unit = ???

  override def onInvokeTimeout(mapDialog: MAPDialog, aLong: lang.Long): Unit = ???

}
