package io.snice.gatling

import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder
import io.snice.gatling.simulations.{Ss7AttachmentSimulation, StartDiameterStack}

import scala.sys.exit

object Engine extends App {

  override def main(args: Array[String]): Unit = {
    val usage = """
      Usage: [--operation operation_name]
    """

    def getOptions(map : Map[String, String], list: List[String]) : Map[String, String] = {
      list match {
        case Nil => map
        case "--operation" :: value :: tail =>
          getOptions(map ++ Map("operation" -> value.trim), tail)
        case option :: tail => {
          println("Unknown option "+option)
          println(usage)
        }
          exit(1)
      }
    }

    val props = new GatlingPropertiesBuilder()

    val simulationMap = collection.immutable.HashMap(
      "ss7AttachmentSimulation" -> classOf[Ss7AttachmentSimulation].getName,
      "startDiameterStack" -> classOf[StartDiameterStack].getName
    )

    if (args.length == 0) {
      println(usage)
      exit(1)
    }
    val arglist = args.toList
    val options = getOptions(Map(),arglist)
    val simulationName = options.get("operation")
      .flatMap(inputName => {
        println(inputName.toLowerCase())
        simulationMap.get(inputName)
      })

    buildSimulation(props, simulationName)
    Gatling.fromMap(props.build)

    def buildSimulation(props: GatlingPropertiesBuilder, simulationName: Option[String]): Unit = {
      if (simulationName.isEmpty) {
        println(s"Operation not found. Available operations: ${simulationMap.keys.toList.toString()}\n")
        exit(1)
      } else {
        props.simulationClass(simulationName.get)
      }
    }
  }
}
