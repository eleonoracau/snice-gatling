package io.snice.gatling.simulations

import io.gatling.core.Predef._
import io.snice.gatling.Settings
import io.snice.gatling.gtp.Predef._
import io.snice.gatling.gtp.protocol.GtpProtocolBuilder
import io.snice.gatling.scenarios.MoIpCommandScenario
import org.slf4j.LoggerFactory

class MoIpCommandSimulation extends Simulation
  with Settings {

  private val log = LoggerFactory.getLogger(getClass)

  var gtpProtocol: GtpProtocolBuilder.GtpProtocolBuilder = gtp
    .localNattedAddress(getLocalAddress)
    .remoteEndpoint(getRemotePgwAddress)

  log.info(
    s"""Executing Mo Ip Command test with the following parameters:
       | - concurrent users: $getConcurrentUsers
       | - ramp up duration: $getRampUpDuration
       | - stable phase duration: $getStablePhaseDuration
       | - cool down duration: $getCoolDownDuration
       |""".stripMargin
  )

  setUp(MoIpCommandScenario.basicMoCommand(getLocalAddress).inject(atOnceUsers(3),
    // Warmup
    rampUsersPerSec(1) to getConcurrentUsers during getRampUpDuration,
    // Stable phase
    constantUsersPerSec(getConcurrentUsers) during getStablePhaseDuration,
    // Cool down
    rampUsersPerSec(getConcurrentUsers) to 1 during getCoolDownDuration
  )).protocols(gtpProtocol)
}
