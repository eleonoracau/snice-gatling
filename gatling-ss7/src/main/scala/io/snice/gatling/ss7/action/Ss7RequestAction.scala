package io.snice.gatling.ss7.action

import io.gatling.commons.stats.Status
import io.gatling.commons.stats.OK
import io.gatling.commons.util.Clock
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.snice.gatling.ss7.engine.Ss7Client
import io.snice.gatling.ss7.request.Ss7RequestDef

case class Ss7RequestAction(reqDef: Ss7RequestDef,
                            client: Ss7Client,
                            clock: Clock,
                            statsEngine: StatsEngine,
                            next: Action) extends Ss7Action {

  override def name: String = "SS7"

  override def execute(session: Session): Unit = {
    val start = clock.nowMillis
    val name = reqDef.requestName.apply(session).toOption.get
    val imsi = reqDef.imsi.apply(session).toOption.get
    val requestName = reqDef.requestName

    val callback = (status: Status) => {
      val responseCode = if (status.equals(OK)) Some("SUCCESS") else Some("FAILURE")
      logger.info(s"IMSI: $imsi, responseCode: $responseCode")

      statsEngine.logResponse(session, name, start, clock.nowMillis, status, responseCode, Some(s"Received $requestName response for imsi $imsi"))
      next ! session
    }
    val applicationCtx = reqDef.mapRequestType.mapApplicationCtx
    client.sendRequest(imsi, applicationCtx, callback)
  }
}
