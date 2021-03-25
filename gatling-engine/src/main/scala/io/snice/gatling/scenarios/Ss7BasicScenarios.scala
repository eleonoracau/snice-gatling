package io.snice.gatling.scenarios

import io.gatling.core.Predef._
import io.snice.gatling.config.{Ss7Config, Ss7SimulationConfig}
import io.snice.gatling.requests.Ss7Requests
import io.snice.gatling.requests.Ss7Requests.{CUSTOM_GT, NUMBER_OF_REQUESTED_VECTORS}

import scala.concurrent.duration._
import scala.util.Random

object Ss7BasicScenarios {

  val feeder = csv("data/imsis.csv").random.circular
  val numberOfRequestedVectors = List.apply(1, 2, 3, 5)
  val random = new Random

  val ss7Attach = (simulationConfig: Ss7SimulationConfig) => scenario("Ss7 Attachment Scenario")
    .feed(feeder)
    .exec(session => {
      val numberOfRequestVectors = (NUMBER_OF_REQUESTED_VECTORS, numberOfRequestedVectors(random.nextInt(numberOfRequestedVectors.length)))
      val customGT = (CUSTOM_GT, simulationConfig.customGT)
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
