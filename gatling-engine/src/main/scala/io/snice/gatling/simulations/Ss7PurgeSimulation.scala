package io.snice.gatling.simulations

import io.gatling.core.Predef._
import io.snice.gatling.scenarios.Ss7BasicScenarios
import io.snice.gatling.ss7.protocol.Ss7Protocol

import scala.concurrent.duration._

class Ss7PurgeSimulation extends Simulation {

  var ss7: Ss7Protocol = Ss7Protocol()

  setUp(Ss7BasicScenarios.basicSs7Purge.inject(
    atOnceUsers(10),
    constantUsersPerSec(10).during(1.minutes),
    rampUsersPerSec(10) to 200 during (1.minutes),
    constantUsersPerSec(10).during(1.minutes),
  )).protocols(ss7)

}
