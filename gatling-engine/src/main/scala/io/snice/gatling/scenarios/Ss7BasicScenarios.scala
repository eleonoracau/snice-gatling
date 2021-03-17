package io.snice.gatling.scenarios

import java.util.concurrent.ThreadLocalRandom

import io.gatling.core.Predef._
import io.snice.gatling.requests.Ss7Requests
import io.snice.gatling.requests.Ss7Requests.{CUSTOM_GT, NUMBER_OF_REQUESTED_VECTORS}

import scala.concurrent.duration._
import scala.util.Random

object Ss7BasicScenarios {

  val feeder = csv("data/imsis.csv").random.circular
  val customGT = (CUSTOM_GT, "883260000000991") // different GT than the `localGT` specified in `default_engine_config.yml` in gatling-ss7
  val numberOfRequestedVectors = List.apply(1, 2, 3, 5)
  val random = new Random

  val ss7Attach = scenario("Ss7 Attachment Scenario")
    .feed(feeder)
    .exec(session => {
      val numberOfRequestVectors = (NUMBER_OF_REQUESTED_VECTORS, numberOfRequestedVectors(random.nextInt(numberOfRequestedVectors.length)))
      session.setAll(numberOfRequestVectors, customGT)
    })
//    .pause(5.seconds)
    .exec(Ss7Requests.air)
    .pause(1.seconds)
    .exec(Ss7Requests.ulr)
    .pause(1.seconds)
    .exec(Ss7Requests.air)
    .pause(1.seconds)
    .exec(Ss7Requests.gprsUlr)
    .pause(1.seconds)
    .exec(Ss7Requests.ulrWithDifferentGT)
}
