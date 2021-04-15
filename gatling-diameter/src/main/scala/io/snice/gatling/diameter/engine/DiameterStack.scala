package io.snice.gatling.diameter.engine

import java.util
import io.snice.codecs.codec.diameter.avp.{Avp}
import io.snice.codecs.codec.diameter.avp.`type`.{DiameterType, OctetString}
import io.snice.codecs.codec.diameter.avp.api._
import io.snice.codecs.codec.diameter.{DiameterAnswer, DiameterMessage, DiameterRequest}
import io.snice.gatling.diameter.engine.avp.{ConfidentialityKey, IntegrityKey}
import io.snice.networking.diameter._
import io.snice.networking.diameter.event.DiameterEvent
import io.snice.networking.diameter.peer.{Peer, PeerConfiguration}

import scala.collection.JavaConverters._

object DiameterStack {

  def apply(config: DiameterStackConfig): DiameterStack = {
    val bundle: DiameterBundle[DiameterStackConfig] = new DiameterBundle()
    new DiameterStack(config, bundle)
  }

}

/**
 * The stack is implementing the snice.io Network
 */
class DiameterStack(config: DiameterStackConfig, bundle: DiameterBundle[DiameterStackConfig]) extends DiameterApplication[DiameterStackConfig] {

  private var environment: DiameterEnvironment[DiameterStackConfig] = _

  override def initialize(bootstrap: DiameterBootstrap[DiameterStackConfig]): Unit = {
    println("initializing the stack!")
    bootstrap.onConnection(id => true).accept(b => {
      b.`match`(msg => msg.isAIR).consume((con, msg) => processAIR(con, msg))
      b.`match`(msg => msg.isULR).consume((con, msg) => processULR(con, msg))
      b.`match`(msg => msg.isULA).consume((con, msg) => processULA(con, msg))
    })
  }

  def addPeer(config: PeerConfiguration): Peer = environment.addPeer(config)

  override def run(config: DiameterStackConfig, environment: DiameterEnvironment[DiameterStackConfig]): Unit = {
    this.environment = environment
  }

  private def processULR(con: PeerConnection, evt: DiameterEvent): Unit = {
    val ulr = evt.getMessage.toRequest
    val subscriptionData = mockSubscriptionData(ulr)
    val ula: DiameterAnswer = ulr.createAnswer(ResultCode.DiameterSuccess2001)
      .withOriginHost("snice.node.epc.mnc001.mcc001.3gppnetwork.org")
      .withOriginRealm("epc.mnc001.mcc001.3gppnetwork.org")
      .withAvp(AuthSessionState.NoStateMaintained)
      .withAvp(subscriptionData)
      .build
    con.send(ula)
  }

  private def mockSubscriptionData(ulr: DiameterRequest): SubscriptionData = {
    val msisdn = Msisdn.of("123456789")
    val userName = ulr.getAvp(UserName.CODE).get().ensure()

    SubscriptionData.of(msisdn, NetworkAccessMode.OnlyPacket,  IcsIndicator.True, SubscriberStatus.ServiceGranted, userName)
  }

  private def processAIR(con: PeerConnection, evt: DiameterEvent): Unit = {
    val air = evt.getMessage.toRequest

    val numberOfRequestedVectors = getNumberOfRequestedVectors(air)
    val utranGeranVectors = buildMockedGeranVectorList(numberOfRequestedVectors)
    val authenticationInfo = AuthenticationInfo.of(utranGeranVectors)

    val aia: DiameterAnswer = air.createAnswer(ResultCode.DiameterSuccess2001)
      .withOriginHost("snice.node.epc.mnc001.mcc001.3gppnetwork.org")
      .withOriginRealm("epc.mnc001.mcc001.3gppnetwork.org")
      .withAvp(AuthSessionState.NoStateMaintained)
      .withAvp(authenticationInfo)
      .build
    con.send(aia)
  }

  def getNumberOfRequestedVectors(air: DiameterRequest): Int = {
    // read number of requested vectors from RequestedUtranGeranAuthenticationInfo buffer
    val buffer = air.getAvps(RequestedUtranGeranAuthenticationInfo.CODE).get(0).ensure().getData.toReadableBuffer
    buffer.getInt(buffer.capacity() - 4)
  }

  def buildMockedGeranVectorList(count : Int): util.List[Avp[_ <: DiameterType]] = {
    val utranGeranVectors = new util.ArrayList[Avp[_ <: DiameterType]]()
    Stream.range(0, count).foreach(i => {
      utranGeranVectors.add(buildGeranVector(i))
      utranGeranVectors.add(buildUtranVector(i))
    })
    utranGeranVectors
  }

  def buildGeranVector(num: Int): GeranVector = {
    val octetString = OctetString.parse("1" * 16)
    GeranVector.of(ItemNumber.of(num), Rand.of(octetString), Sres.of(octetString), Kc.of(octetString), Xres.of(octetString))
  }

  def buildUtranVector(num: Int): UtranVector = {
    val octetString = OctetString.parse("1" * 16)
    UtranVector.of(ItemNumber.of(num), Rand.of(octetString), Sres.of(octetString), Xres.of(octetString), Autn.of(octetString),
      ConfidentialityKey.of(octetString), IntegrityKey.of(octetString))
  }

  private def processULA(con: PeerConnection, evt: DiameterEvent): Unit = {
    println("Got a ULA! " + evt.getAnswer)
  }

  /**
   * Send the given msg and have the stack automatically figure out which [[io.snice.networking.diameter.Peer]] to
   * use.
   *
   * @param msg the message to send.
   */
  def send(msg: DiameterMessage): Unit = {
    // connection.send(msg)
    environment.send(msg)
  }

  def start(): Unit = {
    run(config)
  }

  def peers(): List[Peer] = {
    environment.getPeers.asScala.toList
  }
}
