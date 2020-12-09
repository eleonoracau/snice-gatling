package io.snice.gatling.ss7.action

import io.gatling.core.action.Action
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.structure.ScenarioContext
import io.snice.gatling.ss7.protocol.Ss7Protocol
import io.snice.gatling.ss7.request.Ss7RequestBuilder

class Ss7RequestActionBuilder(requestBuilder: Ss7RequestBuilder) extends ActionBuilder {

  override def build(ctx: ScenarioContext, next: Action): Action = {
    val ss7 = ctx.protocolComponentsRegistry.components(Ss7Protocol.ss7ProtocolKey)
    val client = ss7.client
    val statsEngine = ctx.coreComponents.statsEngine
    val clock = ctx.coreComponents.clock
    val ss7RequestDef = requestBuilder.build()
    Ss7RequestAction(ss7RequestDef, client, clock, statsEngine, next)
  }

}
