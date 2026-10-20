package io.github.vitoosh.vitquest.core.data.seed

import android.content.Context
import androidx.annotation.StringRes
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.data.local.RewardEntity
import io.github.vitoosh.vitquest.core.model.RewardType

/**
 * Starter rewards, written in the app language at the time they are added.
 * They have no price: the user fills in what it really costs. No food rewards.
 */
object RewardSuggestions {

    private data class Suggestion(val key: String, @StringRes val name: Int, val type: RewardType)

    private val ALL = listOf(
        Suggestion("free_evening", R.string.reward_seed_free_evening, RewardType.EXPERIENCE),
        Suggestion("water_bottle", R.string.reward_seed_water_bottle, RewardType.PHYSICAL),
        Suggestion("movie", R.string.reward_seed_movie, RewardType.EXPERIENCE),
        Suggestion("book", R.string.reward_seed_book, RewardType.PHYSICAL),
        Suggestion("sports_shirt", R.string.reward_seed_sports_shirt, RewardType.PHYSICAL),
        Suggestion("exercise_mat", R.string.reward_seed_exercise_mat, RewardType.PHYSICAL),
        Suggestion("massage", R.string.reward_seed_massage, RewardType.EXPERIENCE),
        Suggestion("day_trip", R.string.reward_seed_day_trip, RewardType.EXPERIENCE),
        Suggestion("sports_shoes", R.string.reward_seed_sports_shoes, RewardType.PHYSICAL),
    )

    fun entities(context: Context, nowEpochMs: Long): List<RewardEntity> = ALL.map { suggestion ->
        RewardEntity(
            seedKey = suggestion.key,
            name = context.getString(suggestion.name),
            description = null,
            type = suggestion.type.name,
            priceRupiah = null,
            wishlist = false,
            createdAtEpochMs = nowEpochMs,
        )
    }
}
