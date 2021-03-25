package io.snice.gatling.simulations

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}
import com.typesafe.config.{ConfigObject, ConfigRenderOptions}
import io.gatling.core.Predef._
import io.gatling.core.config.GatlingConfiguration
import io.snice.gatling.config.Ss7Config
import io.snice.gatling.scenarios.Ss7BasicScenarios
import io.snice.gatling.ss7.protocol.Ss7Protocol

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class Ss7AttachmentSimulation extends Simulation {

  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(new Jdk8Module)
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)

  val gatlingConfig = implicitly[GatlingConfiguration]

  implicit val ss7Config = getSs7EngineConfig(gatlingConfig)
  var ss7: Ss7Protocol = Ss7Protocol(ss7Config.ss7EngineConfig)
  val simConfig = ss7Config.ss7SimulationConfig

  val interval = simConfig.intervalInMinutes.minutes
  val ss7Scenario = Ss7BasicScenarios.ss7Attach(simConfig).inject(
    atOnceUsers(simConfig.atOnceUsers),
    constantUsersPerSec(simConfig.startConstantUsersPerSec) during interval,
    rampUsersPerSec(simConfig.rampRatePerSec) to simConfig.rampRateTarget during interval,
    constantUsersPerSec(simConfig.endConstantUsersPerSec).during(interval)
  )

  setUp(ss7Scenario).protocols(ss7).maxDuration(5.minutes)

  def getSs7EngineConfig(configuration: GatlingConfiguration): Ss7Config = {
    val gatlingSs7Config = configuration.config.getObject("gatling").toConfig.getObject("ss7")
    mapper.readValue[Ss7Config](toJson(gatlingSs7Config))
  }

  def toJson(config: ConfigObject): String = {
    val map = config.entrySet().asScala.map(e => (e.getKey, e.getValue.render(ConfigRenderOptions.concise()))).toMap
    val json = mapper.writeValueAsString(map)
    // clean up json
    json.replace("\"", "").replace("\\", "\"")
  }
}
