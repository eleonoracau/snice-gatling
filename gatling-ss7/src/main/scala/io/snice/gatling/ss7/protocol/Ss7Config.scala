package io.snice.gatling.ss7.protocol

import org.mobicents.protocols.api.IpChannelType
import org.restcomm.protocols.ss7.indicator.RoutingIndicator
import org.restcomm.protocols.ss7.map.api.service.mobility.locationManagement.CancellationType
import org.restcomm.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl
import org.restcomm.protocols.ss7.sccp.parameter.SccpAddress

//todo - convert this to a proper config
class Ss7Config {

  val ORIGIN = "31628968300"
  val DEST = "204208300008002"
  val MSISDN_STR = "31628838002"
  val IMSI_STR = "1112345678990"
  val LMSI_STR: Array[Byte] = Array[Byte](0, 3, 98, 39)
  val CANCELLATION_TYPE = CancellationType.initialAttachProcedure
  val USSD_STR = "*133#"

  val IP_CHANNEL_TYPE = IpChannelType.SCTP

  // MTP Details
  val CLIENT_SPC = 1
  val SERVET_SPC = 2
  val NETWORK_INDICATOR = 2
  val SERVICE_INIDCATOR = 3 // SCCP
  val ROUTING_INDICATOR = RoutingIndicator.ROUTING_BASED_ON_DPC_AND_SSN

  val SSN = 8

  // M3UA details
  val CLIENT_IP = "127.0.0.1"
  val CLIENT_PORT = 2345

  val SERVER_IP = "127.0.0.1"
  val SERVER_PORT = 3434

  val CLIENT_ASSOCIATION_NAME = "clientAsscoiation"

  val DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT: Int = Runtime.getRuntime.availableProcessors * 2

  // TCAP Details
  val MAX_DIALOGS = 500000

  val LOCAL_ADDRESS: SccpAddress = createSccpAddress(ROUTING_INDICATOR, CLIENT_SPC, null)
  val REMOTE_ADDRESS: SccpAddress = createSccpAddress(ROUTING_INDICATOR, SERVET_SPC, null)

  private def createSccpAddress(ri: RoutingIndicator, dpc: Int, address: String) = {
    val fact = new ParameterFactoryImpl
    fact.createSccpAddress(ri, null, dpc, SSN)
  }
}
