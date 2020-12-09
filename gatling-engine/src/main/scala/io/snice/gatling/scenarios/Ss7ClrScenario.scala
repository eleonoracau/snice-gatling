package io.snice.gatling.scenarios

import io.gatling.core.Predef._
import io.snice.gatling.requests.Ss7CancelLocationRequest

object Ss7ClrScenario {

  val feeder = csv("data/imsis.csv").circular

  val basicSs7Clr = scenario("Ss7 CLR Scenario")
    .feed(feeder)
    .exec(Ss7CancelLocationRequest.cancelLocation)

}
