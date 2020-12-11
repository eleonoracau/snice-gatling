package io.snice.gatling.ss7.protocol

import org.mobicents.protocols.api.IpChannelType
import org.restcomm.protocols.ss7.indicator.RoutingIndicator
import org.restcomm.protocols.ss7.m3ua.parameter.TrafficModeType
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

  val TRAFFIC_MODE = TrafficModeType.Loadshare
  val RC: Long = 101L
  val NETWORK_APPEARANCE: Long = 102L
  val SI = 3

  // MTP Details
  val LOCAL_SPC = 60
  val REMOTE_SPC = 50
  val NETWORK_INDICATOR = 3
  val SERVICE_INDICATOR = 3 // SCCP
  val ROUTING_INDICATOR = RoutingIndicator.ROUTING_BASED_ON_DPC_AND_SSN

  val SSN = 8

  // M3UA details
  val LOCAL_IP = "172.22.182.207"
  val LOCAL_PORT = 2905

  val REMOTE_IP = "172.22.157.172"
  val REMOTE_PORT = 2905

  val CLIENT_ASSOCIATION_NAME = "clientAssociation"

  val DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT: Int = Runtime.getRuntime.availableProcessors * 2

  // TCAP Details
  val MAX_DIALOGS = 500000

  val LOCAL_ADDRESS: SccpAddress = createSccpAddress(ROUTING_INDICATOR, LOCAL_SPC, "172.22.182.207")
  val REMOTE_ADDRESS: SccpAddress = createSccpAddress(ROUTING_INDICATOR, REMOTE_SPC, "172.22.157.172")

  private def createSccpAddress(ri: RoutingIndicator, dpc: Int, address: String) = {
    val factory = new ParameterFactoryImpl
    factory.createSccpAddress(ri, null, dpc, SSN)
  }
}
