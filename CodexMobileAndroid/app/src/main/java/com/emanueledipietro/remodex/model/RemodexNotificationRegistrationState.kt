package com.emanueledipietro.remodex.model

enum class RemodexNotificationAuthorizationStatus {
    UNKNOWN,
    NOT_DETERMINED,
    DENIED,
    AUTHORIZED,
}

enum class RemodexManagedPushPlatform(
    val wireValue: String,
) {
    IOS("ios"),
    ANDROID("android"),
}

enum class RemodexManagedPushProvider(
    val wireValue: String,
) {
    APNS("apns"),
    FCM("fcm"),
}

data class RemodexNotificationRegistrationState(
    val authorizationStatus: RemodexNotificationAuthorizationStatus = RemodexNotificationAuthorizationStatus.UNKNOWN,
    val managedPushSupported: Boolean = false,
    val platform: RemodexManagedPushPlatform? = null,
    val pushProvider: RemodexManagedPushProvider? = null,
    val deviceTokenPreview: String? = null,
    val lastErrorMessage: String? = null,
)
