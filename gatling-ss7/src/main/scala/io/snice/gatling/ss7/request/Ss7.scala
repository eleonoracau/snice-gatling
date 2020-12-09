package io.snice.gatling.ss7.request

import io.gatling.core.session.Expression

final case class Ss7(requestName: Expression[String]) {

  def emptyV1(imsi: Expression[String]): Ss7RequestBuilder = request(imsi, MapRequestType.emptyV1)
  def clr(imsi: Expression[String]): Ss7RequestBuilder = request(imsi, MapRequestType.clr)

  def request(imsi: Expression[String], mapType: MapRequestType): Ss7RequestBuilder =
    Ss7RequestBuilder(requestName, Ss7Attributes(imsi, mapType, List.empty))

}
