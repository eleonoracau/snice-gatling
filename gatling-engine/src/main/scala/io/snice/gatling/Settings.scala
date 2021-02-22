package io.snice.gatling

import scala.concurrent.duration.{Duration, FiniteDuration}

trait Settings {

  private def required[T](name: String, value: Option[T]): T =
    value.getOrElse(throw new RuntimeException(s"$name must be provided"))

  private def getEnvProperty(name: String): Option[String] =
    sys.env.get(name).orElse(Option(System.getProperty(name))).orElse(Option(System.getenv(name)))

  private def toSnakeCase(name: String): String =
    name.toLowerCase.replace('.', '_').replace('-', '_')

  def getInt(name: String): Option[Int] =
    getEnvProperty(toSnakeCase(name)).map(_.toInt)

  def getString(name: String): Option[String] =
    getEnvProperty(toSnakeCase(name))

  def getDuration(name: String): Option[FiniteDuration] =
    getEnvProperty(toSnakeCase(name)).map(Duration.apply).collect { case fd: FiniteDuration => fd }

  final def getRampUpDuration: FiniteDuration =
    required("Ramp up duration", getDuration("simulation.ramp-up-duration"))

  final def getCoolDownDuration: FiniteDuration =
    required("Cool down duration", getDuration("simulation.cool-down-duration"))

  final def getConcurrentUsers: Int =
    required("Concurrent users number", getInt("simulation.concurrent-users"))

  final def getStablePhaseDuration: FiniteDuration =
    required("Stable phase duration", getDuration("simulation.stable-phase-duration"))

  final def getLocalAddress: String =
    required("Local Address", getString("simulation.local-address"))

  final def getRemotePgwAddress: String =
    required("Remote Pgw Address", getString("simulation.remote-pgw-address"))
}
