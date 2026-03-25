package com.emanueledipietro.remodex.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

@Immutable
data class RemodexConversationChrome(
    val canvas: Color,
    val shellSurface: Color,
    val panelSurface: Color,
    val panelSurfaceStrong: Color,
    val mutedSurface: Color,
    val nestedSurface: Color,
    val userBubble: Color,
    val userBubbleBorder: Color,
    val subtleBorder: Color,
    val titleText: Color,
    val bodyText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val accent: Color,
    val accentSurface: Color,
    val warning: Color,
    val destructive: Color,
    val sendButton: Color,
    val sendIcon: Color,
)

object RemodexConversationShapes {
    val shell = RoundedCornerShape(28.dp)
    val panel = RoundedCornerShape(24.dp)
    val card = RoundedCornerShape(20.dp)
    val nestedCard = RoundedCornerShape(18.dp)
    val bubble = RoundedCornerShape(24.dp)
    val composer = RoundedCornerShape(28.dp)
    val pill = RoundedCornerShape(999.dp)
}

@Composable
fun remodexConversationChrome(): RemodexConversationChrome {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.45f

    return if (isDark) {
        RemodexConversationChrome(
            canvas = Color(0xFF101317),
            shellSurface = Color(0xD9181C22),
            panelSurface = Color(0xD9212630),
            panelSurfaceStrong = Color(0xED171C24),
            mutedSurface = Color(0xD92A303B),
            nestedSurface = Color(0xE62E3540),
            userBubble = Color(0xFF262E39),
            userBubbleBorder = Color(0x29FFFFFF),
            subtleBorder = Color(0x14FFFFFF),
            titleText = Color(0xFFF4F7FA),
            bodyText = Color(0xFFF0F3F7),
            secondaryText = Color(0xFFBDC6D3),
            tertiaryText = Color(0xFF8C97A8),
            accent = Color(0xFF7CB5FF),
            accentSurface = Color(0x1F7CB5FF),
            warning = Color(0xFFFFC46D),
            destructive = scheme.error,
            sendButton = Color(0xFFF4F7FA),
            sendIcon = Color(0xFF101317),
        )
    } else {
        RemodexConversationChrome(
            canvas = Color(0xFFF5F7FA),
            shellSurface = Color(0xDBFFFFFF),
            panelSurface = Color(0xD9FFFFFF),
            panelSurfaceStrong = Color(0xF0FFFFFF),
            mutedSurface = Color(0xFFF0F3F7),
            nestedSurface = Color(0xFFF6F8FB),
            userBubble = Color(0xFFEFF2F6),
            userBubbleBorder = Color(0x100F172A),
            subtleBorder = Color(0x120F172A),
            titleText = Color(0xFF171A20),
            bodyText = Color(0xFF171A20),
            secondaryText = Color(0xFF677181),
            tertiaryText = Color(0xFF8B95A5),
            accent = Color(0xFF2563EB),
            accentSurface = Color(0x132563EB),
            warning = Color(0xFFB97518),
            destructive = scheme.error,
            sendButton = Color(0xFF171A20),
            sendIcon = Color(0xFFF8FAFC),
        )
    }
}
