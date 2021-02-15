package io.snice.gatling.ss7.protocol

import org.mobicents.protocols.api.IpChannelType
import org.restcomm.protocols.ss7.indicator.{NatureOfAddress, RoutingIndicator}
import org.restcomm.protocols.ss7.m3ua.parameter.TrafficModeType
import org.restcomm.protocols.ss7.indicator.NumberingPlan
import org.restcomm.protocols.ss7.map.api.service.mobility.locationManagement.CancellationType
import org.restcomm.protocols.ss7.sccp.impl.parameter.{BCDOddEncodingScheme, ParameterFactoryImpl}
import org.restcomm.protocols.ss7.sccp.parameter.{EncodingSchemeType, SccpAddress}

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
  val ROUTING_CONTEXT: Long = 50
  val NETWORK_APPEARANCE: Long = 8
  val SI = 3

  // MTP Details
  val LOCAL_SPC = 60
  val REMOTE_SPC = 50
  val NETWORK_INDICATOR = 3
  val SERVICE_INDICATOR = 3 // SCCP
  val ROUTING_INDICATOR = RoutingIndicator.ROUTING_BASED_ON_DPC_AND_SSN

  val LOCAL_SSN = 149 //sgsn
  val REMOTE_SSN = 6
  val LOCAL_GT = "491720123095"
  val REMOTE_GT = "883260000000990"

  // M3UA details
  val LOCAL_IP = "172.22.185.161"
  val LOCAL_PORT = 2905

  val REMOTE_IP = "172.22.196.64"
  val REMOTE_PORT = 2905

  val CLIENT_ASSOCIATION_NAME = "clientAssociation"

  val DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT: Int = 10

  // TCAP Details
  val MAX_DIALOGS = 500000

  val LOCAL_ADDRESS: SccpAddress = createSccpAddress(ROUTING_INDICATOR, LOCAL_SPC, LOCAL_GT, LOCAL_SSN, null)
  val REMOTE_ADDRESS: SccpAddress = createSccpAddress(ROUTING_INDICATOR, REMOTE_SPC, REMOTE_GT, REMOTE_SSN, null)

  private def createSccpAddress(ri: RoutingIndicator, dpc: Int, gt: String, subSystemNumber: Int, address: String) = {
    val factory = new ParameterFactoryImpl
    val newGt = factory.createGlobalTitle(gt, 0, NumberingPlan.ISDN_TELEPHONY, BCDOddEncodingScheme.INSTANCE, NatureOfAddress.INTERNATIONAL)
    factory.createSccpAddress(ri, newGt, dpc, subSystemNumber)
  }
}
