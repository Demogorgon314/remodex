package com.emanueledipietro.remodex.model

import java.util.Base64

private const val InlineImageFallbackNamePrefix = "image-"
private val InlineImageDataUrlPrefixRegex = Regex(
    pattern = "^data:image/[^;,]+(?:;[^,]+)*;base64,",
    option = RegexOption.IGNORE_CASE,
)

fun isInlineImageDataUrl(value: String?): Boolean {
    return value
        ?.trim()
        ?.let { InlineImageDataUrlPrefixRegex.containsMatchIn(it) }
        ?: false
}

fun fallbackConversationImageDisplayName(
    uriString: String,
    attachmentIndex: Int,
): String {
    val fallbackName = "$InlineImageFallbackNamePrefix${attachmentIndex + 1}"
    val trimmed = uriString.trim()
    if (trimmed.isEmpty() || isInlineImageDataUrl(trimmed)) {
        return fallbackName
    }
    return trimmed
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .ifBlank { fallbackName }
}

fun decodeInlineImageDataUrlBytes(value: String?): ByteArray? {
    val trimmed = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val prefixMatch = InlineImageDataUrlPrefixRegex.find(trimmed) ?: return null
    val encodedPayload = trimmed.substring(prefixMatch.range.last + 1).trim()
    if (encodedPayload.isEmpty()) {
        return null
    }
    return runCatching { Base64.getDecoder().decode(encodedPayload) }.getOrNull()
}
