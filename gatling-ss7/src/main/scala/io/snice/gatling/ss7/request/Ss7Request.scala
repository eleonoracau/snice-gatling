package io.snice.gatling.ss7.request
import io.gatling.core.session.Expression
import io.snice.gatling.ss7.request.AdditionalParameterName.AdditionalParameterName

import scala.collection.mutable

final case class Ss7RequestDef(requestName: Expression[String],
                               imsi: Expression[String],
                               ss7Parameters: List[Ss7Parameter],
                               mapRequestType: MapRequestType,
                               additionalParameters: mutable.HashMap[AdditionalParameterName, Expression[String]])

final case class Ss7Request()
