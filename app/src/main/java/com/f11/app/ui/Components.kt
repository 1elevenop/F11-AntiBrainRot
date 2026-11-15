package com.f11.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------- dot-matrix numerals

private val GLYPHS: Map<Char, List<String>> = mapOf(
    '0' to listOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
    '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    '2' to listOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
    '3' to listOf("11111", "00010", "00100", "00010", "00001", "10001", "01110"),
    '4' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    '5' to listOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
    '6' to listOf("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
    '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    '9' to listOf("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
    ':' to listOf("0", "0", "1", "0", "1", "0", "0"),
    '.' to listOf("0", "0", "0", "0", "0", "0", "1"),
    '-' to listOf("000", "000", "000", "111", "000", "000", "000"),
    ' ' to listOf("00", "00", "00", "00", "00", "00", "00"),
)

/** Numbers drawn as a 5×7 LED dot grid, like the reference design. */
@Composable
fun DotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    dot: Dp = 4.dp,
    color: Color = F11Palette.Text,
    ghost: Color = Color.Transparent,
) {
    val glyphs = text.map { GLYPHS[it] ?: GLYPHS.getValue(' ') }
    val columns = glyphs.sumOf { it.first().length } + (glyphs.size - 1).coerceAtLeast(0)
    val pitch = dot * 1.6f
    Canvas(
        modifier
            .size(width = pitch * columns, height = pitch * 7)
            .clearAndSetSemantics { contentDescription = text },
    ) {
        val p = pitch.toPx()
        val r = dot.toPx() / 2f
        var x0 = 0
        glyphs.forEach { rows ->
            rows.forEachIndexed { y, row ->
                row.forEachIndexed { x, bit ->
                    val c = if (bit == '1') color else ghost
                    if (c != Color.Transparent) {
                        drawCircle(c, r, Offset((x0 + x) * p + p / 2f, y * p + p / 2f))
                    }
                }
            }
            x0 += rows.first().length + 1
        }
    }
}

// ---------------------------------------------------------------- surfaces

@Composable
fun GradientCard(
    brush: Brush,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(36.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .clip(shape)
            .background(brush)
            .border(1.dp, F11Palette.Hairline, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        content = content,
    )
}

@Composable
fun Tile(
    modifier: Modifier = Modifier,
    brush: Brush? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .clip(shape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(F11Palette.Tile))
            .border(1.dp, F11Palette.Hairline, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(18.dp),
        content = content,
    )
}

@Composable
fun TileLabel(text: String, modifier: Modifier = Modifier, color: Color = F11Palette.Text) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = color, modifier = modifier)
}

@Composable
fun F11Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = F11Palette.Violet,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = F11Palette.TileHigh,
            uncheckedBorderColor = Color.Transparent,
            // Locked-but-on (e.g. a running schedule) must still read as "on".
            disabledCheckedThumbColor = Color.White.copy(alpha = 0.7f),
            disabledCheckedTrackColor = F11Palette.Violet.copy(alpha = 0.45f),
            disabledUncheckedTrackColor = F11Palette.Tile,
            disabledUncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** Row of dots marking which pager page is showing. */
@Composable
fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier) {
        repeat(count) { i ->
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (i == current) 6.dp else 4.dp)
                    .clip(CircleShape)
                    .background(if (i == current) F11Palette.Text else F11Palette.TextFaint)
                    .align(Alignment.CenterVertically),
            )
        }
    }
}

/** A row of dots, the last [lit] of which glow; used as a quiet progress track. */
@Composable
fun DotTrack(total: Int, lit: Int, modifier: Modifier = Modifier, accent: Color = F11Palette.Lime) {
    Canvas(modifier.height(8.dp)) {
        if (total <= 0) return@Canvas
        val step = size.width / total
        repeat(total) { i ->
            val on = i < lit
            drawCircle(
                color = if (on) accent else F11Palette.TextFaint,
                radius = if (on) 2.6.dp.toPx() else 1.6.dp.toPx(),
                center = Offset(step * i + step / 2f, size.height / 2f),
            )
        }
    }
}

/**
 * Dot-column chart: each value is a stack of dots, lit up to its share of the max.
 * [highlightLast] lights today's column in the accent colour.
 */
@Composable
fun DotColumnChart(
    values: List<Int>,
    modifier: Modifier = Modifier,
    rows: Int = 12,
    accent: Color = F11Palette.Lime,
    highlightLast: Boolean = true,
) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Canvas(modifier.semantics { contentDescription = "Swipes per day: ${values.joinToString()}" }) {
        if (values.isEmpty()) return@Canvas
        val colW = size.width / values.size
        val rowH = size.height / rows
        val r = (minOf(colW, rowH) * 0.28f).coerceAtMost(4.dp.toPx())
        values.forEachIndexed { i, v ->
            val lit = if (v == 0) 0 else ((v.toFloat() / max) * rows).toInt().coerceIn(1, rows)
            for (row in 0 until rows) {
                val on = row < lit
                val color = when {
                    !on -> F11Palette.TextFaint.copy(alpha = 0.45f)
                    highlightLast && i == values.lastIndex -> accent
                    else -> F11Palette.Text
                }
                drawCircle(color, if (on) r else r * 0.55f,
                    Offset(colW * i + colW / 2f, size.height - rowH * row - rowH / 2f))
            }
        }
    }
}

/** Decorative arcs with dots on top, echoing the reference card art. Heights follow [values]. */
@Composable
fun ArcDots(values: List<Int>, modifier: Modifier = Modifier) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val step = size.width / values.size
        val arcR = step * 0.95f
        values.forEachIndexed { i, v ->
            val cx = step * i + step / 2f
            drawArc(
                color = Color.White.copy(alpha = 0.14f),
                startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(cx - arcR, size.height - arcR),
                size = Size(arcR * 2, arcR * 2),
                style = Stroke(width = 1.dp.toPx()),
            )
            val y = size.height - (v.toFloat() / max) * (size.height * 0.8f) - 6.dp.toPx()
            drawCircle(Color.White, 3.dp.toPx(), Offset(cx, y))
        }
    }
}

// ---------------------------------------------------------------- tiny drawn icons

@Composable
fun CircleIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: @Composable () -> Unit,
) {
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(F11Palette.Tile)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) { glyph() }
}

@Composable
fun MenuGlyph(color: Color = F11Palette.Text) {
    Canvas(Modifier.size(18.dp)) {
        val w = 1.6.dp.toPx()
        listOf(0.25f, 0.5f, 0.75f).forEach { f ->
            drawLine(color, Offset(size.width * 0.12f, size.height * f), Offset(size.width * 0.88f, size.height * f), w, StrokeCap.Round)
        }
    }
}

@Composable
fun BackGlyph(color: Color = F11Palette.Text) {
    Canvas(Modifier.size(18.dp)) {
        val w = 1.8.dp.toPx()
        val mid = size.height / 2f
        drawLine(color, Offset(size.width * 0.15f, mid), Offset(size.width * 0.85f, mid), w, StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.15f, mid), Offset(size.width * 0.45f, size.height * 0.2f), w, StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.15f, mid), Offset(size.width * 0.45f, size.height * 0.8f), w, StrokeCap.Round)
    }
}

@Composable
fun ChartGlyph(color: Color = F11Palette.Text) {
    Canvas(Modifier.size(18.dp)) {
        val r = 1.5.dp.toPx()
        val cols = listOf(2, 4, 3)
        cols.forEachIndexed { i, h ->
            repeat(h) { j ->
                drawCircle(color, r, Offset(size.width * (0.22f + i * 0.28f), size.height * (0.88f - j * 0.22f)))
            }
        }
    }
}

@Composable
fun MicGlyph(color: Color = F11Palette.Text) {
    Canvas(Modifier.size(20.dp)) {
        val w = 1.8.dp.toPx()
        val cx = size.width / 2f
        drawRoundRect(
            color, topLeft = Offset(cx - size.width * 0.17f, size.height * 0.05f),
            size = Size(size.width * 0.34f, size.height * 0.55f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.17f),
        )
        drawArc(color, 0f, 180f, false, Offset(size.width * 0.18f, size.height * 0.22f),
            Size(size.width * 0.64f, size.height * 0.5f), style = Stroke(w, cap = StrokeCap.Round))
        drawLine(color, Offset(cx, size.height * 0.72f), Offset(cx, size.height * 0.92f), w, StrokeCap.Round)
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = F11Palette.TextDim,
        modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp))
}

@Composable
fun SpacerH(dp: Dp) = Spacer(Modifier.height(dp))

@Composable
fun SpacerW(dp: Dp) = Spacer(Modifier.width(dp))
