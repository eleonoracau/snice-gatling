package io.snice.gatling.simulations

import java.net.URI

import io.gatling.core.Predef.{Simulation, configuration}
import io.gatling.core.config.GatlingConfiguration
import io.snice.gatling.diameter.Predef.diameter
import io.snice.gatling.diameter.engine.DiameterEngine
import io.snice.gatling.diameter.protocol.DiameterProtocolBuilder

class StartDiameterStack extends Simulation {

  val gatlingConfig = implicitly[GatlingConfiguration]

  val diameterProtocol = getDiameterStack()

  DiameterEngine.apply(diameterProtocol.build.config).start(diameterProtocol.build.peers)

  // otherwise it will throw a no simulation scenario exception
  while(true) {
    Thread.sleep(5000)
  }

  def getDiameterStack(): DiameterProtocolBuilder = {
    diameter
      .originHost("snice.node.epc.mnc001.mcc001.3gppnetwork.org")
      .originRealm("epc.mnc001.mcc001.3gppnetwork.org")
  }
}