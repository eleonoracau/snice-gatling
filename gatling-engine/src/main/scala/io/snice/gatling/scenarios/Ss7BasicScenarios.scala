package io.snice.gatling.scenarios

import io.gatling.core.Predef._
import io.snice.gatling.requests.Ss7Requests
import scala.concurrent.duration._

object Ss7BasicScenarios {

  val feeder = csv("data/imsis.csv").circular

  val basicSs7Purge = scenario("Ss7 Purge MS Scenario")
    .feed(feeder)
//    .pause(5.seconds)
    .exec(Ss7Requests.purgeMs)
}
