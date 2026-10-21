package io.github.vitoosh.vitquest.core.data.repository

import io.github.vitoosh.vitquest.core.data.local.WorkoutDao
import io.github.vitoosh.vitquest.core.data.local.WorkoutSessionEntity
import io.github.vitoosh.vitquest.core.data.local.WorkoutSessionExerciseEntity
import io.github.vitoosh.vitquest.core.data.local.toModel
import io.github.vitoosh.vitquest.core.domain.workout.PlayerState
import io.github.vitoosh.vitquest.core.domain.workout.WorkoutEnergy
import io.github.vitoosh.vitquest.core.model.WorkoutIntensity
import io.github.vitoosh.vitquest.core.model.WorkoutPlan
import io.github.vitoosh.vitquest.core.model.WorkoutSession
import io.github.vitoosh.vitquest.core.model.WorkoutSource
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A workout done without the player, such as a class or a bike ride. */
data class ManualWorkoutInput(
    val date: LocalDate,
    val minutes: Int,
    val intensity: WorkoutIntensity,
    val note: String?,
)

interface WorkoutRepository {
    fun sessions(): Flow<List<WorkoutSession>>
    fun sessions(from: LocalDate, to: LocalDate): Flow<List<WorkoutSession>>

    /** Stores a player session; [weightKg] (if known) gives the energy estimate. */
    suspend fun savePlayerSession(plan: WorkoutPlan, state: PlayerState, startedAt: Instant, weightKg: Double?): Long
    suspend fun logManual(input: ManualWorkoutInput, weightKg: Double?): Long
    suspend fun deleteSession(id: Long)
}

@Singleton
class OfflineWorkoutRepository @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val timeSource: TimeSource,
) : WorkoutRepository {

    override fun sessions(): Flow<List<WorkoutSession>> = workoutDao.observeSessions().map { rows -> rows.map { it.toModel() } }

    override fun sessions(from: LocalDate, to: LocalDate): Flow<List<WorkoutSession>> =
        workoutDao.observeSessions(from.toString(), to.toString()).map { rows -> rows.map { it.toModel() } }

    override suspend fun savePlayerSession(plan: WorkoutPlan, state: PlayerState, startedAt: Instant, weightKg: Double?): Long {
        val summary = WorkoutEnergy.summarize(plan, state, weightKg)
        val request = plan.request
        val session = WorkoutSessionEntity(
            date = startedAt.atZone(timeSource.zone()).toLocalDate().toString(),
            startedAtEpochMs = startedAt.toEpochMilli(),
            endedAtEpochMs = timeSource.now().toEpochMilli(),
            source = WorkoutSource.PLAYER.name,
            focus = request.focus.name,
            level = request.level.name,
            location = request.location.name,
            intensity = null,
            plannedSeconds = plan.totalSeconds,
            activeSeconds = summary.activeSeconds,
            exercisesCompleted = summary.completedSteps,
            exercisesTotal = summary.totalSteps,
            completed = !state.endedEarly,
            estimatedKcal = summary.estimatedKcal,
            note = null,
        )
        val exercises = plan.steps.mapIndexed { index, step ->
            WorkoutSessionExerciseEntity(
                sessionId = 0,
                position = index,
                exerciseKey = step.exercise.key,
                nameId = step.exercise.name.id,
                nameEn = step.exercise.name.en,
                plannedSeconds = step.workSeconds,
                workedSeconds = (state.workedMs[index] / 1000L).toInt(),
                status = state.statuses[index].name,
            )
        }
        return workoutDao.insertSessionWithExercises(session, exercises)
    }

    override suspend fun logManual(input: ManualWorkoutInput, weightKg: Double?): Long {
        val now = timeSource.now()
        val seconds = input.minutes * 60
        val session = WorkoutSessionEntity(
            date = input.date.toString(),
            startedAtEpochMs = now.toEpochMilli(),
            endedAtEpochMs = now.toEpochMilli(),
            source = WorkoutSource.MANUAL.name,
            focus = null,
            level = null,
            location = null,
            intensity = input.intensity.name,
            plannedSeconds = seconds,
            activeSeconds = seconds,
            exercisesCompleted = 0,
            exercisesTotal = 0,
            completed = true,
            estimatedKcal = weightKg?.let { WorkoutEnergy.kcal(WorkoutEnergy.intensityMet(input.intensity), it, seconds) },
            note = input.note?.trim()?.takeIf { it.isNotEmpty() },
        )
        return workoutDao.insertSessionWithExercises(session, emptyList())
    }

    override suspend fun deleteSession(id: Long) = workoutDao.deleteSession(id)
}
