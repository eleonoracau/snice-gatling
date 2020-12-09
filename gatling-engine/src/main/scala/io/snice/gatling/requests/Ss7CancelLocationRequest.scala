package io.snice.gatling.requests

import io.gatling.core.Predef._
import io.snice.gatling.ss7.Predef.ss7
import io.snice.gatling.ss7.request.Ss7RequestBuilder

object Ss7CancelLocationRequest {

  val cancelLocation: Ss7RequestBuilder =
    ss7("cancelLocation")
    .clr("${imsi}")

}
