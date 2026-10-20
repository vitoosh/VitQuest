package io.github.vitoosh.vitquest.core.data.seed

import android.content.Context
import androidx.annotation.StringRes
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.data.local.RewardEntity
import io.github.vitoosh.vitquest.core.model.RewardType

/**
 * Starter rewards, written in the app language at the time they are added.
 * Costs assume roughly 10 credits on an active day (see RewardRule), so a
 * small treat takes days and a bigger one a few weeks. No food rewards.
 */
object RewardSuggestions {

    private data class Suggestion(val key: String, @StringRes val name: Int, val type: RewardType, val cost: Int)

    private val ALL = listOf(
        Suggestion("free_evening", R.string.reward_seed_free_evening, RewardType.EXPERIENCE, 40),
        Suggestion("water_bottle", R.string.reward_seed_water_bottle, RewardType.PHYSICAL, 80),
        Suggestion("movie", R.string.reward_seed_movie, RewardType.EXPERIENCE, 100),
        Suggestion("book", R.string.reward_seed_book, RewardType.PHYSICAL, 120),
        Suggestion("sports_shirt", R.string.reward_seed_sports_shirt, RewardType.PHYSICAL, 150),
        Suggestion("exercise_mat", R.string.reward_seed_exercise_mat, RewardType.PHYSICAL, 200),
        Suggestion("massage", R.string.reward_seed_massage, RewardType.EXPERIENCE, 250),
        Suggestion("day_trip", R.string.reward_seed_day_trip, RewardType.EXPERIENCE, 400),
        Suggestion("sports_shoes", R.string.reward_seed_sports_shoes, RewardType.PHYSICAL, 500),
    )

    fun entities(context: Context, nowEpochMs: Long): List<RewardEntity> = ALL.map { suggestion ->
        RewardEntity(
            seedKey = suggestion.key,
            name = context.getString(suggestion.name),
            description = null,
            type = suggestion.type.name,
            creditCost = suggestion.cost,
            priceRupiah = null,
            wishlist = false,
            createdAtEpochMs = nowEpochMs,
        )
    }
}
