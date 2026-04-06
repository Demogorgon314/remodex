package com.emanueledipietro.remodex.feature.turn

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

@Composable
internal fun RemodexGitBranchGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.14f
        val nodeRadius = size.minDimension * 0.16f
        val leftX = size.width * 0.3f
        val topY = size.height * 0.24f
        val bottomY = size.height * 0.78f
        val rightX = size.width * 0.74f
        val rightY = size.height * 0.5f

        drawLine(
            color = color,
            start = Offset(leftX, topY + nodeRadius),
            end = Offset(leftX, bottomY - nodeRadius),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(leftX + nodeRadius * 0.9f, topY + nodeRadius * 0.5f),
            end = Offset(rightX - nodeRadius, rightY - nodeRadius * 0.2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawCircle(color = color, radius = nodeRadius, center = Offset(leftX, topY))
        drawCircle(color = color, radius = nodeRadius, center = Offset(leftX, bottomY))
        drawCircle(color = color, radius = nodeRadius, center = Offset(rightX, rightY))
    }
}

@Composable
internal fun RemodexGitCommitGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.14f
        val nodeRadius = size.minDimension * 0.16f
        val center = Offset(size.width / 2f, size.height / 2f)
        val sideInset = size.width * 0.16f

        drawLine(
            color = color,
            start = Offset(sideInset, center.y),
            end = Offset(center.x - nodeRadius * 1.4f, center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(center.x + nodeRadius * 1.4f, center.y),
            end = Offset(size.width - sideInset, center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawCircle(color = color, radius = nodeRadius, center = center)
    }
}
