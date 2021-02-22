package io.snice.gatling.gtp.action

import io.gatling.commons.util.Clock
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.snice.gatling.gtp.engine.GtpEngine
import io.snice.gatling.gtp.request.DataRequestDef
import io.snice.networking.gtp.impl.{DefaultEpsBearer, InternalGtpUserTunnel}
import io.snice.networking.gtp.{EpsBearer, PdnSessionContext}

object DataRequestAction {

}

case class DataRequestAction[T](reqDef: DataRequestDef[T],
                                engine: GtpEngine,
                                clock: Clock,
                                statsEngine: StatsEngine,
                                next: Action) extends GtpAction {
  override def name: String = "Data"

  override def execute(session: Session): Unit = {
    val start = clock.nowMillis

    val bearer = session.attributes.get(GtpRequestAction.PDN_SESSION_CTX_KEY)
      .map(ctx => establishBearer(ctx.asInstanceOf[PdnSessionContext], reqDef.dataAttributes.localPort))

    val newSession = bearer match {
      case Some(bearer) => {
        val buffer = reqDef.dataAttributes.encoder.encode(reqDef.dataAttributes.data)
        bearer.send(reqDef.dataAttributes.remoteIpAddress, reqDef.dataAttributes.remotePort, buffer)
        session
      }
      case None => {
        val message = "Unable to establish bearer due to missing IEs in CreateSessionResponse"
        val responseCode = None
        val responseStopTime = clock.nowMillis
        val failed = session.markAsFailed
        statsEngine.logResponse(session, name, start, responseStopTime, failed.status, responseCode, Some(message))
        failed
      }
    }

    next ! newSession
  }

  private def establishBearer(ctx: PdnSessionContext, localPort: Int): EpsBearer = {
    System.err.println("Establish GTP User Tunnel")
    val remote = ctx.getDefaultRemoteBearer.getIPv4AddressAsString.get
    val address = engine.translateAddress(remote)
    val tunnel = engine.establishGtpUserTunnel(address)
    DefaultEpsBearer.create(tunnel.asInstanceOf[InternalGtpUserTunnel], ctx, localPort)
  }
}
