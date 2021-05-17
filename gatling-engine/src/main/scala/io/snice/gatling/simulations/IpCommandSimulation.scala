package io.snice.gatling.simulations

import io.gatling.core.Predef._
import io.snice.gatling.Settings
import io.snice.gatling.diameter.Predef.diameter
import io.snice.gatling.gtp.Predef._
import io.snice.gatling.gtp.protocol.GtpProtocolBuilder
import io.snice.gatling.scenarios.{MoCommandScenario, MtCommandScenario}
import io.snice.networking.diameter.peer.{Peer, PeerConfiguration}
import org.slf4j.LoggerFactory
import io.gatling.core.structure.ScenarioBuilder

import java.net.URI
import scala.concurrent.duration.DurationInt

class IpCommandSimulation extends Simulation
  with Settings {

  private val log = LoggerFactory.getLogger(getClass)

  var gtpProtocol: GtpProtocolBuilder.GtpProtocolBuilder = gtp
    .localNattedAddress(getLocalAddress)
    .remoteEndpoint(getPgwAddress)

  private val peerConfig = {
    val cfg = new PeerConfiguration()
    cfg.setName("pgw")
    cfg.setMode(Peer.MODE.ACTIVE)
    cfg.setUri(new URI(s"aaa://$getPgwAddress:3868"))
    cfg
  }

  private val diameterProtocol = diameter
    .originHost("snice1.dev-us1.node.epc.mnc062.mcc901.3gppnetwork.org")
    .originRealm("epc.mnc062.mcc901.3gppnetwork.org")
    .peer(peerConfig)


  log.info(
    s"""Executing Mo Ip Command test with the following parameters:
       | - concurrent users: $getConcurrentUsers
       | - ramp up duration: $getRampUpDuration
       | - stable phase duration: $getStablePhaseDuration
       | - cool down duration: $getCoolDownDuration
       |""".stripMargin
  )

  private val mixedScenario: ScenarioBuilder = scenario(getClass.getSimpleName)
    .pause(3.seconds)
    .roundRobinSwitch(
      exec(MoCommandScenario.basicMoCommand(getLocalAddress)),
      exec(MtCommandScenario.basicMtCommand(getLocalAddress))
    )

  setUp(mixedScenario.inject(
    (rampConcurrentUsers(1) to getConcurrentUsers).during(getRampUpDuration),
    // Stable phase
    constantConcurrentUsers(getConcurrentUsers).during(getStablePhaseDuration),
    // Cool down
    (rampConcurrentUsers(getConcurrentUsers) to 1).during(getCoolDownDuration)
  )).protocols(gtpProtocol, diameterProtocol).maxDuration(getMaxDuration)
}
