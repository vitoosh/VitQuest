package io.github.vitoosh.vitquest.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.theme.VitQuestTheme
import io.github.vitoosh.vitquest.core.model.ReminderSetting
import io.github.vitoosh.vitquest.core.model.ReminderType
import io.github.vitoosh.vitquest.core.model.ReminderWindow
import io.github.vitoosh.vitquest.feature.reminders.ReminderActions
import io.github.vitoosh.vitquest.feature.reminders.RemindersScreen
import io.github.vitoosh.vitquest.feature.reminders.RemindersUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en")
class ReminderScreenSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    @Test
    fun remindersExplainMissingPermissionAndAskForATime() {
        val saved = mutableListOf<ReminderSetting>()
        var asked = false
        val settings = ReminderType.entries.associateWith { ReminderSetting(it) }
        composeRule.setContent {
            VitQuestTheme {
                RemindersScreen(
                    uiState = RemindersUiState(settings, ReminderWindow(), settings.mapValues { null }),
                    canNotify = false,
                    actions = ReminderActions(save = { saved += it }, requestPermission = { asked = true }),
                    onNavigateBack = {},
                )
            }
        }
        composeRule.onNodeWithText(string(R.string.reminders_permission_button)).performClick()
        assertTrue(asked)
        // Water can be switched on; it then asks for active hours.
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription(string(R.string.reminder_water)))
        composeRule.onNodeWithContentDescription(string(R.string.reminder_water)).performClick()
        assertEquals(ReminderType.WATER, saved.single().type)
        // A workout reminder has no time yet, so switching it on opens the time picker instead of saving.
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription(string(R.string.reminder_workout)))
        composeRule.onNodeWithContentDescription(string(R.string.reminder_workout)).performClick()
        composeRule.onNodeWithText(string(R.string.reminders_time)).assertIsDisplayed()
        assertEquals(1, saved.size)
    }
}
