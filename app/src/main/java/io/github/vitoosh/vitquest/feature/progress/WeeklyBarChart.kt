package io.github.vitoosh.vitquest.feature.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private val BAR_MAX_WIDTH = 24.dp
private val BAR_CORNER = 4.dp
private val CHART_HEIGHT = 120.dp

data class ChartBar(val label: String, val description: String, val value: Int?)

/**
 * Single-series weekly column chart, styled like the steps chart. A null value
 * draws no column, keeping "no data" distinct from zero; tapping a column
 * shows its exact value in the header.
 */
@Composable
fun WeeklyBarChart(bars: List<ChartBar>, valueText: @Composable (Int) -> String, emptyText: String, modifier: Modifier = Modifier) {
    var selected by rememberSaveable(bars.size) { mutableIntStateOf(bars.lastIndex) }
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val maxValue = niceMax(bars.maxOfOrNull { it.value ?: 0 } ?: 0)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
        bars.getOrNull(selected)?.let { bar ->
            Text(
                text = bar.description + " · " + (bar.value?.let { valueText(it) } ?: emptyText),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val slot = size.width / bars.size
                val barWidth = minOf(BAR_MAX_WIDTH.toPx(), slot * 0.6f)
                drawLine(gridColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                bars.forEachIndexed { index, bar ->
                    val value = bar.value?.takeIf { it > 0 } ?: return@forEachIndexed
                    val left = slot * index + (slot - barWidth) / 2
                    val top = (size.height - size.height * value / maxValue).coerceAtMost(size.height - 2.dp.toPx())
                    val corner = CornerRadius(minOf(BAR_CORNER.toPx(), (size.height - top) / 2))
                    drawPath(
                        Path().apply {
                            addRoundRect(RoundRect(left, top, left + barWidth, size.height, topLeftCornerRadius = corner, topRightCornerRadius = corner))
                        },
                        barColor,
                    )
                }
            }
            Row(modifier = Modifier.fillMaxSize()) {
                bars.forEachIndexed { index, bar ->
                    val description = bar.description + ": " + (bar.value?.let { valueText(it) } ?: emptyText)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .semantics {
                                contentDescription = description
                                this.selected = index == selected
                            }
                            .clickable(role = Role.Button) { selected = index },
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            bars.forEachIndexed { index, bar ->
                val isSelected = index == selected
                Text(
                    text = bar.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = if (isSelected) TextDecoration.Underline else null,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(top = VqSpacing.xs),
                )
            }
        }
    }
}

/** Rounds the axis maximum up to 1, 2 or 5 × a power of ten, with a little headroom. */
private fun niceMax(value: Int): Int {
    if (value <= 0) return 1
    val padded = value * 1.1
    val magnitude = 10.0.pow(floor(log10(padded)))
    val step = listOf(1.0, 2.0, 5.0, 10.0).first { it * magnitude >= padded }
    return ceil(step * magnitude).toInt().coerceAtLeast(1)
}
