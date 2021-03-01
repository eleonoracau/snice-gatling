package io.snice.gatling.ss7.engine

import com.typesafe.scalalogging.StrictLogging
import io.gatling.core.Predef.Status

import scala.collection.mutable

object Ss7Callbacks extends StrictLogging {
  class RequestId(val transactionId: Long, val invokeId: Long) {
    def canEqual(other: Any): Boolean = other.isInstanceOf[RequestId]

    override def equals(other: Any): Boolean = other match {
      case that: RequestId =>
        (that canEqual this) &&
          transactionId == that.transactionId &&
          invokeId == that.invokeId
      case _ => false
    }

    override def hashCode(): Int = {
      val state = Seq(transactionId, invokeId)
      state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
    }
  }

  private val responseCallbacks = new mutable.HashMap[RequestId, (Status, Long) => Unit]()

  def addCallback(requestId: RequestId, handler: (Status, Long) => Unit): Unit = responseCallbacks += (requestId -> handler)

  def getThenRemove(requestId: RequestId): (Status, Long) => Unit = {
    val result = responseCallbacks.getOrElse(requestId,
      (status:Status, timeNow: Long) => logger.warn(s"No request stored for response with transaction ID ${requestId.transactionId} and invoke ID ${requestId.invokeId}"))
    responseCallbacks.remove(requestId)
    result
  }
}
