package io.github.vitoosh.vitquest.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.theme.VitQuestTheme
import io.github.vitoosh.vitquest.core.locale.AppLanguage
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.ThemeMode
import io.github.vitoosh.vitquest.core.model.UserProfile
import io.github.vitoosh.vitquest.core.domain.weight.WeightStats
import io.github.vitoosh.vitquest.core.model.WeightEntry
import io.github.vitoosh.vitquest.feature.home.HomeActions
import io.github.vitoosh.vitquest.feature.home.HomeScreen
import java.time.LocalDate
import io.github.vitoosh.vitquest.feature.home.HomeUiState
import io.github.vitoosh.vitquest.feature.onboarding.OnboardingScreen
import io.github.vitoosh.vitquest.feature.onboarding.OnboardingStep
import io.github.vitoosh.vitquest.feature.onboarding.OnboardingUiState
import io.github.vitoosh.vitquest.feature.settings.SettingsScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Renders each screen on the JVM to catch crashes and missing content (English resources). */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en")
class ScreenSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    @Test
    fun welcomeStepStartsOnboarding() {
        var nextClicks = 0
        composeRule.setContent {
            VitQuestTheme {
                OnboardingScreen(OnboardingUiState(), onUpdateForm = {}, onNext = { nextClicks++ }, onSkip = {}, onBack = {})
            }
        }

        composeRule.onNodeWithText(string(R.string.onboarding_welcome_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.onboarding_start)).performClick()
        assertEquals(1, nextClicks)
    }

    @Test
    fun everyOnboardingStepRenders() {
        var step by mutableStateOf(OnboardingStep.BODY)
        composeRule.setContent {
            VitQuestTheme {
                OnboardingScreen(OnboardingUiState(step = step), onUpdateForm = {}, onNext = {}, onSkip = {}, onBack = {})
            }
        }
        OnboardingStep.entries.drop(1).forEach { current ->
            step = current
            composeRule.waitForIdle()
            composeRule.onNodeWithText(string(R.string.onboarding_step_counter, current.ordinal, 4)).assertIsDisplayed()
        }
    }

    @Test
    fun homeShowsTargetsAndRewardsWithoutInventedNumbers() {
        composeRule.setContent {
            VitQuestTheme(darkTheme = true) {
                HomeScreen(
                    uiState = HomeUiState.Ready(
                        profile = UserProfile(currentWeightKg = 80.0),
                        goals = Goals(targetWeightKg = 75.0, dailyStepGoal = 8_000, dailyWaterTargetMl = 2_400, waterServingMl = 250),
                        weight = WeightStats.summarize(
                            listOf(WeightEntry(1, LocalDate.parse("2026-10-19"), 80.0, null)),
                            75.0,
                            LocalDate.parse("2026-10-19"),
                        ),
                    ),
                    actions = HomeActions(),
                )
            }
        }

        // No step source has data, so Home says so instead of showing 0.
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(string(R.string.home_steps_no_data)))
        composeRule.onNodeWithText(string(R.string.home_steps_no_data)).assertIsDisplayed()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(string(R.string.home_weight_to_lose, "5")))
        composeRule.onNodeWithText(string(R.string.home_weight_to_lose, "5")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.data_source_manual)).assertIsDisplayed()
    }

    @Test
    fun homeAsksToCompleteProfileWhenWeightMissing() {
        composeRule.setContent {
            VitQuestTheme {
                HomeScreen(
                    uiState = HomeUiState.Ready(profile = null, goals = Goals(dailyStepGoal = 7_000, dailyWaterTargetMl = 2_000, waterServingMl = 250)),
                    actions = HomeActions(),
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.home_complete_profile_title)).assertIsDisplayed()
    }

    @Test
    fun deletingAllDataRequiresConfirmation() {
        var deleted = false
        var theme: ThemeMode? = null
        composeRule.setContent {
            VitQuestTheme {
                SettingsScreen(
                    themeMode = ThemeMode.SYSTEM,
                    language = AppLanguage.SYSTEM,
                    onThemeModeSelected = { theme = it },
                    onLanguageSelected = {},
                    onEditProfile = {},
                    onDeleteAllData = { deleted = true },
                    onNavigateBack = {},
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.theme_dark)).performScrollTo().performClick()
        assertEquals(ThemeMode.DARK, theme)

        composeRule.onNodeWithText(string(R.string.settings_delete_all_data)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.settings_delete_dialog_title)).assertIsDisplayed()
        assertFalse(deleted)
        composeRule.onNodeWithText(string(R.string.settings_delete_dialog_confirm)).performClick()
        assertTrue(deleted)
    }

    @Test
    fun restoringABackupAsksFirstAndExportIsOneTap() {
        var exported = 0
        var pickerOpened = 0
        composeRule.setContent {
            VitQuestTheme {
                SettingsScreen(
                    themeMode = ThemeMode.SYSTEM,
                    language = AppLanguage.SYSTEM,
                    onThemeModeSelected = {},
                    onLanguageSelected = {},
                    onEditProfile = {},
                    onDeleteAllData = {},
                    onNavigateBack = {},
                    onExportBackup = { exported++ },
                    onRestoreBackup = { pickerOpened++ },
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.settings_backup_export)).performScrollTo().performClick()
        assertEquals(1, exported)
        composeRule.onNodeWithText(string(R.string.settings_backup_restore)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.settings_restore_dialog_message)).assertIsDisplayed()
        assertEquals(0, pickerOpened)
        composeRule.onNodeWithText(string(R.string.settings_restore_dialog_confirm)).performClick()
        assertEquals(1, pickerOpened)
    }
}
