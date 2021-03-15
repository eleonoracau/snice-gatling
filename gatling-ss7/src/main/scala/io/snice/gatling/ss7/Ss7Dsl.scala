package io.snice.gatling.ss7

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Expression
import io.snice.gatling.ss7.protocol.Ss7Protocol
import io.snice.gatling.ss7.request.Ss7

trait Ss7Dsl {

  //def ss7(implicit configuration: GatlingConfiguration): Ss7Protocol = Ss7Protocol(new Ss7Config())

  def ss7(requestName: Expression[String]): Ss7 = Ss7(requestName)

}
