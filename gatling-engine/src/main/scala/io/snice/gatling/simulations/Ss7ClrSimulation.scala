package io.snice.gatling.simulations

import io.gatling.core.Predef._
import io.snice.gatling.scenarios.Ss7ClrScenario
import io.snice.gatling.ss7.protocol.Ss7Protocol

class Ss7ClrSimulation extends Simulation {

  var ss7: Ss7Protocol = Ss7Protocol()

  setUp(Ss7ClrScenario.basicSs7Clr.inject(atOnceUsers(1),
    // constantUsersPerSec(1).during(60.seconds),
    // rampUsersPerSec(10) to 100 during (1.minutes),
    // constantUsersPerSec(100).during(1.minutes),
  )).protocols(ss7)

}
