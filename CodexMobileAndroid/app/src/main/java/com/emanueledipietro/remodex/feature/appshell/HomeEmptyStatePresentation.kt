package com.emanueledipietro.remodex.feature.appshell

import com.emanueledipietro.remodex.data.connection.SecureConnectionState
import com.emanueledipietro.remodex.model.RemodexConnectionPhase
import com.emanueledipietro.remodex.model.RemodexTrustedMacPresentation

internal enum class HomeEmptyStateStatus {
    OFFLINE,
    CONNECTING,
    CONNECTED,
}

internal data class HomeEmptyStatePresentation(
    val status: HomeEmptyStateStatus,
    val primaryTitle: String,
    val isPrimaryBusy: Boolean,
    val primaryEnabled: Boolean,
    val trustedMac: RemodexTrustedMacPresentation?,
    val bodyMessage: String?,
    val showsScanNewQrAction: Boolean,
    val showsForgetPairAction: Boolean,
)

internal fun AppUiState.toHomeEmptyStatePresentation(): HomeEmptyStatePresentation {
    val busySecureStates = setOf(
        SecureConnectionState.HANDSHAKING,
        SecureConnectionState.RECONNECTING,
    )
    val isBusy = recoveryState.secureState in busySecureStates ||
        connectionStatus.phase == RemodexConnectionPhase.CONNECTING
    val status = when {
        isConnected -> HomeEmptyStateStatus.CONNECTED
        isBusy -> HomeEmptyStateStatus.CONNECTING
        else -> HomeEmptyStateStatus.OFFLINE
    }
    val primaryTitle = when (status) {
        HomeEmptyStateStatus.CONNECTING -> "Reconnecting..."
        HomeEmptyStateStatus.CONNECTED -> "Disconnect"
        HomeEmptyStateStatus.OFFLINE -> if (trustedMac != null) "Reconnect" else "Scan QR Code"
    }
    val showSavedPairActions = trustedMac != null && !isConnected

    return HomeEmptyStatePresentation(
        status = status,
        primaryTitle = primaryTitle,
        isPrimaryBusy = isBusy,
        primaryEnabled = !isBusy,
        trustedMac = trustedMac,
        bodyMessage = connectionMessage.trim().ifEmpty { null },
        showsScanNewQrAction = isBusy || showSavedPairActions,
        showsForgetPairAction = showSavedPairActions,
    )
}
