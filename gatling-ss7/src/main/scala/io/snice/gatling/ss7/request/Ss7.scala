package io.snice.gatling.ss7.request

import io.gatling.core.session.Expression

final case class Ss7(requestName: Expression[String]) {

  def emptyV1(imsi: Expression[String]): Ss7RequestBuilder = request(imsi, MapRequestType.emptyV1)
  def clr(imsi: Expression[String]): Ss7RequestBuilder = request(imsi, MapRequestType.clr)
  def air(imsi: Expression[String]): Ss7RequestBuilder = request(imsi, MapRequestType.air)
  def authenticationFailure(imsi: Expression[String]): Ss7RequestBuilder = request(imsi, MapRequestType.authenticationFailure)
  def pur(imsi: Expression[String]): Ss7RequestBuilder = request(imsi, MapRequestType.pur)
  def ulr(imsi: Expression[String]): Ss7RequestBuilder = request(imsi, MapRequestType.ulr)
  def gprsUlr(imsi: Expression[String]): Ss7RequestBuilder = request(imsi, MapRequestType.gprsUlr)
  def readyForSm(imsi: Expression[String]): Ss7RequestBuilder = request(imsi, MapRequestType.readyForSm)

  def request(imsi: Expression[String], mapType: MapRequestType): Ss7RequestBuilder =
    Ss7RequestBuilder(requestName, Ss7Attributes(imsi, mapType, List.empty))
}
