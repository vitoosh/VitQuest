package io.github.vitoosh.vitquest.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.theme.VitQuestTheme
import io.github.vitoosh.vitquest.core.domain.steps.StepDiagnostics
import io.github.vitoosh.vitquest.core.model.DailySteps
import io.github.vitoosh.vitquest.core.model.StepRecord
import io.github.vitoosh.vitquest.core.model.StepSource
import io.github.vitoosh.vitquest.core.model.StepSourcePreference
import io.github.vitoosh.vitquest.core.steps.StepTrackingNotifications
import io.github.vitoosh.vitquest.feature.activity.StepDiagnosticsScreen
import io.github.vitoosh.vitquest.feature.activity.StepDiagnosticsUiState
import io.github.vitoosh.vitquest.testing.assertAccessible
import android.app.Notification
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en")
class StepDiagnosticsSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun string(id: Int, vararg args: Any): String = context.getString(id, *args)
    private val today = LocalDate.parse("2026-10-21")

    @Test
    fun showsRawCountsPerSourceAndWhichOneIsShown() {
        val state = StepDiagnosticsUiState(
            diagnostics = StepDiagnostics(
                collecting = true,
                counterAvailable = true,
                lastCounterValue = 265,
                lastCounterAt = Instant.parse("2026-10-21T05:00:00Z"),
                motionAvailable = true,
                motionStepsThisSession = 4_120,
            ),
            permissionGranted = true,
            checkpoint = null,
            todayRecords = listOf(StepRecord(today, StepSource.PHONE_SENSOR, 10, Instant.EPOCH), StepRecord(today, StepSource.MOTION, 4_120, Instant.EPOCH)),
            shown = DailySteps(today, 4_120, StepSource.MOTION),
            preference = StepSourcePreference.PHONE_SENSOR,
            status = null,
            zone = ZoneId.of("Asia/Jakarta"),
        )
        composeRule.setContent { VitQuestTheme { StepDiagnosticsScreen(state, onNavigateBack = {}) } }
        composeRule.assertAccessible()
        composeRule.onNodeWithText("265").assertIsDisplayed()
        composeRule.onNodeWithText("4,120 · " + string(R.string.step_source_motion)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun ongoingNotificationShowsTodaysStepsButNotOnTheLockScreen() {
        val notification = StepTrackingNotifications.build(context, steps = 4_120)
        assertEquals(string(R.string.step_tracking_notification_steps, "4,120"), notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        assertEquals(null, notification.publicVersion.extras.getCharSequence(Notification.EXTRA_TEXT))
        assertEquals(Notification.FLAG_ONGOING_EVENT, notification.flags and Notification.FLAG_ONGOING_EVENT)
    }
}
