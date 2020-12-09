package io.snice.gatling.ss7.engine

import io.snice.gatling.ss7.protocol.Ss7Config
import org.mobicents.protocols.api.IpChannelType
import org.mobicents.protocols.sctp.netty.NettySctpManagementImpl
import org.restcomm.protocols.ss7.m3ua.impl.M3UAManagementImpl
import org.restcomm.protocols.ss7.m3ua.impl.parameter.ParameterFactoryImpl
import org.restcomm.protocols.ss7.m3ua.parameter.TrafficModeType
import org.restcomm.protocols.ss7.m3ua.{ExchangeType, Functionality, IPSPType}
import org.restcomm.protocols.ss7.map.MAPStackImpl
import org.restcomm.protocols.ss7.sccp.impl.SccpStackImpl
import org.restcomm.protocols.ss7.tcap.TCAPStackImpl

class Ss7Engine (config: Ss7Config) {

  def initializeStack(ipChannelType: IpChannelType): MAPStackImpl = {
//    rateLimiterObj = RateLimiter.create(MAXCONCURRENTDIALOGS) // rate

    val sctpMgmt = initSCTP(ipChannelType)
    // Initialize M3UA first
    val clientM3UAMgmt = initM3UA(sctpMgmt)
    // Initialize SCCP
    val sccpStack = initSCCP(clientM3UAMgmt)
    // Initialize TCAP
    val tcapStack = initTCAP(sccpStack)
    // Initialize MAP
    val mapStack = initMAP(tcapStack)
    // FInally start ASP
    // Set 5: Finally start ASP
    clientM3UAMgmt.startAsp("ASP1")
    mapStack
  }

  private def initSCTP(ipChannelType: IpChannelType): NettySctpManagementImpl = {
    val sctpManagement = new NettySctpManagementImpl("Client")
    // this.sctpManagement.setSingleThread(false);
    sctpManagement.start()
    sctpManagement.setConnectDelay(10000)
    sctpManagement.removeAllResourses()
    // 1. Create SCTP Association
    sctpManagement.addAssociation(config.CLIENT_IP, config.CLIENT_PORT, config.SERVER_IP, config.SERVER_PORT, config.CLIENT_ASSOCIATION_NAME, ipChannelType, null)
    sctpManagement
  }

  private def initM3UA(sctpManagement: NettySctpManagementImpl): M3UAManagementImpl = {
    val clientM3UAMgmt = new M3UAManagementImpl("Client", null, null)
    clientM3UAMgmt.setTransportManagement(sctpManagement)
    clientM3UAMgmt.setDeliveryMessageThreadCount(config.DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT)
    clientM3UAMgmt.start()
    clientM3UAMgmt.removeAllResourses()
    // m3ua as create rc <rc> <ras-name>
    val factory = new ParameterFactoryImpl
    val rc = factory.createRoutingContext(Array[Long](101L))
    val trafficModeType = factory.createTrafficModeType(TrafficModeType.Loadshare)
    val na = factory.createNetworkAppearance(102L)
    clientM3UAMgmt.createAs("AS1", Functionality.IPSP, ExchangeType.SE, IPSPType.CLIENT, rc, trafficModeType, 1, na)
    // Step 2 : Create ASP
    clientM3UAMgmt.createAspFactory("ASP1", config.CLIENT_ASSOCIATION_NAME)
    // Step3 : Assign ASP to AS
    val asp = clientM3UAMgmt.assignAspToAs("AS1", "ASP1")
    // Step 4: Add Route. Remote point code is 2
    clientM3UAMgmt.addRoute(config.SERVET_SPC, -1, -1, "AS1")
    clientM3UAMgmt
  }

  private def initSCCP(clientM3UAMgmt: M3UAManagementImpl): SccpStackImpl = {
    val sccpStack = new SccpStackImpl("MapLoadClientSccpStack", null)
    sccpStack.setMtp3UserPart(1, clientM3UAMgmt)
    // sccpStack.setCongControl_Algo(SccpCongestionControlAlgo.levelDepended);
    sccpStack.start()
    sccpStack.removeAllResourses()
    sccpStack.getSccpResource.addRemoteSpc(0, config.SERVET_SPC, 0, 0)
    sccpStack.getSccpResource.addRemoteSsn(0, config.SERVET_SPC, config.SSN, 0, false)
    sccpStack.getRouter.addMtp3ServiceAccessPoint(1, 1, config.CLIENT_SPC, config.NETWORK_INDICATOR, 0, null)
    sccpStack.getRouter.addMtp3Destination(1, 1, config.SERVET_SPC, config.SERVET_SPC, 0, 255, 255)
    sccpStack
  }

  private def initTCAP(sccpStack: SccpStackImpl): TCAPStackImpl = {
    val tcapStack = new TCAPStackImpl("Test", sccpStack.getSccpProvider, config.SSN)
    tcapStack.start()
    tcapStack.setDialogIdleTimeout(60000)
    tcapStack.setInvokeTimeout(30000)
    tcapStack.setMaxDialogs(config.MAX_DIALOGS)
    tcapStack
  }

  private def initMAP(tcapStack: TCAPStackImpl): MAPStackImpl = {
    val mapStack = new MAPStackImpl("GatlingClient", tcapStack.getProvider)
    mapStack.start()
    mapStack
  }

}
