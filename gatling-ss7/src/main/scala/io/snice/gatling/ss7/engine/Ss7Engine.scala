package io.snice.gatling.ss7.engine

import com.typesafe.scalalogging.StrictLogging
import io.snice.gatling.ss7.protocol.Ss7Config
import org.mobicents.protocols.api.IpChannelType
import org.mobicents.protocols.sctp.netty.NettySctpManagementImpl
import org.restcomm.protocols.ss7.m3ua.impl.M3UAManagementImpl
import org.restcomm.protocols.ss7.m3ua.impl.parameter.ParameterFactoryImpl
import org.restcomm.protocols.ss7.m3ua.{ExchangeType, Functionality, IPSPType}
import org.restcomm.protocols.ss7.map.MAPStackImpl
import org.restcomm.protocols.ss7.sccp.impl.SccpStackImpl
import org.restcomm.protocols.ss7.sccp.impl.parameter.ProtocolClassImpl
import org.restcomm.protocols.ss7.tcap.TCAPStackImpl

class Ss7Engine (config: Ss7Config) extends StrictLogging {

  import scala.collection.JavaConverters._

  private val parameterFactory = new ParameterFactoryImpl
  private val sccpConfig = config.sccpConfig
  private val m3UAConfig = config.m3uaConfig
  private val mtpConfig = config.mtpConfig
  private val tcapConfig = config.tcapConfig

  private lazy val sccpStack = initializeSccpStack(sccpConfig.IP_CHANNEL_TYPE)

  def initializeSgsnStack() = initializeStack(mtpConfig.LOCAL_SGSN_SSN)

  def initializeVlrStack() = initializeStack(mtpConfig.LOCAL_VLR_SSN)

  /**
   * Initialize MAP Stack with shared SCTP, M3UA and SCCP layers
   */
  def initializeStack(ssn: Int): MAPStackImpl = {
    //    rateLimiterObj = RateLimiter.create(MAXCONCURRENTDIALOGS) // rate
    logger.info(s"starting Ss7 Engine with SSN $ssn")
    // Initialize TCAP
    val tcapStack = initTCAP(sccpStack, ssn)
    // Initialize MAP
    val mapStack = initMAP(tcapStack)

    Thread.sleep(5000)
    //    clientM3UAMgmt.getRoute.asScala.foreach { case(k, v) =>
    //      while (!v.getAsArray.headOption.get.isUp){
    //        Thread.sleep(5000)
    //        logger.info("ROUTE is DOWN")
    //      }
    //    }
    logger.info(s"OK Ss7 Engine with SSN $ssn")
    mapStack
  }

  def initializeSccpStack(ipChannelType: IpChannelType): SccpStackImpl = {
    val sctpMgmt = initSCTP(ipChannelType)
    val clientM3UAMgmt = initM3UA(sctpMgmt)
    val sccpStack = initSCCP(clientM3UAMgmt)
    clientM3UAMgmt.startAsp("RASP1")
    sccpStack
  }

  private def initSCTP(ipChannelType: IpChannelType): NettySctpManagementImpl = {
    logger.info("Init SCTP stack")
    val sctpManagement = new NettySctpManagementImpl("Client")
    // this.sctpManagement.setSingleThread(false);
    sctpManagement.start()
    sctpManagement.setConnectDelay(2000)
    sctpManagement.removeAllResourses()
    // 1. Create SCTP Association
    sctpManagement.addAssociation(mtpConfig.LOCAL_IP, mtpConfig.LOCAL_PORT, mtpConfig.REMOTE_IP, mtpConfig.REMOTE_PORT, m3UAConfig.CLIENT_ASSOCIATION_NAME, ipChannelType, null)
    sctpManagement
  }

  private def initM3UA(sctpManagement: NettySctpManagementImpl): M3UAManagementImpl = {
    val clientM3UAMgmt = new M3UAManagementImpl("Client", null, null)
    clientM3UAMgmt.setTransportManagement(sctpManagement)
    clientM3UAMgmt.setDeliveryMessageThreadCount(m3UAConfig.DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT)
    logger.info("Init M3UA stack")
    clientM3UAMgmt.start()
    clientM3UAMgmt.removeAllResourses()
    // m3ua as create rc <rc> <ras-name>
    val rc = parameterFactory.createRoutingContext(Array(m3UAConfig.ROUTING_CONTEXT))
//    val trafficModeType = parameterFactory.createTrafficModeType(config.TRAFFIC_MODE)
    val na = parameterFactory.createNetworkAppearance(m3UAConfig.NETWORK_APPEARANCE)
    // Step 1 : Create AS
    val as = clientM3UAMgmt.createAs("RAS1", Functionality.AS, ExchangeType.SE, IPSPType.CLIENT, rc, null, 1, na)
    // Step 2 : Create ASP
    clientM3UAMgmt.createAspFactory("RASP1", m3UAConfig.CLIENT_ASSOCIATION_NAME, 2, false)
    // Step3 : Assign ASP to AS
    clientM3UAMgmt.assignAspToAs("RAS1", "RASP1")
    // Step 4: Add Route. Remote point code is 2
    clientM3UAMgmt.addRoute(mtpConfig.REMOTE_SPC,  mtpConfig.LOCAL_SPC, mtpConfig.SERVICE_INDICATOR, "RAS1")
    clientM3UAMgmt.getRoute.asScala.foreach { case(k, v) =>
      println(s"key $k value ${v.getAsArray.headOption.get.isUp} state ${v.getAsArray.headOption.get.getState.getName}")
    }
    clientM3UAMgmt
  }

  private def initSCCP(clientM3UAMgmt: M3UAManagementImpl): SccpStackImpl = {
    logger.info("Init SCCP stack")
    val sccpStack = new SccpStackImpl("MapLoadClientSccpStack", null)
    sccpStack.setMtp3UserPart(1, clientM3UAMgmt)
    // sccpStack.setCongControl_Algo(SccpCongestionControlAlgo.levelDepended);
    sccpStack.start()
    sccpStack.removeAllResourses()
    sccpStack.getSccpResource.addRemoteSpc(1, mtpConfig.REMOTE_SPC, 0, 0)
    sccpStack.getSccpResource.addRemoteSsn(1, mtpConfig.REMOTE_SPC, mtpConfig.REMOTE_SSN, 0, false)
    sccpStack.getRouter.addMtp3ServiceAccessPoint(1, 1, mtpConfig.LOCAL_SPC, mtpConfig.NETWORK_INDICATOR, 0, null)
    sccpStack.getRouter.addMtp3Destination(1, 1, mtpConfig.REMOTE_SPC, mtpConfig.REMOTE_SPC, 0, 255, 255)
    sccpStack.newConnection(mtpConfig.LOCAL_SGSN_SSN, new ProtocolClassImpl(3))
    sccpStack.newConnection(mtpConfig.LOCAL_VLR_SSN, new ProtocolClassImpl(3))
    sccpStack
  }

  private def initTCAP(sccpStack: SccpStackImpl, ssn: Int): TCAPStackImpl = {
    logger.info("Init TCAP stack")
    val tcapStack = new TCAPStackImpl(s"Test-$ssn", sccpStack.getSccpProvider, ssn)
    tcapStack.start()
    tcapStack.setDialogIdleTimeout(60000)
    tcapStack.setInvokeTimeout(30000)
    tcapStack.setMaxDialogs(tcapConfig.MAX_DIALOGS)
    tcapStack
  }

  private def initMAP(tcapStack: TCAPStackImpl): MAPStackImpl = {
    logger.info("Init MAP stack")
    val mapStack = new MAPStackImpl("Test", tcapStack.getProvider)
    mapStack.start()
    mapStack
  }
}
