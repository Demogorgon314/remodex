package com.emanueledipietro.remodex.model

enum class RemodexConnectionPhase {
    DISCONNECTED,
    CONNECTING,
    RETRYING,
    CONNECTED,
}

data class RemodexConnectionStatus(
    val phase: RemodexConnectionPhase = RemodexConnectionPhase.DISCONNECTED,
    val attempt: Int = 0,
)
