package io.github.vitoosh.vitquest.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.theme.VitQuestTheme
import io.github.vitoosh.vitquest.core.domain.rewards.Leveling
import io.github.vitoosh.vitquest.core.model.ReminderSetting
import io.github.vitoosh.vitquest.core.model.ReminderType
import io.github.vitoosh.vitquest.core.model.ReminderWindow
import io.github.vitoosh.vitquest.core.model.Reward
import io.github.vitoosh.vitquest.core.model.RewardCurrency
import io.github.vitoosh.vitquest.core.model.RewardReason
import io.github.vitoosh.vitquest.core.model.RewardTransaction
import io.github.vitoosh.vitquest.core.model.RewardType
import io.github.vitoosh.vitquest.feature.reminders.ReminderActions
import io.github.vitoosh.vitquest.feature.reminders.RemindersScreen
import io.github.vitoosh.vitquest.feature.reminders.RemindersUiState
import io.github.vitoosh.vitquest.feature.rewards.RewardActions
import io.github.vitoosh.vitquest.feature.rewards.RewardsScreen
import io.github.vitoosh.vitquest.feature.rewards.RewardsUiState
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en")
class RewardScreensSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.parse("2026-10-21")

    private fun string(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private val bottle = Reward(1, "Water bottle", null, RewardType.PHYSICAL, creditCost = 80, priceRupiah = 85_000, wishlist = true)
    private val shoes = Reward(2, "Sports shoes", null, RewardType.PHYSICAL, creditCost = 500, priceRupiah = null, wishlist = false)

    private fun state(credits: Int = 120) = RewardsUiState(
        today = today,
        level = Leveling.progress(260),
        credits = credits,
        transactions = listOf(RewardTransaction(1, RewardCurrency.XP, 50, RewardReason.WORKOUT, today, Instant.EPOCH, isReversal = false)),
        rewards = listOf(bottle, shoes),
        redemptions = emptyList(),
        budgetEntries = emptyList(),
        monthlyBudget = null,
    )

    @Test
    fun overviewShowsLevelCreditsAndEarningRules() {
        composeRule.setContent { VitQuestTheme { RewardsScreen(state(), RewardActions(), onOpenSettings = {}) } }
        composeRule.onNodeWithText(string(R.string.rewards_level, "3")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.value_credits, "120")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.rewards_credits_not_money)).assertIsDisplayed()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(string(R.string.reward_rule_workout_limit)))
        composeRule.onNodeWithText(string(R.string.reward_rule_workout_limit)).assertIsDisplayed()
    }

    @Test
    fun catalogOnlyRedeemsAffordableRewardsAfterConfirmation() {
        val redeemed = mutableListOf<Pair<String, Reward>>()
        composeRule.setContent {
            VitQuestTheme { RewardsScreen(state(), RewardActions(redeem = { id, reward -> redeemed += id to reward }), onOpenSettings = {}) }
        }
        composeRule.onNodeWithText(string(R.string.rewards_tab_catalog)).performClick()
        val buttons = composeRule.onAllNodesWithText(string(R.string.rewards_redeem))
        // Wishlist bottle (80 RC) is affordable; the shoes (500 RC) are not.
        buttons[0].assertIsEnabled()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Sports shoes"))
        composeRule.onAllNodesWithText(string(R.string.rewards_redeem))[1].assertIsNotEnabled()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Water bottle"))
        composeRule.onAllNodesWithText(string(R.string.rewards_redeem))[0].performClick()
        composeRule.onNodeWithText(string(R.string.rewards_redeem_title, "Water bottle")).assertIsDisplayed()
        assertTrue(redeemed.isEmpty())
        composeRule.onAllNodesWithText(string(R.string.rewards_redeem)).let { it[it.fetchSemanticsNodes().size - 1] }.performClick()
        assertEquals(listOf(bottle), redeemed.map { it.second })
    }

    @Test
    fun budgetIsClearlySeparateFromCredits() {
        composeRule.setContent { VitQuestTheme { RewardsScreen(state(), RewardActions(), onOpenSettings = {}) } }
        composeRule.onNodeWithText(string(R.string.rewards_tab_budget)).performClick()
        composeRule.onNodeWithText(string(R.string.budget_separate_note)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.budget_not_set)).assertIsDisplayed()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(string(R.string.budget_add_entry)))
        composeRule.onNodeWithText(string(R.string.budget_add_entry)).performClick()
        composeRule.onNodeWithText(string(R.string.budget_kind_spending)).assertIsDisplayed()
    }

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
