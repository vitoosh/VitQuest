package io.github.vitoosh.vitquest.feature.activity

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.steps.StepDay
import io.github.vitoosh.vitquest.core.ui.dayLabel
import io.github.vitoosh.vitquest.core.ui.formatInteger
import io.github.vitoosh.vitquest.core.ui.shortWeekday
import java.time.LocalDate
import kotlin.math.ceil

private val BAR_MAX_WIDTH = 24.dp
private val BAR_CORNER = 4.dp
private val CHART_HEIGHT = 140.dp

/**
 * Single-series column chart of the last seven days. Tapping a day shows its
 * exact value above the chart; the history list below is the table view.
 * Days without data have no column, which keeps "no data" distinct from 0.
 */
@Composable
fun WeeklyStepsChart(
    days: List<StepDay>,
    goal: Int?,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable(days.size) { mutableIntStateOf(days.lastIndex) }
    val selectedDay = days.getOrNull(selected)
    val today = days.lastOrNull()?.date

    val barColor = MaterialTheme.colorScheme.primary
    val goalColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val maxValue = niceMax(maxOf(days.maxOfOrNull { it.steps?.steps ?: 0 } ?: 0, goal ?: 0))

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
        if (selectedDay != null && today != null) {
            Text(
                text = dayLabel(selectedDay.date, today) + " · " + (
                    selectedDay.steps?.let { stringResource(R.string.value_steps, formatInteger(it.steps)) }
                        ?: stringResource(R.string.steps_no_data_short)
                    ),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val slot = size.width / days.size
                val barWidth = minOf(BAR_MAX_WIDTH.toPx(), slot * 0.6f)
                fun y(value: Int) = size.height - size.height * value / maxValue

                // Baseline hairline.
                drawLine(gridColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())

                days.forEachIndexed { index, day ->
                    val steps = day.steps?.steps ?: return@forEachIndexed
                    val left = slot * index + (slot - barWidth) / 2
                    val top = y(steps).coerceAtMost(size.height - 2.dp.toPx())
                    val corner = CornerRadius(minOf(BAR_CORNER.toPx(), (size.height - top) / 2))
                    // Rounded data end, square at the baseline.
                    val path = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = left,
                                top = top,
                                right = left + barWidth,
                                bottom = size.height,
                                topLeftCornerRadius = corner,
                                topRightCornerRadius = corner,
                            ),
                        )
                    }
                    drawPath(path, barColor)
                }

                if (goal != null && goal > 0) {
                    val goalY = y(goal)
                    drawLine(goalColor, Offset(0f, goalY), Offset(size.width, goalY), strokeWidth = 2.dp.toPx())
                }
            }
            // Tap targets cover the whole column slot, larger than the bar itself.
            Row(modifier = Modifier.fillMaxSize()) {
                days.forEachIndexed { index, day ->
                    val description = dayDescription(day, today)
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
            days.forEachIndexed { index, day ->
                // The selected day is marked on its label, not with an extra shape that could read as data.
                val isSelected = index == selected
                Text(
                    text = shortWeekday(day.date),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = if (isSelected) TextDecoration.Underline else null,
                    modifier = Modifier.weight(1f).padding(top = VqSpacing.xs),
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (goal != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                Canvas(modifier = Modifier.height(2.dp).fillMaxWidth(0.06f)) {
                    drawLine(goalColor, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 2.dp.toPx())
                }
                Text(
                    text = stringResource(R.string.steps_goal_line, formatInteger(goal)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun dayDescription(day: StepDay, today: LocalDate?): String {
    val label = if (today != null) dayLabel(day.date, today) else day.date.toString()
    val value = day.steps?.let { stringResource(R.string.value_steps, formatInteger(it.steps)) }
        ?: stringResource(R.string.steps_no_data_short)
    return "$label: $value"
}

/** Rounds the axis maximum up to a clean value so the goal line never touches the top. */
private fun niceMax(value: Int): Int {
    if (value <= 0) return 1_000
    val padded = value * 1.15
    val step = when {
        padded <= 5_000 -> 1_000.0
        padded <= 20_000 -> 2_000.0
        else -> 5_000.0
    }
    return (ceil(padded / step) * step).toInt()
}
