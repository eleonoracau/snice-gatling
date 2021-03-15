package io.snice.gatling.ss7.protocol

import com.fasterxml.jackson.annotation.JsonProperty
import io.netty.util.internal.ObjectUtil.checkNotNull
import org.mobicents.protocols.api.IpChannelType
import org.restcomm.protocols.ss7.indicator.{NatureOfAddress, RoutingIndicator}

class Ss7EngineConfig(@JsonProperty("M3UA") _m3uaConfig: M3UAConfig,
                      @JsonProperty("TCAP") _tcapConfig: TcapConfig,
                      @JsonProperty("MTP") _mtpConfig: MtpConfig,
                      @JsonProperty("SCCP") _sccpConfig: SccpConfig) {

  val m3uaConfig = checkNotNull(_m3uaConfig, "M3UA config cannot be null")
  val tcapConfig = checkNotNull(_tcapConfig, "TCAP config cannot be null")
  val mtpConfig = checkNotNull(_mtpConfig, "MTP config cannot be null")
  val sccpConfig = checkNotNull(_sccpConfig, "SCCP config cannot be null")

}

class M3UAConfig(@JsonProperty("routingContext") _routingContext: Long,
                 @JsonProperty("clientAssociationName") _clientAssociateName: String,
                 @JsonProperty("deliveryTransferMessageThreadCount") _deliveryTransferMessageThreadCount: Int,
                 @JsonProperty("networkAppearance") _networkAppearance: Int) {
  val ROUTING_CONTEXT = checkNotNull(_routingContext, "routingContext cannot be null")
  val CLIENT_ASSOCIATION_NAME = checkNotNull(_clientAssociateName, "clientAssociateName cannot be null")
  val DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT = checkNotNull(_deliveryTransferMessageThreadCount, "deliveryTransferMessageThreadCount cannot be null")
  val NETWORK_APPEARANCE = checkNotNull(_networkAppearance, "networkAppearance cannot be null")
}

class SccpConfig(@JsonProperty("ipChannelType") _ipChannelType: String) {
  val IP_CHANNEL_TYPE = checkNotNull(IpChannelType.getInstance(_ipChannelType), s"ipChannelType can only be one of ${IpChannelType.values()}")
}

class TcapConfig(@JsonProperty("maxDialogs") _maxDialogs: Int) {
  val MAX_DIALOGS = checkNotNull(_maxDialogs, "maxDialogs cannot be null")
}

class MtpConfig(@JsonProperty("localIP") _localIp: String,
                @JsonProperty("localPort") _localPort: Int,
                @JsonProperty("remoteIP") _remoteIp: String,
                @JsonProperty("remotePort") _remotePort: Int,
                @JsonProperty("localSPC") _localSpc: Int,
                @JsonProperty("remoteSPC") _remoteSpc: Int,
                @JsonProperty("networkIndicator") _networkIndicator: Int,
                @JsonProperty("serviceIndicator") _serviceIndicator: Int,
                @JsonProperty("routingIndicator") _routingIndicator: Int,
                @JsonProperty("localSgsnSSN") _localSgsnSsn: Int,
                @JsonProperty("localVlrSSN") _localVlrSsn: Int,
                @JsonProperty("remoteSSN") _remoteSsn: Int,
                @JsonProperty("localGT") _localGT: String,
                @JsonProperty("remoteGT") _remoteGt: String) {
  val LOCAL_IP = checkNotNull(_localIp, "localIP cannot be null")
  val LOCAL_PORT = checkNotNull(_localPort, "localPort cannot be null")
  val REMOTE_IP = checkNotNull(_remoteIp, "remoteIP cannot be null")
  val REMOTE_PORT = checkNotNull(_remotePort, "remotePort cannot be null")
  val LOCAL_SPC = checkNotNull(_localSpc, "localSPC cannot be null")
  val REMOTE_SPC = checkNotNull(_remoteSpc, "remoteSPC cannot be null")
  val NETWORK_INDICATOR = checkNotNull(_networkIndicator, "networkIndicator cannot be null")
  val SERVICE_INDICATOR = checkNotNull(_serviceIndicator, "serviceIndicator cannot be null")
  val ROUTING_INDICATOR = checkNotNull(RoutingIndicator.valueOf(_routingIndicator), "routingIndicator cannot be null")
  val LOCAL_SGSN_SSN = checkNotNull(_localSgsnSsn, "localSgsnSsn cannot be null")
  val LOCAL_VLR_SSN = checkNotNull(_localVlrSsn, "localVlrSsn cannot be null")
  val REMOTE_SSN = checkNotNull(_remoteSsn, "remoteSsn cannot be null")
  val DEFAULT_LOCAL_GT = checkNotNull(_localGT, "localGT1 cannot be null")
  val REMOTE_GT = checkNotNull(_remoteGt, "remoteGT cannot be null")
}