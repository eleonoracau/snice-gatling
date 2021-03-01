package io.snice.gatling.ss7.engine

import java.lang

import com.typesafe.scalalogging.StrictLogging
import io.gatling.commons.stats.{KO, OK}
import io.gatling.core.Predef.{Status, clock}
import io.snice.gatling.ss7.engine.Ss7Callbacks.RequestId
import org.restcomm.protocols.ss7.map.api.{MAPDialog, MAPDialogListener, MAPMessage}
import org.restcomm.protocols.ss7.map.api.dialog.{MAPAbortProviderReason, MAPAbortSource, MAPNoticeProblemDiagnostic, MAPRefuseReason, MAPUserAbortChoice}
import org.restcomm.protocols.ss7.map.api.errors.MAPErrorMessage
import org.restcomm.protocols.ss7.map.api.primitives.{AddressString, MAPExtensionContainer}
import org.restcomm.protocols.ss7.map.api.service.mobility.{MAPServiceMobilityListener, MobilityMessage}
import org.restcomm.protocols.ss7.map.api.service.mobility.authentication.{AuthenticationFailureReportRequest, AuthenticationFailureReportResponse, SendAuthenticationInfoRequest, SendAuthenticationInfoResponse}
import org.restcomm.protocols.ss7.map.api.service.mobility.faultRecovery.{ForwardCheckSSIndicationRequest, ResetRequest, RestoreDataRequest, RestoreDataResponse}
import org.restcomm.protocols.ss7.map.api.service.mobility.imei.{CheckImeiRequest, CheckImeiResponse}
import org.restcomm.protocols.ss7.map.api.service.mobility.locationManagement.{CancelLocationRequest, CancelLocationResponse, PurgeMSRequest, PurgeMSResponse, SendIdentificationRequest, SendIdentificationResponse, UpdateGprsLocationRequest, UpdateGprsLocationResponse, UpdateLocationRequest, UpdateLocationResponse}
import org.restcomm.protocols.ss7.map.api.service.mobility.oam.{ActivateTraceModeRequest_Mobility, ActivateTraceModeResponse_Mobility}
import org.restcomm.protocols.ss7.map.api.service.mobility.subscriberInformation.{AnyTimeInterrogationRequest, AnyTimeInterrogationResponse, AnyTimeSubscriptionInterrogationRequest, AnyTimeSubscriptionInterrogationResponse, ProvideSubscriberInfoRequest, ProvideSubscriberInfoResponse}
import org.restcomm.protocols.ss7.map.api.service.mobility.subscriberManagement.{DeleteSubscriberDataRequest, DeleteSubscriberDataResponse, InsertSubscriberDataRequest, InsertSubscriberDataResponse}
import org.restcomm.protocols.ss7.map.service.mobility.subscriberManagement.ODBGeneralDataImpl
import org.restcomm.protocols.ss7.tcap.asn.ApplicationContextName
import org.restcomm.protocols.ss7.tcap.asn.comp.Problem

object Ss7Listener extends MAPDialogListener
  with MAPServiceMobilityListener
  with StrictLogging {

  val callbacks = Ss7Callbacks

  def handleResponse(requestId: RequestId, status: Status): Unit = {
    val timeEnd = clock.nowMillis
    val callback = callbacks.getThenRemove(requestId)
    callback.apply(status, timeEnd)
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
    logger.debug(s"onUpdateLocationResponse, invoke ID:${updateLocationResponse.getInvokeId}")
    successResponseHandler(updateLocationResponse)
  }

  override def onUpdateGprsLocationResponse(updateGprsLocationResponse: UpdateGprsLocationResponse): Unit = {
    logger.debug(s"onUpdateGprsLocationResponse, invoke ID:${updateGprsLocationResponse.getInvokeId}")
    successResponseHandler(updateGprsLocationResponse)
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

  override def onInsertSubscriberDataRequest(insertSubscriberDataRequest: InsertSubscriberDataRequest): Unit = {
    val clientDialogMobility = insertSubscriberDataRequest.getMAPDialog
    val invokeId = insertSubscriberDataRequest.getInvokeId
    val bearerServiceList = insertSubscriberDataRequest.getBearerServiceList
    val teleServiceList = insertSubscriberDataRequest.getTeleserviceList
    val ssCodeList = null
    val odbGeneralData = new ODBGeneralDataImpl()
    val regionalSubscriptionResponse = null

    clientDialogMobility.addInsertSubscriberDataResponse(invokeId, teleServiceList, bearerServiceList, ssCodeList, odbGeneralData, regionalSubscriptionResponse)
    clientDialogMobility.send()
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
