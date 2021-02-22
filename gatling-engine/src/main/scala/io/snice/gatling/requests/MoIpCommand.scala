package io.snice.gatling.requests

import io.gatling.core.Predef._
import io.snice.buffer.{Buffer, Buffers}
import io.snice.gatling.gtp.Predef._
import io.snice.gatling.gtp.data._
import io.snice.gatling.gtp.request.DataRequestBuilder

object MoCommand {

  def moRequest(payload: String): DataRequestBuilder[MoMessage] = gtp("Send Mo IP Command")
    .data(MoMessage(Buffers.wrap(payload)))
    .encoder(new MoEncoder)
    .decoder(new MoDecoder)
    .localPort(12345)
    .remoteAddress("100.64.0.1") //magic IP
    .remotePort(33445)
}

final class MoEncoder extends DataEncoder[MoMessage] {
  override def encode(request: MoMessage): Buffer = request.query
}

final class MoDecoder extends DataDecoder[MoMessage] {
  override def decode(raw: Buffer): MoMessage = MoMessage(raw)
}

final case class MoMessage(query: Buffer) extends TransactionSupport {
  private val id = BufferTransactionId(query.slice(2))

  override def transactionId: TransactionId = id
}
