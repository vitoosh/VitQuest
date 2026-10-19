package io.github.vitoosh.vitquest.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.data.repository.HealthConnectStatus
import io.github.vitoosh.vitquest.core.data.repository.PhoneSensorStatus
import io.github.vitoosh.vitquest.core.data.repository.StepSourcesStatus
import io.github.vitoosh.vitquest.core.designsystem.theme.VitQuestTheme
import io.github.vitoosh.vitquest.core.domain.steps.StepStats
import io.github.vitoosh.vitquest.core.model.DailySteps
import io.github.vitoosh.vitquest.core.model.StepSource
import io.github.vitoosh.vitquest.core.model.StepSourcePreference
import io.github.vitoosh.vitquest.feature.activity.ActivityPlatformActions
import io.github.vitoosh.vitquest.feature.activity.ActivityScreen
import io.github.vitoosh.vitquest.feature.activity.ActivityUiState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en")
class ActivityScreenSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.parse("2026-10-19")

    private fun string(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private fun state(todaySteps: DailySteps?, status: StepSourcesStatus): ActivityUiState.Ready {
        val byDate = buildMap {
            todaySteps?.let { put(today, it) }
            put(today.minusDays(2), DailySteps(today.minusDays(2), 9_100, StepSource.MANUAL))
        }
        val history = StepStats.days(today.minusDays(29), today, byDate)
        val week = history.takeLast(7)
        return ActivityUiState.Ready(
            today = history.last(),
            goal = 8_000,
            week = week,
            weekSummary = StepStats.summarize(week, 8_000),
            history = history.reversed(),
            status = status,
            preference = StepSourcePreference.AUTOMATIC,
        )
    }

    private val noActions = ActivityPlatformActions({}, {}, {}, {}, {})

    @Test
    fun showsTodaysStepsWithSource() {
        composeRule.setContent {
            VitQuestTheme {
                ActivityScreen(
                    uiState = state(
                        DailySteps(today, 6_540, StepSource.PHONE_SENSOR),
                        StepSourcesStatus(HealthConnectStatus.PERMISSION_REQUIRED, PhoneSensorStatus.ACTIVE),
                    ),
                    sensorPermanentlyDenied = false,
                    actions = noActions,
                    onSaveManualSteps = { _, _ -> null },
                    onClearManualSteps = {},
                    onSaveStepGoal = { null },
                    onPreferenceSelected = {},
                    onRetry = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("6,540").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.steps_remaining, "1,460", "8,000")).assertIsDisplayed()
    }

    @Test
    fun missingDataIsNotShownAsZeroAndConnectAsksFirst() {
        var connectCalls = 0
        composeRule.setContent {
            VitQuestTheme {
                ActivityScreen(
                    uiState = state(null, StepSourcesStatus(HealthConnectStatus.PERMISSION_REQUIRED, PhoneSensorStatus.PERMISSION_REQUIRED)),
                    sensorPermanentlyDenied = false,
                    actions = noActions.copy(connectHealthConnect = { connectCalls++ }),
                    onSaveManualSteps = { _, _ -> null },
                    onClearManualSteps = {},
                    onSaveStepGoal = { null },
                    onPreferenceSelected = {},
                    onRetry = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.steps_no_data_today)).assertIsDisplayed()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(string(R.string.hc_status_permission_required)))
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(string(R.string.action_connect)))
        composeRule.onNodeWithText(string(R.string.action_connect)).performClick()
        // The explanation dialog is shown before the system permission prompt.
        composeRule.onNodeWithText(string(R.string.rationale_health_connect_title)).assertIsDisplayed()
        assertEquals(0, connectCalls)
        composeRule.onNodeWithText(string(R.string.action_continue)).performClick()
        assertEquals(1, connectCalls)
    }

    @Test
    fun manualEntryValidatesInput() {
        var saved: Pair<LocalDate, String>? = null
        composeRule.setContent {
            VitQuestTheme {
                ActivityScreen(
                    uiState = state(null, StepSourcesStatus(HealthConnectStatus.NOT_SUPPORTED, PhoneSensorStatus.NOT_AVAILABLE)),
                    sensorPermanentlyDenied = false,
                    actions = noActions,
                    onSaveManualSteps = { date, input -> saved = date to input; null },
                    onClearManualSteps = {},
                    onSaveStepGoal = { null },
                    onPreferenceSelected = {},
                    onRetry = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.steps_log_manually)).performClick()
        composeRule.onNodeWithText(string(R.string.manual_steps_title, string(R.string.day_today))).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_save)).performClick()
        assertEquals(today, saved?.first)
    }

}
