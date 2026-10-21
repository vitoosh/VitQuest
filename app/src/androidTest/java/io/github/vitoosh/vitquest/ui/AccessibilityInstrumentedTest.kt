package io.github.vitoosh.vitquest.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.core.designsystem.theme.VitQuestTheme
import io.github.vitoosh.vitquest.core.domain.habits.Achievements
import io.github.vitoosh.vitquest.core.domain.habits.ProgressStats
import io.github.vitoosh.vitquest.core.locale.AppLanguage
import io.github.vitoosh.vitquest.core.model.ThemeMode
import io.github.vitoosh.vitquest.feature.progress.ProgressScreen
import io.github.vitoosh.vitquest.feature.progress.ProgressUiState
import io.github.vitoosh.vitquest.feature.settings.SettingsScreen
import java.time.LocalDate
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Google's Accessibility Test Framework on a real device: labels, touch targets and
 * measured text contrast. It cannot evaluate Compose under Robolectric, which is why
 * the unit tests use their own semantics checks. Run with `./gradlew connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun check(dark: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent { VitQuestTheme(darkTheme = dark) { content() } }
        composeRule.onRoot().tryPerformAccessibilityChecks()
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

    private val progress: @Composable () -> Unit = {
        val today = LocalDate.of(2026, 10, 21)
        ProgressScreen(
            ProgressUiState(
                today = today,
                weeks = ProgressStats.weeks(today, 8, emptyMap(), null),
                weights = emptyList(),
                targetWeightKg = null,
                achievements = Achievements.evaluate(emptyList(), emptyList(), null, emptyList()),
            ),
            onNavigateBack = {},
            onOpenWeight = {},
        )
    }

    @Test fun settingsLight() = check(dark = false, content = settings)

    @Test fun settingsDark() = check(dark = true, content = settings)

    @Test fun progressLight() = check(dark = false, content = progress)

    @Test fun progressDark() = check(dark = true, content = progress)
}
