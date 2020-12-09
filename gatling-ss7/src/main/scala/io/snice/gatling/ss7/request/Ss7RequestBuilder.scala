package io.snice.gatling.ss7.request
import io.gatling.core.session.{Expression, Session}
import io.snice.gatling.ss7.action.Ss7RequestActionBuilder
import org.restcomm.protocols.ss7.map.api.MAPApplicationContext
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextName.{locationCancellationContext, shortMsgAlertContext}
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextVersion.{version1, version2}

final case class Ss7Attributes(imsi: Expression[String],
                               mapType: MapRequestType,
                               ss7Parameters: List[Ss7Parameter])

trait Ss7Parameter {
  def apply(session: Session): Map[String, String]
}

object MapRequestType {
    def emptyV1: MapRequestType = MapRequestType(MAPApplicationContext.getInstance(shortMsgAlertContext, version1))
    def clr: MapRequestType = MapRequestType(MAPApplicationContext.getInstance(locationCancellationContext, version2))
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
