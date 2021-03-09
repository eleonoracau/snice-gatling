package io.snice.gatling.requests

import io.gatling.core.Predef._
import io.snice.gatling.ss7.Predef.ss7
import io.snice.gatling.ss7.request.Ss7RequestBuilder

object Ss7Requests {

  val purgeMs: Ss7RequestBuilder =
    ss7("purgeMs")
      .pur("${imsi}")

  val air: Ss7RequestBuilder =
    ss7("authenticationInfo")
      .air("${imsi}")
      .numberOfRequestedVectorsForAir("${airNumberOfVectors}")

  val ulr: Ss7RequestBuilder =
    ss7("updateLocation")
      .ulr("${imsi}")

  val gprsUlr: Ss7RequestBuilder =
    ss7("gprsUpdateLocation")
      .gprsUlr("${imsi}")
}
