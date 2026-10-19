package io.github.vitoosh.vitquest.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        GoalsEntity::class,
        DailyStepsEntity::class,
        StepSensorStateEntity::class,
    ],
    version = VitQuestDatabase.VERSION,
    exportSchema = true,
)
abstract class VitQuestDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    abstract fun stepsDao(): StepsDao

    companion object {
        const val NAME = "vitquest.db"
        const val VERSION = 2
    }
}
