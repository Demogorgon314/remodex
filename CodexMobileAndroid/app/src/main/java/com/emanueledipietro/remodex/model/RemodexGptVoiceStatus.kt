package com.emanueledipietro.remodex.model

enum class RemodexGptVoiceStatus {
    READY,
    DISCONNECTED,
    LOGIN_REQUIRED,
    LOGIN_PENDING,
    REAUTH_REQUIRED,
    VOICE_SYNC_IN_PROGRESS,
}

fun remodexGptVoiceStatus(
    snapshot: RemodexGptAccountSnapshot,
    isConnected: Boolean,
): RemodexGptVoiceStatus {
    return when {
        !isConnected -> RemodexGptVoiceStatus.DISCONNECTED
        snapshot.needsReauth || snapshot.status == RemodexGptAccountStatus.EXPIRED ->
            RemodexGptVoiceStatus.REAUTH_REQUIRED
        snapshot.isAuthenticated && snapshot.isVoiceTokenReady ->
            RemodexGptVoiceStatus.READY
        snapshot.isAuthenticated ->
            RemodexGptVoiceStatus.VOICE_SYNC_IN_PROGRESS
        snapshot.hasActiveLogin ->
            RemodexGptVoiceStatus.LOGIN_PENDING
        else ->
            RemodexGptVoiceStatus.LOGIN_REQUIRED
    }
}

fun remodexGptHintText(
    snapshot: RemodexGptAccountSnapshot,
    isConnected: Boolean,
): String? {
    return when (remodexGptVoiceStatus(snapshot = snapshot, isConnected = isConnected)) {
        RemodexGptVoiceStatus.READY -> null
        RemodexGptVoiceStatus.REAUTH_REQUIRED ->
            remodexLocalizedText(
                "这个 bridge 的语音功能需要在电脑上重新登录 ChatGPT.",
                "Voice on this bridge needs a fresh ChatGPT sign-in on your computer.",
            )
        RemodexGptVoiceStatus.VOICE_SYNC_IN_PROGRESS ->
            remodexLocalizedText("正在等待语音同步...", "Waiting for voice sync...")
        RemodexGptVoiceStatus.LOGIN_PENDING ->
            remodexLocalizedText(
                "请在电脑浏览器中完成 ChatGPT 登录.",
                "Finish the ChatGPT sign-in flow in the browser on your computer.",
            )
        RemodexGptVoiceStatus.DISCONNECTED ->
            remodexLocalizedText("请先连接 bridge.", "Connect to your bridge first.")
        RemodexGptVoiceStatus.LOGIN_REQUIRED ->
            remodexLocalizedText(
                "ChatGPT 语音会使用已在电脑上登录的账号.",
                "ChatGPT voice uses the account already signed in on your computer.",
            )
    }
}

fun remodexGptSummaryText(
    snapshot: RemodexGptAccountSnapshot,
    isConnected: Boolean,
): String {
    return when (remodexGptVoiceStatus(snapshot = snapshot, isConnected = isConnected)) {
        RemodexGptVoiceStatus.READY ->
            remodexLocalizedText(
                "正在使用配对电脑 bridge 上的 ChatGPT 会话.",
                "Using the ChatGPT session from your paired computer bridge.",
            )
        RemodexGptVoiceStatus.REAUTH_REQUIRED ->
            remodexLocalizedText(
                "请在配对电脑上刷新 ChatGPT 登录.",
                "Refresh the ChatGPT sign-in on your paired computer.",
            )
        RemodexGptVoiceStatus.VOICE_SYNC_IN_PROGRESS ->
            remodexLocalizedText(
                "已登录. 正在等待电脑同步语音状态.",
                "Signed in. Waiting for voice sync from your computer.",
            )
        RemodexGptVoiceStatus.LOGIN_PENDING ->
            remodexLocalizedText(
                "请在配对电脑的浏览器中完成登录.",
                "Finish the browser sign-in flow on your paired computer.",
            )
        RemodexGptVoiceStatus.DISCONNECTED ->
            remodexLocalizedText(
                "检查 ChatGPT 语音前, 请先连接配对电脑.",
                "Connect to your paired computer before checking ChatGPT voice.",
            )
        RemodexGptVoiceStatus.LOGIN_REQUIRED ->
            remodexLocalizedText(
                "请在配对电脑上登录 ChatGPT, 不需要在手机上登录.",
                "Sign in to ChatGPT on the paired computer, not on this phone.",
            )
    }
}
