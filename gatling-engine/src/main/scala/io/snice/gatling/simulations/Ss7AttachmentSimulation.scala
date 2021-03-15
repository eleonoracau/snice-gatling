package io.snice.gatling.simulations

import java.io.FileInputStream
import java.nio.file.{Files, Path}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.gatling.core.Predef._
import io.snice.gatling.config.Ss7Config
import io.snice.gatling.scenarios.Ss7BasicScenarios
import io.snice.gatling.ss7.protocol.Ss7Protocol

import scala.concurrent.duration._

class Ss7AttachmentSimulation extends Simulation {

  private val CONFIG_FILE_PATH = "./ss7_gatling_config.yml"
  val ss7Config = getConfig()
  var ss7: Ss7Protocol = Ss7Protocol(ss7Config.ss7EngineConfig)
  val simConfig = ss7Config.ss7SimulationConfig

  val interval = simConfig.intervalInMinutes.minutes
  val ss7Scenario = Ss7BasicScenarios.ss7Attach.inject(
    atOnceUsers(simConfig.atOnceUsers),
    constantUsersPerSec(simConfig.startConstantUsersPerSec) during interval,
    rampUsersPerSec(simConfig.rampRatePerSec) to simConfig.rampRateTarget during interval,
    constantUsersPerSec(simConfig.endConstantUsersPerSec).during(interval)
  )

  setUp(ss7Scenario).protocols(ss7).maxDuration(5.minutes)

  def getConfig(): Ss7Config = {
    prepareSimulation()
    val reader = new FileInputStream(CONFIG_FILE_PATH)
    val mapper = new ObjectMapper(new YAMLFactory())
    mapper.readValue(reader, classOf[Ss7Config])
  }

  def prepareSimulation(): Unit = {
    // copy default config file from 'resources' to local directory if does not exist
    if (!Files.exists(Path.of(CONFIG_FILE_PATH))) {
      val reader = getClass.getResourceAsStream("/ss7_config.yml")
      Files.copy(reader, Path.of(CONFIG_FILE_PATH))
    }
  }
}
