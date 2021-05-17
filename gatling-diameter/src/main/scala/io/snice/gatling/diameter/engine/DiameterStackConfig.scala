package io.snice.gatling.diameter.engine

import io.snice.gatling.diameter.protocol.DiameterConfig
import io.snice.networking.diameter.DiameterAppConfig

import scala.collection.JavaConverters._

object DiameterStackConfig {

  def apply(conf: DiameterConfig): DiameterStackConfig = {

    // TODO: we need to convert all of this. Perhaps I should just go back to
    // using the yaml instead of this annoying translation stuff...
    /*
    val lo = new URI(null, null, "10.36.10.77", 3868, null, null, null)
    val transports = {
      val l = new util.ArrayList[Transport]()
      l.add(Transport.tcp)
      l
    }

    val ifs = {
      val l = new util.ArrayList[NetworkInterfaceConfiguration]()
      l.add(new NetworkInterfaceConfiguration("default", lo, null, transports))
      l
    }
     */

    val config = new DiameterStackConfig()
    config.setNetworkInterfaces(conf.interfaces.asJava)

    val diameter = new io.snice.networking.diameter.DiameterConfig()
    diameter.setPeers(conf.peers.asJava)
    diameter.setInternalTransactionCacheConfig(conf.internalTransactionCache)
    config.setConfig(diameter)

    config
  }

}

/**
 * Glue configuration between Gatling's way of configuring and the configuration
 * needed by snice.io networking application.
 */
class DiameterStackConfig extends DiameterAppConfig {}
