package io.snice.gatling.scenarios

import java.util.concurrent.ThreadLocalRandom

import io.gatling.core.Predef._
import io.snice.gatling.requests.Ss7Requests
import io.snice.gatling.requests.Ss7Requests.{CUSTOM_GT, NUMBER_OF_REQUESTED_VECTORS}

import scala.concurrent.duration._

object Ss7BasicScenarios {

  val feeder = csv("data/imsis.csv").random.circular

  val basicSs7Purge = scenario("Ss7 Purge MS Scenario")
    .feed(feeder)
    .exec(session => {
      // randomly inject number of requested vectors between 1 and 5(inclusive)
      val numberOfRequestVectors = (NUMBER_OF_REQUESTED_VECTORS, ThreadLocalRandom.current.nextInt(1, 6))
      val customGT = (CUSTOM_GT, "883260000000991") // different GT than the `localGT` specified in `config.yml` in gatling-ss7
      session.setAll(numberOfRequestVectors, customGT)
    })
//    .pause(5.seconds)
    .exec(Ss7Requests.air)
    .pause(2.seconds)
    .exec(Ss7Requests.gprsUlr)
    .pause(1.seconds)
    .exec(Ss7Requests.purgeMs)
    .pause(1.seconds)
    .exec(Ss7Requests.ulr)
    .pause(1.seconds)
    .exec(Ss7Requests.ulrWithDifferentGT)
}
