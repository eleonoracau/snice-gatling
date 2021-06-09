package io.snice.gatling.ss7.engine

import java.util.concurrent.ConcurrentHashMap

import com.typesafe.scalalogging.StrictLogging
import io.gatling.core.Predef.Status

import scala.collection.mutable

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

object Ss7Callbacks extends StrictLogging {

  private val responseCallbacks = new ConcurrentHashMap[RequestId, (Status, Long) => Unit](1000000)

  def addCallback(requestId: RequestId, handler: (Status, Long) => Unit): Unit = responseCallbacks.putIfAbsent(requestId, handler)

  def getThenRemove(requestId: RequestId): (Status, Long) => Unit = {
    val result = responseCallbacks.getOrDefault(requestId,
      (status:Status, timeNow: Long) => logger.warn(s"No request stored for response with transaction ID ${requestId.transactionId} and invoke ID ${requestId.invokeId}"))
    responseCallbacks.remove(requestId)
    result
  }
}
