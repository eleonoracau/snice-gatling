package io.snice.gatling.scenarios

import io.gatling.core.Predef._
import io.gatling.core.feeder.BatchableFeederBuilder
import io.gatling.core.structure.ScenarioBuilder
import io.snice.gatling.requests.{CreateSessionRequest, DeleteSessionRequest, MoCommand}

import scala.concurrent.duration._

object MoIpCommandScenario {

  val feeder: BatchableFeederBuilder[String]#F = csv("data/mo-command-imsis.csv").circular

  def basicMoCommand(localAddress: String): ScenarioBuilder = scenario("Send Mo IP command")
    .feed(feeder)
    .exec(CreateSessionRequest.csr(localAddress))
    .pause(1.seconds)
    .exec(MoCommand.moRequest("Hello Ip Command"))
    .exec(session => session.markAsSucceeded)
    .pause(1.seconds)
    .exec(DeleteSessionRequest.dsrBase)
}
