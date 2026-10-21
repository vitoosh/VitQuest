package io.github.vitoosh.vitquest.core.data.local

import io.github.vitoosh.vitquest.core.model.ActivityLevel
import io.github.vitoosh.vitquest.core.model.DietaryPreference
import io.github.vitoosh.vitquest.core.model.Equipment
import io.github.vitoosh.vitquest.core.model.FitnessExperience
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.UserProfile
import io.github.vitoosh.vitquest.core.model.WorkoutLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileMappersTest {

    @Test
    fun profileRoundTripsThroughEntity() {
        val profile = UserProfile(
            age = 29,
            heightCm = 158.5,
            currentWeightKg = 61.2,
            fitnessExperience = FitnessExperience.BEGINNER,
            activityLevel = ActivityLevel.SEDENTARY,
            workoutLocation = WorkoutLocation.OFFICE,
            equipment = setOf(Equipment.DUMBBELLS, Equipment.CHAIR_OR_BENCH),
            dietaryPreferences = setOf(DietaryPreference.HALAL),
        )

        val entity = profile.toEntity(updatedAtEpochMs = 123L)

        assertEquals("CHAIR_OR_BENCH,DUMBBELLS", entity.equipment)
        assertEquals(123L, entity.updatedAtEpochMs)
        assertEquals(profile, entity.toModel())
    }

    @Test
    fun emptySetsAreStoredAsEmptyStrings() {
        val entity = UserProfile().toEntity(updatedAtEpochMs = 0L)
        assertEquals("", entity.equipment)
        assertEquals(UserProfile(), entity.toModel())
    }

    @Test
    fun unknownStoredValuesAreIgnored() {
        val entity = UserProfile().toEntity(0L).copy(
            activityLevel = "SUPER_ACTIVE",
            equipment = "DUMBBELLS,KETTLEBELL",
        )

        val model = entity.toModel()

        assertNull(model.activityLevel)
        assertEquals(setOf(Equipment.DUMBBELLS), model.equipment)
    }

    @Test
    fun goalsRoundTripThroughEntity() {
        val goals = Goals(targetWeightKg = null, dailyStepGoal = 6_000, dailyWaterTargetMl = 1_800, waterServingMl = 250)
        assertEquals(goals, goals.toEntity(updatedAtEpochMs = 1L).toModel())
    }
}
