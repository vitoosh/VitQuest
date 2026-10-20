package io.github.vitoosh.vitquest.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.core.designsystem.theme.VitQuestTheme
import io.github.vitoosh.vitquest.core.domain.rewards.Leveling
import io.github.vitoosh.vitquest.core.locale.AppLanguage
import io.github.vitoosh.vitquest.core.model.Reward
import io.github.vitoosh.vitquest.core.model.RewardType
import io.github.vitoosh.vitquest.core.model.ThemeMode
import io.github.vitoosh.vitquest.feature.rewards.RewardActions
import io.github.vitoosh.vitquest.feature.rewards.RewardsScreen
import io.github.vitoosh.vitquest.feature.rewards.RewardsUiState
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

    private val rewards: @Composable () -> Unit = {
        RewardsScreen(
            RewardsUiState(
                today = LocalDate.of(2026, 10, 21),
                level = Leveling.progress(260),
                transactions = emptyList(),
                rewards = listOf(Reward(1, "Botol minum", null, RewardType.PHYSICAL, 85_000, wishlist = true)),
                history = emptyList(),
                budgetEntries = emptyList(),
                monthlyBudget = null,
            ),
            RewardActions(),
            onOpenSettings = {},
        )
    }

    @Test fun settingsLight() = check(dark = false, content = settings)

    @Test fun settingsDark() = check(dark = true, content = settings)

    @Test fun rewardsLight() = check(dark = false, content = rewards)

    @Test fun rewardsDark() = check(dark = true, content = rewards)
}
