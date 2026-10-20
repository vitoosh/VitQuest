package io.github.vitoosh.vitquest.feature.rewards

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.domain.rewards.RewardRule
import io.github.vitoosh.vitquest.core.model.RewardReason
import io.github.vitoosh.vitquest.core.model.RewardType
import io.github.vitoosh.vitquest.core.ui.formatInteger
import io.github.vitoosh.vitquest.core.ui.currentLocale
import java.text.NumberFormat

@get:StringRes
val RewardReason.labelRes: Int
    get() = when (this) {
        RewardReason.WORKOUT -> R.string.reward_reason_workout
        RewardReason.STEP_GOAL -> R.string.reward_reason_step_goal
        RewardReason.WATER_GOAL -> R.string.reward_reason_water_goal
        RewardReason.FOOD_LOG -> R.string.reward_reason_food_log
        RewardReason.WEIGHT_LOG -> R.string.reward_reason_weight_log
        RewardReason.HABIT_CHECK_IN -> R.string.reward_reason_habit_check_in
        RewardReason.ACHIEVEMENT -> R.string.reward_reason_achievement
    }

/** When and how often a rule pays out. */
@get:StringRes
val RewardRule.limitRes: Int
    get() = when (this) {
        RewardRule.WORKOUT -> R.string.reward_rule_workout_limit
        RewardRule.STEP_GOAL -> R.string.reward_rule_per_day
        RewardRule.WATER_GOAL -> R.string.reward_rule_per_day
        RewardRule.FOOD_LOG -> R.string.reward_rule_per_day
        RewardRule.WEIGHT_LOG -> R.string.reward_rule_weight_limit
        RewardRule.HABIT_CHECK_IN -> R.string.reward_rule_habit_limit
        RewardRule.ACHIEVEMENT -> R.string.reward_rule_achievement_limit
    }

@get:StringRes
val RewardType.labelRes: Int
    get() = when (this) {
        RewardType.PHYSICAL -> R.string.reward_type_physical
        RewardType.EXPERIENCE -> R.string.reward_type_experience
    }

@Composable
@ReadOnlyComposable
fun xpText(amount: Int): String = stringResource(R.string.value_xp, formatInteger(amount))

/** Real money, always with the currency so it is never confused with XP. */
@Composable
@ReadOnlyComposable
fun rupiahText(amount: Long): String =
    stringResource(R.string.value_rupiah, NumberFormat.getIntegerInstance(currentLocale()).format(amount))
