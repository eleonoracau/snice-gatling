package io.snice.gatling.diameter.protocol

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}
import com.typesafe.config.{ConfigValue, ConfigValueType}
import io.gatling.core.config.GatlingConfiguration
import io.snice.functional.Maps
import io.snice.networking.config.{NetworkInterfaceConfiguration, NetworkInterfaceDeserializer}
import io.snice.networking.diameter.peer.PeerConfiguration
import io.snice.networking.diameter.peer.fsm.InternalTransactionCacheConfig

import scala.collection.JavaConverters._

object DiameterConfig {

  /**
   * Extract out the diameter configuration out of the Gatling config object.
   * If there is no diameter section defined, then create a default configuration
   * object.
   */
  def apply(gatlingConfiguration: GatlingConfiguration): DiameterConfig = {
    val gatling = gatlingConfiguration.config.getObject("gatling")
    val conf  = Option(gatling.get("diameter")) match {
      case Some(v) => flattenGatlingConf(v)
      case None => defaultConfiguration()
    }

    DiameterConfig(conf)
  }

  /**
   * This is quite annoying. The Lightbend Config stuff is quite nice but this conversion between
   * scala/java, special flat mapping and what not is really annoying.
   *
   * And looking at what Gatling themselves are doing doing, they manually will extract out all keys
   * and build up the objects, which is what I'm trying to avoid here. I just want to be able
   * to map to case classes and be done with it.
   */
  def flattenGatlingConf(value: ConfigValue): Map[String, Any] = {
    if (value.valueType() != ConfigValueType.OBJECT) {
      throw new IllegalArgumentException(s"Unexpected type ${value.valueType()}. " +
        s"It is expected that the diameter section of type ${ConfigValueType.OBJECT}")
    }

    // Note: not correct. Could be a list as well.
    val raw = value.unwrapped().asInstanceOf[java.util.Map[String, Object]]
    val interfaces: List[Map[String, Any]] = Option(raw.get("interfaces")) match {
      case Some(ifs) => Maps.flatten("name", ifs).asScala.toStream.map(_.asScala.toMap).toList
      case None => List(defaultInterfaceConfig())
    }

    val peers: List[Map[String, Any]] = Option(raw.get("peers")) match {
      case Some(ifs) => Maps.flatten("name", ifs).asScala.toStream.map(_.asScala.toMap).toList
      case None => List(defaultPeerConfig())
    }

    val internalTransactionCache: Map[String, Any] = Option(raw.get("internalTransactionCache")) match {
      case Some(up) => up.asInstanceOf[java.util.Map[String, Object]].asScala.toMap
      case None => defaultInternalTransactionCache()
    }

    Map("interfaces" -> interfaces, "peers" -> peers, "internalTransactionCache" -> internalTransactionCache)
  }

  def apply(conf: Map[String, Any]): DiameterConfig = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(new Jdk8Module)
    mapper.registerModule(DefaultScalaModule)

    val module = new SimpleModule
    module.addDeserializer(classOf[NetworkInterfaceConfiguration], new NetworkInterfaceDeserializer)
    mapper.registerModule(module)

    val json = mapper.writeValueAsString(conf)
    mapper.readValue[DiameterConfig](json)
  }

  /**
   * The default configuration if the user did not specify one.
   */
  private def defaultConfiguration() : Map[String, Any] = {
    val interfaces = List(defaultInterfaceConfig())
    val peers = List(defaultPeerConfig())
    Map("interfaces" -> interfaces, "peers" -> peers, "internalTransactionCache" -> defaultInternalTransactionCache)
  }

  /**
   * Default interface configuration if the user did not specify one.
   *
   * Note that these default values are part of the public contract and as such,
   * if you do change them you will also have to update the public documentation.
   */
  private def defaultInterfaceConfig() : Map[String, Any] =
    Map("name" -> "default", "address" -> "aaa://*:3868", "isDefault" -> true)

  private def defaultInternalTransactionCache(): Map[String, Any] = Map("expiryIntervalInSeconds" -> 10, "initialSize" -> 100, "maxEntries" -> 1024)

  /**
   * Default peer configuration if the user did not specify one.
   *
   * Note that these default values are part of the public contract and as such,
   * if you do change them you will also have to update the public documentation.
   */
  private def defaultPeerConfig() : Map[String, Any] = Map("name" -> "default")
}

/**
 * Class containing the configuration of the underlying diameter stack.
 */
final case class DiameterConfig(interfaces: List[NetworkInterfaceConfiguration],
                                peers: List[PeerConfiguration],
                                internalTransactionCache: InternalTransactionCacheConfig)
/**
 *
 * @param name
 * @param address
 * @param isDefault
 */
final case class InterfaceConfig(name: String, address: String, isDefault: Boolean = false)

/**
 * Case class containing the configuration of an individual peer.
 */
final case class PeerConfig(name: String)
