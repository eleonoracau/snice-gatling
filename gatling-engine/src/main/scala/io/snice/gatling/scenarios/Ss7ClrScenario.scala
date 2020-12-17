package io.snice.gatling.scenarios

import io.gatling.core.Predef._
import io.snice.gatling.requests.Ss7CancelLocationRequest
import scala.concurrent.duration._

object Ss7ClrScenario {

  val feeder = csv("data/imsis.csv").circular

  val basicSs7Clr = scenario("Ss7 CLR Scenario")
    .feed(feeder)
//    .pause(5.seconds)
    .exec(Ss7CancelLocationRequest.cancelLocation)

}
