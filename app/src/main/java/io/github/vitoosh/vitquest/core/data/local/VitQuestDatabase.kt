package io.github.vitoosh.vitquest.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        GoalsEntity::class,
        DailyStepsEntity::class,
        StepSensorStateEntity::class,
        FoodEntity::class,
        FoodServingEntity::class,
        FoodLogEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        MealPlanEntity::class,
        ShoppingItemEntity::class,
        WeightEntryEntity::class,
        BodyMeasurementEntity::class,
        WaterLogEntity::class,
    ],
    version = VitQuestDatabase.VERSION,
    exportSchema = true,
)
abstract class VitQuestDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    abstract fun stepsDao(): StepsDao

    abstract fun foodDao(): FoodDao

    abstract fun recipeDao(): RecipeDao

    abstract fun bodyDao(): BodyDao

    companion object {
        const val NAME = "vitquest.db"
        const val VERSION = 3
    }
}
