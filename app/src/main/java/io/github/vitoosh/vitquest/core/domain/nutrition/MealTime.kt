package io.github.vitoosh.vitquest.core.domain.nutrition

import io.github.vitoosh.vitquest.core.model.MealType

/** Suggests which meal a quick "log food" action is for, based on the local hour. */
fun mealForHour(hour: Int): MealType = when (hour) {
    in 4..10 -> MealType.BREAKFAST
    in 11..14 -> MealType.LUNCH
    in 17..21 -> MealType.DINNER
    else -> MealType.SNACK
}
