package com.emanueledipietro.remodex.model

import kotlinx.serialization.Serializable

@Serializable
enum class RemodexAppearanceMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    val label: String
        get() = when (this) {
            SYSTEM -> "System"
            LIGHT -> "Light"
            DARK -> "Dark"
        }
}
