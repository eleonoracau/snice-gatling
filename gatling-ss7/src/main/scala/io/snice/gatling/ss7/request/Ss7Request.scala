package io.snice.gatling.ss7.request
import io.gatling.core.session.Expression
import io.snice.gatling.ss7.request.AdditionalParameterName.{AdditionalParameterName, AirNumberOfVectors}

import scala.util.Try

final case class Ss7RequestDef(requestName: Expression[String],
                               imsi: Expression[String],
                               ss7Parameters: List[Ss7Parameter],
                               mapRequestType: MapRequestType,
                               additionalParameters: collection.Map[AdditionalParameterName, String]) {

  def getNumberOfRequestedVectorsForAir(): Option[Int] = {
    additionalParameters.get(AirNumberOfVectors)
      .map(v => Try(v.toInt).get)
  }
}

final case class Ss7Request()
