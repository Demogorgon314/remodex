package com.emanueledipietro.remodex.model

data class RemodexTrustedMacPresentation(
    val deviceId: String? = null,
    val title: String = "Trusted Pair",
    val name: String,
    val systemName: String? = null,
    val detail: String? = null,
)
