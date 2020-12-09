package io.snice.gatling.ss7.engine

import java.lang

import com.typesafe.scalalogging.StrictLogging
import io.snice.gatling.ss7.protocol.Ss7Config
import org.restcomm.protocols.ss7.map.api._
import org.restcomm.protocols.ss7.map.api.dialog._
import org.restcomm.protocols.ss7.map.api.errors.MAPErrorMessage
import org.restcomm.protocols.ss7.map.api.primitives.AddressNature.international_number
import org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.{ISDN, land_mobile}
import org.restcomm.protocols.ss7.map.api.primitives._
import org.restcomm.protocols.ss7.map.api.service.mobility.MAPServiceMobilityListener
import org.restcomm.protocols.ss7.map.api.service.mobility.authentication.{AuthenticationFailureReportRequest, AuthenticationFailureReportResponse, SendAuthenticationInfoRequest, SendAuthenticationInfoResponse}
import org.restcomm.protocols.ss7.map.api.service.mobility.faultRecovery.{ForwardCheckSSIndicationRequest, ResetRequest, RestoreDataRequest, RestoreDataResponse}
import org.restcomm.protocols.ss7.map.api.service.mobility.imei.{CheckImeiRequest, CheckImeiResponse}
import org.restcomm.protocols.ss7.map.api.service.mobility.locationManagement._
import org.restcomm.protocols.ss7.map.api.service.mobility.oam.{ActivateTraceModeRequest_Mobility, ActivateTraceModeResponse_Mobility}
import org.restcomm.protocols.ss7.map.api.service.mobility.subscriberInformation._
import org.restcomm.protocols.ss7.map.api.service.mobility.subscriberManagement.{DeleteSubscriberDataRequest, DeleteSubscriberDataResponse, InsertSubscriberDataRequest, InsertSubscriberDataResponse}
import org.restcomm.protocols.ss7.map.datacoding.CBSDataCodingSchemeImpl
import org.restcomm.protocols.ss7.tcap.asn.ApplicationContextName
import org.restcomm.protocols.ss7.tcap.asn.comp.Problem

import scala.collection.mutable

object Ss7Client {

  def apply(config: Ss7Config): Ss7Client = {
    val engine = new Ss7Engine(config)
    val actualStack = engine.initializeStack(config.IP_CHANNEL_TYPE)
    new Ss7Client(actualStack, config)
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
    //    mp.getMAPServiceSupplementary.addMAPServiceListener(mapProvider)
    //    mp.getMAPServiceSms.addMAPServiceListener(mapProvider)
    //    mp.getMAPServiceLsm.addMAPServiceListener(mapProvider)
    //    mp.getMAPServiceCallHandling.addMAPServiceListener(mapProvider)
    //    mp.getMAPServiceOam.addMAPServiceListener(mapProvider)
    //    mp.getMAPServicePdpContextActivation.addMAPServiceListener(mapProvider)
    mp.getMAPServiceSupplementary.acivate()
    mp.getMAPServiceSms.acivate()
    mp.getMAPServiceMobility.acivate()
    mp.getMAPServiceLsm.acivate()
    mp.getMAPServiceCallHandling.acivate()
    mp.getMAPServiceOam.acivate()
    mp.getMAPServicePdpContextActivation.acivate()
    mp
  }

  private lazy val mapParameterFactory = mapProvider.getMAPParameterFactory

  private var clrResponseHandlers = new mutable.HashMap[String, CancelLocationResponse => Unit]()

  def addClrHandler(imsi: String, handler: CancelLocationResponse => Unit): Unit = clrResponseHandlers += (imsi -> handler)

  def start(): Unit = {
    mapProvider.getMAPServiceSupplementary.acivate()

    //todo: this should be moved to the RequestBuilder where the params for the request are created
    val appCnt: MAPApplicationContext = MAPApplicationContext.getInstance(MAPApplicationContextName.networkUnstructuredSsContext, MAPApplicationContextVersion.version2)
    val orginReference: AddressString = mapParameterFactory.createAddressString(international_number, ISDN, config.ORIGIN)
    val destReference: AddressString = mapParameterFactory.createAddressString(international_number, land_mobile, config.DEST)
    val msisdn: ISDNAddressString = mapParameterFactory.createISDNAddressString(international_number, ISDN, config.MSISDN_STR)
    val clientDialog = mapProvider.getMAPServiceSupplementary.createNewDialog(appCnt, config.LOCAL_ADDRESS, orginReference, config.REMOTE_ADDRESS, destReference)
    val ussd: USSDString = mapParameterFactory.createUSSDString(config.USSD_STR)

    clientDialog.addProcessUnstructuredSSRequest(new CBSDataCodingSchemeImpl(0x0f), ussd, null, msisdn)
    clientDialog.send()
  }

  def sendEmptyV1Request(): Unit = {
    mapProvider.getMAPServiceSms.acivate()

    //todo: this should be moved to the RequestBuilder where the params for the request are created
    val appCnt = MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgAlertContext, MAPApplicationContextVersion.version1)
    val orginReference = mapParameterFactory.createAddressString(international_number, ISDN, config.ORIGIN)
    val destReference = mapParameterFactory.createAddressString(international_number, land_mobile, config.DEST)
    val clientDialogSms = mapProvider.getMAPServiceSms.createNewDialog(appCnt, config.LOCAL_ADDRESS, orginReference, config.REMOTE_ADDRESS, destReference)

    clientDialogSms.send()
  }

  def sendCancelLocation(imsiStr: String): Unit = {
    mapProvider.getMAPServiceMobility.acivate()

    //todo: this should be moved to the RequestBuilder where the params for the request are created
    val appCnt = MAPApplicationContext.getInstance(MAPApplicationContextName.locationCancellationContext, MAPApplicationContextVersion.version2)
    val clientDialogMobility = mapProvider.getMAPServiceMobility.createNewDialog(appCnt, config.LOCAL_ADDRESS, null, this.config.REMOTE_ADDRESS, null)
    val imsi = mapParameterFactory.createIMSI(config.IMSI_STR)
    val lmsi = mapParameterFactory.createLMSI(config.LMSI_STR)
    val imsiWithLmsi = mapParameterFactory.createIMSIWithLMSI(imsi, lmsi)

    clientDialogMobility.addCancelLocationRequest(imsi, imsiWithLmsi, config.CANCELLATION_TYPE, null, null, false, false, null, null, null)
    clientDialogMobility.send()
  }

  override def onCancelLocationResponse(cla: CancelLocationResponse): Unit = clrResponseHandlers.apply(cla.getInvokeId.toString).apply(cla)

  override def onDialogDelimiter(mapDialog: MAPDialog): Unit = logger.info("onDialogDelimiter")

  override def onDialogRequest(mapDialog: MAPDialog, addressString: AddressString, addressString1: AddressString, mapExtensionContainer: MAPExtensionContainer): Unit = logger.info("onDialogRequest")

  override def onDialogRequestEricsson(mapDialog: MAPDialog, addressString: AddressString, addressString1: AddressString, addressString2: AddressString, addressString3: AddressString): Unit = logger.info("onDialogRequestEricsson")

  override def onDialogAccept(mapDialog: MAPDialog, mapExtensionContainer: MAPExtensionContainer): Unit = logger.info("onDialogAccept")

  override def onDialogReject(mapDialog: MAPDialog, mapRefuseReason: MAPRefuseReason, applicationContextName: ApplicationContextName, mapExtensionContainer: MAPExtensionContainer): Unit = logger.info("onDialogReject")

  override def onDialogUserAbort(mapDialog: MAPDialog, mapUserAbortChoice: MAPUserAbortChoice, mapExtensionContainer: MAPExtensionContainer): Unit = logger.info("onDialogUserAbort")

  override def onDialogProviderAbort(mapDialog: MAPDialog, mapAbortProviderReason: MAPAbortProviderReason, mapAbortSource: MAPAbortSource, mapExtensionContainer: MAPExtensionContainer): Unit = logger.info("onDialogProviderAbort")

  override def onDialogClose(mapDialog: MAPDialog): Unit = logger.info("onDialogClose")

  override def onDialogNotice(mapDialog: MAPDialog, mapNoticeProblemDiagnostic: MAPNoticeProblemDiagnostic): Unit = logger.info("onDialogNotice")

  override def onDialogRelease(mapDialog: MAPDialog): Unit = logger.info("onDialogRelease")

  override def onDialogTimeout(mapDialog: MAPDialog): Unit = logger.info("onDialogTimeout")

  override def onUpdateLocationRequest(updateLocationRequest: UpdateLocationRequest): Unit = ???

  override def onUpdateLocationResponse(updateLocationResponse: UpdateLocationResponse): Unit = ???

  override def onCancelLocationRequest(cancelLocationRequest: CancelLocationRequest): Unit = ???

  override def onSendIdentificationRequest(sendIdentificationRequest: SendIdentificationRequest): Unit = ???

  override def onSendIdentificationResponse(sendIdentificationResponse: SendIdentificationResponse): Unit = ???

  override def onUpdateGprsLocationRequest(updateGprsLocationRequest: UpdateGprsLocationRequest): Unit = ???

  override def onUpdateGprsLocationResponse(updateGprsLocationResponse: UpdateGprsLocationResponse): Unit = ???

  override def onPurgeMSRequest(purgeMSRequest: PurgeMSRequest): Unit = ???

  override def onPurgeMSResponse(purgeMSResponse: PurgeMSResponse): Unit = ???

  override def onSendAuthenticationInfoRequest(sendAuthenticationInfoRequest: SendAuthenticationInfoRequest): Unit = ???

  override def onSendAuthenticationInfoResponse(sendAuthenticationInfoResponse: SendAuthenticationInfoResponse): Unit = ???

  override def onAuthenticationFailureReportRequest(authenticationFailureReportRequest: AuthenticationFailureReportRequest): Unit = ???

  override def onAuthenticationFailureReportResponse(authenticationFailureReportResponse: AuthenticationFailureReportResponse): Unit = ???

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

  override def onErrorComponent(mapDialog: MAPDialog, aLong: lang.Long, mapErrorMessage: MAPErrorMessage): Unit = ???

  override def onRejectComponent(mapDialog: MAPDialog, aLong: lang.Long, problem: Problem, b: Boolean): Unit = ???

  override def onInvokeTimeout(mapDialog: MAPDialog, aLong: lang.Long): Unit = ???

  override def onMAPMessage(mapMessage: MAPMessage): Unit = ???
}
