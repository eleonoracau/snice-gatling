package io.snice.gatling.scenarios

import java.util.concurrent.ThreadLocalRandom

import io.gatling.core.Predef._
import io.snice.gatling.requests.Ss7Requests

import scala.concurrent.duration._

object Ss7BasicScenarios {

  val feeder = csv("data/imsis.csv").random.circular

  val basicSs7Purge = scenario("Ss7 Purge MS Scenario")
    .feed(feeder)
    .exec(session => {
      val numberOfRequestVectors = ThreadLocalRandom.current.nextInt(1, 6)
      session.set("airNumberOfVectors", numberOfRequestVectors)
    })
//    .pause(5.seconds)
    .exec(Ss7Requests.purgeMs)
    .pause(1.seconds)
    .exec(Ss7Requests.air)
}
