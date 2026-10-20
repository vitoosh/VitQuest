package io.github.vitoosh.vitquest.feature.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.model.WeightEntry
import io.github.vitoosh.vitquest.core.ui.formatDecimal
import io.github.vitoosh.vitquest.core.ui.shortDate
import java.time.temporal.ChronoUnit

/**
 * Single-series line of weight over time with the target as a reference line.
 * The x axis is real time, so gaps between weigh-ins stay visible.
 * The history list below the chart is the table view.
 */
@Composable
fun WeightChart(entries: List<WeightEntry>, targetKg: Double?, modifier: Modifier = Modifier) {
    if (entries.size < 2) {
        Text(
            text = stringResource(R.string.weight_chart_need_more),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
    val sorted = entries.sortedBy { it.date }
    val lineColor = MaterialTheme.colorScheme.primary
    val targetColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    val values = sorted.map { it.weightKg } + listOfNotNull(targetKg)
    val min = values.min() - 1.0
    val max = values.max() + 1.0
    val firstDay = sorted.first().date
    val spanDays = ChronoUnit.DAYS.between(firstDay, sorted.last().date).coerceAtLeast(1)
    val description = pluralStringResource(
        R.plurals.weight_chart_description,
        sorted.size,
        formatDecimal(sorted.first().weightKg),
        formatDecimal(sorted.last().weightKg),
        sorted.size,
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .semantics { contentDescription = description },
        ) {
            fun x(entry: WeightEntry) = size.width * ChronoUnit.DAYS.between(firstDay, entry.date) / spanDays
            fun y(kg: Double) = (size.height * (1 - (kg - min) / (max - min))).toFloat()

            drawLine(gridColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            if (targetKg != null) {
                drawLine(targetColor, Offset(0f, y(targetKg)), Offset(size.width, y(targetKg)), strokeWidth = 2.dp.toPx())
            }
            val path = Path().apply {
                sorted.forEachIndexed { index, entry ->
                    val point = Offset(x(entry), y(entry.weightKg))
                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
            }
            drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            // End marker with a surface ring, as the latest value is the one that matters most.
            val last = sorted.last()
            val end = Offset(x(last), y(last.weightKg))
            drawCircle(surface, radius = 6.dp.toPx(), center = end)
            drawCircle(lineColor, radius = 4.dp.toPx(), center = end)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = shortDate(firstDay),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = shortDate(sorted.last().date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (targetKg != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                Canvas(modifier = Modifier.height(2.dp).fillMaxWidth(0.06f)) {
                    drawLine(targetColor, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 2.dp.toPx())
                }
                Text(
                    text = stringResource(R.string.weight_target_line, formatDecimal(targetKg)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
