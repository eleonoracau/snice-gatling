package io.snice.gatling.requests

import io.gatling.core.Predef._
import io.snice.codecs.codec.MccMnc
import io.snice.codecs.codec.diameter.avp.api._
import io.snice.gatling.diameter.Predef._

object AuthenticationInfoRequest {

  val vendorId = VendorId.of(10415L)
  val authAppId = AuthApplicationId.of(16777251L)
  val appId = VendorSpecificApplicationId.of(vendorId, authAppId)

  val eUtran = RequestedEutranAuthenticationInfo.of(NumberOfRequestedVectors.of(1), ImmediateResponsePreferred.of(1))
  val uTranGeran = RequestedUtranGeranAuthenticationInfo.of(NumberOfRequestedVectors.of(3), ImmediateResponsePreferred.of(1))

  // val vplmnId = WritableBuffer.of(3).fastForwardWriterIndex
  val mccMnc = MccMnc.parse("130/110");
  val vplmn = VisitedPlmnId.of(mccMnc.toBuffer);

  val expectedOiReplacement: ApnOiReplacement = ApnOiReplacement.of("hello.apn.mcc123.mcc123.gprs")

  val airBase = diameter("Authenticate")
    .air("${imsi}")
    .originHost("${originHost}")
    .originRealm("${originRealm}")
    .destinationRealm("${destinationRealm}")
    .sessionId("${sessionId}")
    .avp(appId)
    .avp(AuthSessionState.NoStateMaintained)
    .avp(vplmn)
  // .check(avp(classOf[ApnOiReplacement]).is(expectedOiReplacement).saveAs("oi-replacement"))
  // .check(status.is(2001).saveAs("fup"))
  // .check(status.is(ExperimentalResultCode.DiameterErrorUserUnknown5001.getAsEnum.get.getCode).saveAs("fup"))
  // .check(avp(classOf[DsaFlags]).exists.saveAs("dsa"))
  // .check(avp(classOf[ApnOiReplacement]).is(ApnOiReplacement.of("hello")).saveAs("oi-replacement"))
  // .check(originHost.exists.saveAs("apa2"))
  // .check(status.is(2001))
  // .check(avp(AuthenticationInfo.CODE).notExists) // ensure an AVP exists/does not exist.
  // .check(avp2(AuthenticationInfo.CODE))

  /**
   * A basic AIR that asks for 1 eutran vector and checks that the
   * [[AuthenticationInfo]] AVP actually exists on the AIA
   */
  val air = airBase
    .avp(eUtran)
    .check(status.is(2001))
    .check(avp(classOf[AuthenticationInfo]).exists)

  val airUserUnknown = airBase
    .check(status.is(ExperimentalResultCode.DiameterErrorUserUnknown5001.getAsEnum.get.getCode))

}
