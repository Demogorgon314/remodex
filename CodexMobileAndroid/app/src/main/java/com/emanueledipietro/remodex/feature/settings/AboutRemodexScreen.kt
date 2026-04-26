package com.emanueledipietro.remodex.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AboutRemodexScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        AboutHeader()
        AboutSection(
            title = "How It Works",
            paragraphs = listOf(
                "Your computer runs a lightweight bridge that connects to a relay server over WebSocket.",
            ),
            bullets = listOf(
                "You send a prompt from your phone",
                "It travels through the relay to the bridge on your computer",
                "The bridge forwards it to codex app-server via JSON-RPC",
                "Responses stream back the same path in real time",
            ),
            callout = AboutCallout(
                accent = Color(0xFF2AAE67),
                text = "All execution happens locally on your computer. Code generation, tool use, and file edits never run on the relay.",
            ),
        )
        AboutArchitectureSection()
        AboutSection(
            title = "The Relay",
            paragraphs = listOf(
                "A lightweight WebSocket server that routes messages between your Android phone and your computer.",
                "You can self-host the relay on your own VPS, or use the default endpoint from the npm package.",
            ),
            bullets = listOf(
                "Handles session discovery so your phone finds the computer's live session",
                "Never sees decrypted message contents after the handshake",
                "Only observes connection metadata such as session IDs and timing",
            ),
        )
        AboutSection(
            title = "Codex App-Server",
            paragraphs = listOf(
                "The bridge spawns a codex app-server process, the same JSON-RPC interface behind the Codex desktop app and IDE extensions.",
            ),
            bullets = listOf(
                "Phone conversations are first-class Codex sessions",
                "Produces JSONL rollout files under ~/.codex/sessions/",
                "Threads started from your phone show up in Codex.app",
            ),
            callout = AboutCallout(
                accent = Color(0xFFF29D38),
                text = "Already have a running Codex instance? Point the bridge at it with REMODEX_CODEX_ENDPOINT instead of spawning a new one.",
            ),
        )
        AboutSection(
            title = "Pairing & Security",
            paragraphs = listOf(
                "On first connect, the bridge prints a QR code containing the relay URL, the session ID, and the bridge identity public key.",
                "Scan it once from this app. Later launches auto-reconnect without another QR unless trust changes or the session can’t be resolved.",
            ),
            bullets = listOf(
                "Android saves the paired computer as a trusted device locally",
                "The bridge persists your phone's identity on the computer",
                "The QR stays available as a recovery path",
            ),
        )
        AboutSection(
            title = "End-to-End Encryption",
            paragraphs = listOf(
                "After pairing, every message is wrapped in encrypted envelopes.",
            ),
            specs = listOf(
                "Cipher" to "AES-256-GCM",
                "Key derivation" to "HKDF-SHA256, per-direction keys",
                "Key exchange" to "X25519 ephemeral",
                "Identity" to "Ed25519 signatures",
                "Replay protection" to "Monotonic counters",
                "At-rest (Android)" to "Keystore-backed encrypted state",
            ),
        )
        AboutSection(
            title = "Git & Workspace",
            paragraphs = listOf(
                "The bridge handles git commands from your phone locally on the computer.",
            ),
            bullets = listOf(
                "git/status, git/commit, git/push, git/pull, git/branches",
                "git/checkout, git/createBranch, git/log, git/stash, git/stashPop",
                "workspace revert preview and apply for assistant-made changes",
            ),
        )
        AboutSection(
            title = "Connection Resilience",
            bullets = listOf(
                "Auto-reconnect with exponential backoff",
                "Bounded outbound buffer re-sends missed encrypted messages",
                "The Codex process stays alive across transient disconnects",
                "SIGINT / SIGTERM trigger a clean shutdown",
            ),
        )
        AboutSection(
            title = "Desktop App Integration",
            paragraphs = listOf(
                "Threads from your phone are persisted as JSONL rollout files, so they appear in Codex.app on your Mac.",
            ),
            callout = AboutCallout(
                accent = Color(0xFF5E9BFF),
                text = "The desktop app doesn't live-reload external writes. Use the handoff flow to keep working on the same thread from your Mac.",
            ),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Remodex",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "ISC License",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AboutHeader() {
    AboutCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Remodex",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Control Codex from your Android phone.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AboutCalloutCard(
                callout = AboutCallout(
                    accent = Color(0xFF52A7FF),
                    text = "The Codex runtime stays on your computer. Your phone is a secure remote control connected through a relay.",
                ),
            )
        }
    }
}

private data class AboutCallout(
    val accent: Color,
    val text: String,
)

@Composable
private fun AboutArchitectureSection() {
    AboutCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AboutSectionTitle("Architecture")
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.92f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ArchitectureStep(
                        from = "Remodex Android",
                        via = "WebSocket",
                        to = "Bridge (computer)",
                    )
                    ArchitectureStep(
                        from = "Bridge (computer)",
                        via = "JSON-RPC",
                        to = "codex app-server",
                    )
                    ArchitectureStep(
                        from = "codex app-server",
                        via = "JSONL rollout",
                        to = "~/.codex/sessions",
                        isLast = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchitectureStep(
    from: String,
    via: String,
    to: String,
    isLast: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = from,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = via,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = to,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (!isLast) {
            Box(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(width = 1.dp, height = 12.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

@Composable
private fun AboutSection(
    title: String,
    paragraphs: List<String> = emptyList(),
    bullets: List<String> = emptyList(),
    specs: List<Pair<String, String>> = emptyList(),
    callout: AboutCallout? = null,
) {
    AboutCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AboutSectionTitle(title)
            paragraphs.forEach { paragraph ->
                Text(
                    text = paragraph,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (bullets.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bullets.forEach { bullet ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "->",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = bullet,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (specs.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    specs.forEach { (label, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.weight(0.4f),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = value,
                                modifier = Modifier.weight(0.6f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
            callout?.let { AboutCalloutCard(callout = it) }
        }
    }
}

@Composable
private fun AboutCalloutCard(
    callout: AboutCallout,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(callout.accent.copy(alpha = 0.16f), RoundedCornerShape(8.dp)),
            )
            Text(
                text = callout.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AboutSectionTitle(
    title: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun AboutCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}
