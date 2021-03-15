package io.snice.gatling.config

import com.fasterxml.jackson.annotation.JsonProperty
import io.netty.util.internal.ObjectUtil.checkNotNull
import io.snice.gatling.ss7.protocol.{M3UAConfig, MtpConfig, SccpConfig, Ss7EngineConfig, TcapConfig}

class Ss7Config(@JsonProperty("engine") _ss7EngineConfig: Ss7EngineConfig,
                @JsonProperty("simulation") _ss7SimulationConfig: Ss7SimulationConfig) {
  val ss7EngineConfig = checkNotNull(_ss7EngineConfig, "Ss7 engine configuration cannot be null")
  val ss7SimulationConfig = checkNotNull(_ss7SimulationConfig, "Ss7 simulation configuration cannot be null")
}

class Ss7SimulationConfig(@JsonProperty("atOnceUsers") _atOnceUsers: Int,
                          @JsonProperty("startConstantUsersPerSec") _startConstantUsersPerSec: Int,
                          @JsonProperty("rampRatePerSec") _rampRatePerSec: Int,
                          @JsonProperty("rampRateTarget") _rampRateTarget: Int,
                          @JsonProperty("endConstantUsersPerSec") _endConstantUsersPerSec: Int,
                          @JsonProperty("intervalInMinutes") _intervalInMinutes: Int) {
  val atOnceUsers = checkNotNull(_atOnceUsers, "atOnceUsers cannot be null")
  val startConstantUsersPerSec = checkNotNull(_startConstantUsersPerSec, "startConstantUsersPerSec cannot be null")
  val rampRatePerSec = checkNotNull(_rampRatePerSec, "rampRatePerSec cannot be null")
  val rampRateTarget = checkNotNull(_rampRateTarget, "rampRateTarget cannot be null")
  val endConstantUsersPerSec = checkNotNull(_endConstantUsersPerSec, "endConstantUsersPerSec cannot be null")
  val intervalInMinutes = checkNotNull(_intervalInMinutes, "intervalInMinutes cannot be null")
}
