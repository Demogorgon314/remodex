package com.emanueledipietro.remodex.feature.turn

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.emanueledipietro.remodex.ui.theme.RemodexConversationChrome
import com.emanueledipietro.remodex.ui.theme.remodexConversationChrome

private val DiffAdditionColor = Color(0xFF22A653)
private val DiffDeletionColor = Color(0xFFE04646)
private val DiffHunkColor = Color(0xFFB3BDD9)

internal enum class ConversationDiffLineKind {
    ADDITION,
    DELETION,
    HUNK,
    META,
    NEUTRAL;

    companion object {
        fun classify(line: String): ConversationDiffLineKind {
            return when {
                line.startsWith("@@") -> HUNK
                line.startsWith("diff ")
                    || line.startsWith("index ")
                    || line.startsWith("---")
                    || line.startsWith("+++")
                    || line.startsWith("new file mode")
                    || line.startsWith("deleted file mode")
                    || line.startsWith("old mode ")
                    || line.startsWith("new mode ")
                    || line.startsWith("rename from ")
                    || line.startsWith("rename to ")
                    || line.startsWith("similarity index ")
                    || line.startsWith("dissimilarity index ") -> META
                line.startsWith("+") && !line.startsWith("+++") -> ADDITION
                line.startsWith("-") && !line.startsWith("---") -> DELETION
                else -> NEUTRAL
            }
        }
    }

    fun cleanDisplayText(line: String): String {
        return when (this) {
            ADDITION, DELETION -> line
            NEUTRAL -> if (line.startsWith(" ")) line.drop(1) else line
            HUNK, META -> line
        }
    }

    fun textColor(chrome: RemodexConversationChrome): Color {
        return when (this) {
            ADDITION -> DiffAdditionColor
            DELETION -> DiffDeletionColor
            HUNK -> DiffHunkColor
            META -> chrome.secondaryText
            NEUTRAL -> chrome.bodyText
        }
    }

    fun backgroundColor(): Color {
        return when (this) {
            ADDITION -> Color(0x1F1A7342)
            DELETION -> Color(0x1F8C2E2E)
            HUNK, META, NEUTRAL -> Color.Transparent
        }
    }

    fun indicatorColor(): Color {
        return when (this) {
            ADDITION -> DiffAdditionColor
            DELETION -> DiffDeletionColor
            HUNK, META, NEUTRAL -> Color.Transparent
        }
    }

    fun hasIndicator(): Boolean {
        return this == ADDITION || this == DELETION
    }
}

internal fun shouldRenderMarkdownCodeBlockAsDiff(language: String?): Boolean {
    return language?.trim()?.equals("diff", ignoreCase = true) == true
}

@Composable
internal fun ConversationCleanDiffCodeBlock(
    code: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    enablesSelection: Boolean = true,
    onLongPress: ((IntOffset) -> Unit)? = null,
) {
    val chrome = remodexConversationChrome()
    val lines = remember(code) {
        code.split('\n')
    }
    val gestureModifier = if (!enablesSelection && onLongPress != null) {
        Modifier
            .pointerInput(onLongPress) {
                detectTapGestures(
                    onLongPress = { offset ->
                        onLongPress(
                            IntOffset(
                                x = offset.x.toInt(),
                                y = offset.y.toInt(),
                            ),
                        )
                    },
                )
            }
            .semantics {
                onLongClick {
                    onLongPress(IntOffset.Zero)
                    true
                }
            }
    } else {
        Modifier
    }

    @Composable
    fun DiffContent() {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .then(gestureModifier)
                .padding(contentPadding),
        ) {
            lines.forEach { line ->
                val kind = ConversationDiffLineKind.classify(line)
                when (kind) {
                    ConversationDiffLineKind.META -> Unit
                    ConversationDiffLineKind.HUNK -> {
                        HorizontalDivider(
                            color = chrome.subtleBorder,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(kind.backgroundColor()),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .width(if (kind.hasIndicator()) 2.dp else 0.dp)
                                        .background(kind.indicatorColor()),
                                )
                                Text(
                                    text = kind.cleanDisplayText(line),
                                    modifier = Modifier
                                        .weight(1f, fill = true)
                                        .padding(horizontal = 10.dp, vertical = 1.dp),
                                    style = style,
                                    color = kind.textColor(chrome),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (enablesSelection) {
        SelectionContainer {
            DiffContent()
        }
    } else {
        DiffContent()
    }
}
