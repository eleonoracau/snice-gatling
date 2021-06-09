package io.snice.gatling.scenarios

import io.gatling.core.Predef._
import io.snice.gatling.config.Ss7SimulationConfig
import io.snice.gatling.requests.Ss7Requests
import io.snice.gatling.requests.Ss7Requests.{CUSTOM_GT, NUMBER_OF_REQUESTED_VECTORS}

import scala.concurrent.duration._
import scala.util.Random
import scala.collection.immutable._

object Ss7BasicScenarios {

  val numberOfRequestedVectors = List.apply(1, 2, 3, 5)
  val random = new Random

  val feeder = (simulationConfig: Ss7SimulationConfig) => {
    val feederArray = (simulationConfig.imsiStart to simulationConfig.imsiEnd).map {imsi =>
      val imsiString = imsi.toString

      // converting IMSIs start with 0 to Long  truncates the leading 0s, so this adds leading zeros back if necessary
      val processedImsi = if (imsiString.length < 15) "0" * (15 - imsiString.length) + imsiString else imsiString
      Map(
        "imsi" -> processedImsi,
        NUMBER_OF_REQUESTED_VECTORS -> numberOfRequestedVectors(random.nextInt(numberOfRequestedVectors.length)),
        CUSTOM_GT -> simulationConfig.customGT
      )
    }.toArray
    array2FeederBuilder(feederArray).circular
  }

  val ss7Attach = (simulationConfig: Ss7SimulationConfig) => scenario("Ss7 Attachment Scenario")
    .feed(feeder(simulationConfig))
//    .pause(5.seconds)
    .exec(Ss7Requests.air)
    .pause(1.seconds)
    .exec(Ss7Requests.ulr)
    .pause(1.seconds)
    //.exec(Ss7Requests.purgeMs)
    //.pause(1.seconds)
    .exec(Ss7Requests.gprsUlr)
  /*
    .pause(1.seconds)
    .exec(Ss7Requests.clr)

   */
}
