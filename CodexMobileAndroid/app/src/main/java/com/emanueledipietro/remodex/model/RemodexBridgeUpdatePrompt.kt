package com.emanueledipietro.remodex.model

const val remodexBridgeUpdateCommand = "npm install -g @deepdream314/remodex@latest"

data class RemodexBridgeUpdatePrompt(
    val title: String,
    val message: String,
    val command: String = remodexBridgeUpdateCommand,
)
