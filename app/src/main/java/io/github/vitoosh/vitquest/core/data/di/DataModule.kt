package io.github.vitoosh.vitquest.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.vitoosh.vitquest.core.data.backup.BackupRepository
import io.github.vitoosh.vitquest.core.data.backup.LocalBackupRepository
import io.github.vitoosh.vitquest.core.data.health.AndroidHealthConnectStepsSource
import io.github.vitoosh.vitquest.core.data.health.HealthConnectStepsSource
import io.github.vitoosh.vitquest.core.data.local.BodyDao
import io.github.vitoosh.vitquest.core.data.local.DatabaseMigrations
import io.github.vitoosh.vitquest.core.data.local.FoodDao
import io.github.vitoosh.vitquest.core.data.local.HabitDao
import io.github.vitoosh.vitquest.core.data.local.ProfileDao
import io.github.vitoosh.vitquest.core.data.local.RecipeDao
import io.github.vitoosh.vitquest.core.data.local.ReminderDao
import io.github.vitoosh.vitquest.core.data.local.StepsDao
import io.github.vitoosh.vitquest.core.data.local.VitQuestDatabase
import io.github.vitoosh.vitquest.core.data.local.WorkoutDao
import io.github.vitoosh.vitquest.core.data.repository.BodyRepository
import io.github.vitoosh.vitquest.core.data.repository.DataStoreUserPreferencesRepository
import io.github.vitoosh.vitquest.core.data.repository.HabitRepository
import io.github.vitoosh.vitquest.core.data.repository.LocalUserDataRepository
import io.github.vitoosh.vitquest.core.data.repository.NutritionRepository
import io.github.vitoosh.vitquest.core.data.repository.OfflineBodyRepository
import io.github.vitoosh.vitquest.core.data.repository.OfflineFirstStepsRepository
import io.github.vitoosh.vitquest.core.data.repository.OfflineHabitRepository
import io.github.vitoosh.vitquest.core.data.repository.OfflineNutritionRepository
import io.github.vitoosh.vitquest.core.data.repository.OfflineProfileRepository
import io.github.vitoosh.vitquest.core.data.repository.OfflineRecipeRepository
import io.github.vitoosh.vitquest.core.data.repository.OfflineReminderRepository
import io.github.vitoosh.vitquest.core.data.repository.OfflineWorkoutRepository
import io.github.vitoosh.vitquest.core.data.repository.ProfileRepository
import io.github.vitoosh.vitquest.core.data.repository.RecipeRepository
import io.github.vitoosh.vitquest.core.data.repository.ReminderRepository
import io.github.vitoosh.vitquest.core.data.repository.ReminderScheduler
import io.github.vitoosh.vitquest.core.data.repository.StepsRepository
import io.github.vitoosh.vitquest.core.data.repository.UserDataRepository
import io.github.vitoosh.vitquest.core.data.repository.UserPreferencesRepository
import io.github.vitoosh.vitquest.core.data.repository.WorkoutRepository
import io.github.vitoosh.vitquest.core.data.sensor.AndroidStepSensorSource
import io.github.vitoosh.vitquest.core.data.sensor.StepSensorSource
import io.github.vitoosh.vitquest.core.reminders.WorkManagerReminderScheduler
import io.github.vitoosh.vitquest.core.time.SystemTimeSource
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataProvidersModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VitQuestDatabase =
        Room.databaseBuilder(context, VitQuestDatabase::class.java, VitQuestDatabase.NAME)
            .addMigrations(*DatabaseMigrations.ALL)
            .build()

    @Provides
    fun provideProfileDao(database: VitQuestDatabase): ProfileDao = database.profileDao()

    @Provides
    fun provideStepsDao(database: VitQuestDatabase): StepsDao = database.stepsDao()

    @Provides
    fun provideFoodDao(database: VitQuestDatabase): FoodDao = database.foodDao()

    @Provides
    fun provideRecipeDao(database: VitQuestDatabase): RecipeDao = database.recipeDao()

    @Provides
    fun provideBodyDao(database: VitQuestDatabase): BodyDao = database.bodyDao()

    @Provides
    fun provideWorkoutDao(database: VitQuestDatabase): WorkoutDao = database.workoutDao()

    @Provides
    fun provideHabitDao(database: VitQuestDatabase): HabitDao = database.habitDao()


    @Provides
    fun provideReminderDao(database: VitQuestDatabase): ReminderDao = database.reminderDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("user_preferences") },
        )

    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {

    @Binds
    abstract fun bindProfileRepository(impl: OfflineProfileRepository): ProfileRepository

    @Binds
    abstract fun bindUserPreferencesRepository(
        impl: DataStoreUserPreferencesRepository,
    ): UserPreferencesRepository

    @Binds
    abstract fun bindUserDataRepository(impl: LocalUserDataRepository): UserDataRepository

    @Binds
    abstract fun bindBackupRepository(impl: LocalBackupRepository): BackupRepository

    @Binds
    abstract fun bindStepsRepository(impl: OfflineFirstStepsRepository): StepsRepository

    @Binds
    abstract fun bindHealthConnectStepsSource(impl: AndroidHealthConnectStepsSource): HealthConnectStepsSource

    @Binds
    abstract fun bindStepSensorSource(impl: AndroidStepSensorSource): StepSensorSource

    @Binds
    abstract fun bindTimeSource(impl: SystemTimeSource): TimeSource

    @Binds
    abstract fun bindNutritionRepository(impl: OfflineNutritionRepository): NutritionRepository

    @Binds
    abstract fun bindRecipeRepository(impl: OfflineRecipeRepository): RecipeRepository

    @Binds
    abstract fun bindBodyRepository(impl: OfflineBodyRepository): BodyRepository

    @Binds
    abstract fun bindWorkoutRepository(impl: OfflineWorkoutRepository): WorkoutRepository

    @Binds
    abstract fun bindHabitRepository(impl: OfflineHabitRepository): HabitRepository


    @Binds
    abstract fun bindReminderRepository(impl: OfflineReminderRepository): ReminderRepository

    @Binds
    abstract fun bindReminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler
}
