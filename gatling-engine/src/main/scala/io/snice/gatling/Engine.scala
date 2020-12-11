package io.snice.gatling

import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder
import io.snice.gatling.simulations.{Ss7ClrSimulation, UlrSimulation}

object Engine extends App {

  val props = new GatlingPropertiesBuilder()

  Option(System.getProperty("s")) match {
    case Some(simulation) => props.simulationClass(simulation)
    case None => props.simulationClass(classOf[Ss7ClrSimulation].getName)
  }

  Option(System.getProperty("rd")) match {
    case Some(description) => props.runDescription(description)
    case None =>
  }

  Gatling.fromMap(props.build)
}
