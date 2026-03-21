package com.emanueledipietro.remodex.platform.notifications

private const val ThreadRoutePrefix = "remodex://thread/"

fun buildThreadRoute(threadId: String): String = "$ThreadRoutePrefix$threadId"

fun parseThreadIdFromRoute(route: String?): String? {
    if (route.isNullOrBlank() || !route.startsWith(ThreadRoutePrefix)) {
        return null
    }
    return route.removePrefix(ThreadRoutePrefix).takeIf(String::isNotBlank)
}
