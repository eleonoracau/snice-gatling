package io.snice.gatling.ss7.request
import io.gatling.core.session.Expression

final case class Ss7RequestDef(requestName: Expression[String],
                               imsi: Expression[String],
                               ss7Parameters: List[Ss7Parameter],
                               mapRequestType: MapRequestType)

final case class Ss7Request()
