package io.snice.gatling.ss7.action

import io.gatling.commons.stats.Status
import io.gatling.commons.stats.OK
import io.gatling.commons.util.Clock
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.snice.gatling.ss7.engine.Ss7Client
import io.snice.gatling.ss7.request.Ss7RequestBuilder

case class Ss7RequestAction(requestBuilder: Ss7RequestBuilder,
                            client: Ss7Client,
                            clock: Clock,
                            statsEngine: StatsEngine,
                            next: Action) extends Ss7Action {

  override def name: String = "SS7"

  override def execute(session: Session): Unit = {
    val reqDef = requestBuilder.withSession(session).build()
    val start = clock.nowMillis
    val name = reqDef.requestName.apply(session).toOption.get
    val imsi = reqDef.imsi.apply(session).toOption.get

    val callback = (status: Status, timeEnd: Long) => {
      val responseCode = if (status.equals(OK)) Some("SUCCESS") else Some("FAILURE")
      statsEngine.logResponse(session, name, start, timeEnd, status, responseCode, Some(s"Received $name response"))
      next ! session
    }
    client.sendRequest(imsi, reqDef, callback)
  }
}
