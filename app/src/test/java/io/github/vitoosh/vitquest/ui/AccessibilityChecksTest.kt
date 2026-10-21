package io.github.vitoosh.vitquest.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.core.designsystem.theme.VitQuestTheme
import io.github.vitoosh.vitquest.core.locale.AppLanguage
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.ReminderSetting
import io.github.vitoosh.vitquest.core.model.ReminderType
import io.github.vitoosh.vitquest.core.model.ReminderWindow
import io.github.vitoosh.vitquest.core.model.ThemeMode
import io.github.vitoosh.vitquest.core.model.UserProfile
import io.github.vitoosh.vitquest.feature.home.HomeActions
import io.github.vitoosh.vitquest.feature.home.HomeScreen
import io.github.vitoosh.vitquest.feature.home.HomeUiState
import io.github.vitoosh.vitquest.feature.nutrition.photo.FoodPhotoActions
import io.github.vitoosh.vitquest.feature.nutrition.photo.FoodPhotoScreen
import io.github.vitoosh.vitquest.feature.nutrition.photo.FoodPhotoUiState
import io.github.vitoosh.vitquest.feature.nutrition.photo.PhotoError
import io.github.vitoosh.vitquest.feature.onboarding.OnboardingScreen
import io.github.vitoosh.vitquest.feature.onboarding.OnboardingStep
import io.github.vitoosh.vitquest.feature.onboarding.OnboardingUiState
import io.github.vitoosh.vitquest.feature.reminders.ReminderActions
import io.github.vitoosh.vitquest.feature.reminders.RemindersScreen
import io.github.vitoosh.vitquest.feature.reminders.RemindersUiState
import io.github.vitoosh.vitquest.feature.settings.SettingsScreen
import io.github.vitoosh.vitquest.testing.accessibilityProblems
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Every control on the main screens has a label for screen readers and a touch
 * area of at least 48 dp that does not collide with its neighbours, in Indonesian
 * (the longer language) at phone width.
 * The window is very tall so whole screens are laid out and checked at once.
 * Text contrast is covered by ColorContrastTest.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "in-w393dp-h4000dp")
class AccessibilityChecksTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.parse("2026-10-21")

    private fun check(dark: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent { VitQuestTheme(darkTheme = dark) { content() } }
        assertEquals(emptyList<String>(), composeRule.accessibilityProblems())
    }

    @Test
    fun checkerFindsUnlabelledAndTinyControls() {
        composeRule.setContent {
            Column {
                IconButton(onClick = {}) { Icon(Icons.Filled.Delete, contentDescription = null) }
                // Two small controls side by side: their widened tap areas collide.
                Row {
                    Box(Modifier.size(20.dp).semantics { contentDescription = "a" }.clickable {})
                    Box(Modifier.size(20.dp).semantics { contentDescription = "b" }.clickable {})
                }
            }
        }
        val problems = composeRule.accessibilityProblems()
        assertTrue(problems.toString(), problems.any { it.startsWith("No label") })
        assertTrue(problems.toString(), problems.any { it.startsWith("Touch areas overlap") && it.endsWith(": a / b") })
    }

    private val home: @Composable () -> Unit = {
        HomeScreen(
            uiState = HomeUiState.Ready(
                profile = UserProfile(currentWeightKg = 80.0),
                goals = Goals(targetWeightKg = 75.0, dailyStepGoal = 8_000, dailyWaterTargetMl = 2_400, waterServingMl = 250),
                today = today,
            ),
            actions = HomeActions(),
        )
    }

    private val settings: @Composable () -> Unit = {
        SettingsScreen(
            themeMode = ThemeMode.SYSTEM,
            language = AppLanguage.SYSTEM,
            onThemeModeSelected = {},
            onLanguageSelected = {},
            onEditProfile = {},
            onDeleteAllData = {},
            onNavigateBack = {},
        )
    }

    private val reminders: @Composable () -> Unit = {
        val settings = ReminderType.entries.associateWith { ReminderSetting(it) }
        RemindersScreen(RemindersUiState(settings, ReminderWindow(), settings.mapValues { null }), canNotify = false, actions = ReminderActions(), onNavigateBack = {})
    }

    @Test fun home() = check(content = home)

    @Test fun settings() = check(content = settings)

    @Test fun reminders() = check(content = reminders)

    @Test fun foodPhotoConsent() = check {
        FoodPhotoScreen(FoodPhotoUiState.Idle, consent = false, onNavigateBack = {}, actions = FoodPhotoActions())
    }

    @Test fun foodPhotoChoose() = check {
        FoodPhotoScreen(FoodPhotoUiState.Idle, consent = true, onNavigateBack = {}, actions = FoodPhotoActions())
    }

    @Test fun foodPhotoReview() = check {
        FoodPhotoScreen(FoodPhotoScreenSmokeTest.reviewState(), consent = true, onNavigateBack = {}, actions = FoodPhotoActions())
    }

    @Test fun foodPhotoFailed() = check {
        FoodPhotoScreen(FoodPhotoUiState.Failed(PhotoError.SERVER, canRetry = true), consent = true, onNavigateBack = {}, actions = FoodPhotoActions())
    }

    @Test fun onboardingWelcome() = check {
        OnboardingScreen(OnboardingUiState(), onUpdateForm = {}, onNext = {}, onSkip = {}, onBack = {})
    }

    @Test fun onboardingBody() = check {
        OnboardingScreen(OnboardingUiState(step = OnboardingStep.BODY), onUpdateForm = {}, onNext = {}, onSkip = {}, onBack = {})
    }
}
