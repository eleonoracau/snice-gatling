package io.snice.gatling.ss7.request
import io.gatling.core.session.{Expression, Session}
import io.snice.gatling.ss7.action.Ss7RequestActionBuilder
import org.restcomm.protocols.ss7.map.api.MAPApplicationContext
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextName.{authenticationFailureReportContext, gprsLocationUpdateContext, gprsNotifyContext, infoRetrievalContext, locationCancellationContext, msPurgingContext, mwdMngtContext, networkLocUpContext, shortMsgAlertContext, subscriberDataMngtContext}
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextVersion.{version1, version2, version3}

final case class Ss7Attributes(imsi: Expression[String],
                               mapType: MapRequestType,
                               ss7Parameters: List[Ss7Parameter])

trait Ss7Parameter {
  def apply(session: Session): Map[String, String]
}

object MapRequestType {
  def emptyV1: MapRequestType = MapRequestType(MAPApplicationContext.getInstance(shortMsgAlertContext, version1))
  def clr: MapRequestType = MapRequestType(MAPApplicationContext.getInstance(locationCancellationContext, version3))
  def air: MapRequestType = MapRequestType(MAPApplicationContext.getInstance(infoRetrievalContext, version3))
  def authenticationFailure:  MapRequestType = MapRequestType(MAPApplicationContext.getInstance(authenticationFailureReportContext, version3))

  def ulr: MapRequestType = MapRequestType(MAPApplicationContext.getInstance(networkLocUpContext, version2))
  def gprsUlr: MapRequestType = MapRequestType(MAPApplicationContext.getInstance(gprsLocationUpdateContext, version2))
  def insertSubscriberData: MapRequestType = MapRequestType(MAPApplicationContext.getInstance(subscriberDataMngtContext, version2))
  def readyForSm: MapRequestType = MapRequestType(MAPApplicationContext.getInstance(mwdMngtContext, version3))

  def nor: MapRequestType = MapRequestType(MAPApplicationContext.getInstance(gprsNotifyContext, version3))
  def pur: MapRequestType = MapRequestType(MAPApplicationContext.getInstance(msPurgingContext, version3))
}

final case class MapRequestType(mapApplicationCtx: MAPApplicationContext)

object Ss7RequestBuilder {

  implicit def toActionBuilder(requestBuilder: Ss7RequestBuilder): Ss7RequestActionBuilder = {
    println("Converting to an action builder")
    new Ss7RequestActionBuilder(requestBuilder)
  }

}

case class Ss7RequestBuilder(requestName: Expression[String], ss7Attributes: Ss7Attributes) {

  def build(): Ss7RequestDef = {
    Ss7RequestDef(requestName, ss7Attributes.imsi, ss7Attributes.ss7Parameters, ss7Attributes.mapType)
  }
}
