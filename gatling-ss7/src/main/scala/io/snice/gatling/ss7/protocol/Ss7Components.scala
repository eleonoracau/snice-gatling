package io.snice.gatling.ss7.protocol
import io.gatling.core.protocol.ProtocolComponents
import io.gatling.core.session.Session
import io.snice.gatling.ss7.engine.{Ss7Client, Ss7Engine}
import org.mobicents.protocols.api.IpChannelType.SCTP
import org.restcomm.protocols.ss7.indicator.RoutingIndicator
import org.restcomm.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl
import org.restcomm.protocols.ss7.sccp.parameter.SccpAddress

final case class Ss7Components(protocol: Ss7Protocol, client: Ss7Client) extends ProtocolComponents  {

  override def onStart: Session => Session = ProtocolComponents.NoopOnStart

  override def onExit: Session => Unit = ProtocolComponents.NoopOnExit

}
