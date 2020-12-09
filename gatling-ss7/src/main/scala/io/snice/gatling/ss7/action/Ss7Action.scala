package io.snice.gatling.ss7.action

import io.gatling.commons.stats.{KO, OK}
import io.gatling.core.action.ChainableAction
import io.gatling.core.session.{Expression, Session}
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

import scala.util.Try

trait Ss7Action extends ChainableAction with NameGen {

  def log(start: Long, end: Long, tried: Try[_], requestName: Expression[String], session: Session, statsEngine: StatsEngine): Session = {
    val (status, message) = tried match {
      case scala.util.Success(_) => (OK, None)
      case scala.util.Failure(exception) => (KO, Some(exception.getMessage))
    }
    requestName.apply(session).foreach { resolvedRequestName =>
      statsEngine.logResponse(session, resolvedRequestName, start, end, status, None, message)
    }
    session.logGroupRequestTimings(start, end)
  }

}
