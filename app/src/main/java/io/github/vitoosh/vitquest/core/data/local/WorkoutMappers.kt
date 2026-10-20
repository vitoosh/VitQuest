package io.github.vitoosh.vitquest.core.data.local

import io.github.vitoosh.vitquest.core.model.Bilingual
import io.github.vitoosh.vitquest.core.model.CustomHabit
import io.github.vitoosh.vitquest.core.model.StepStatus
import io.github.vitoosh.vitquest.core.model.WorkoutSession
import io.github.vitoosh.vitquest.core.model.WorkoutSessionExercise
import io.github.vitoosh.vitquest.core.model.WorkoutSource
import java.time.Instant
import java.time.LocalDate

fun WorkoutSessionWithExercises.toModel(): WorkoutSession = WorkoutSession(
    id = session.id,
    date = LocalDate.parse(session.date),
    startedAt = Instant.ofEpochMilli(session.startedAtEpochMs),
    endedAt = Instant.ofEpochMilli(session.endedAtEpochMs),
    source = session.source.toEnumOrNull<WorkoutSource>() ?: WorkoutSource.MANUAL,
    focus = session.focus?.toEnumOrNull(),
    level = session.level?.toEnumOrNull(),
    location = session.location?.toEnumOrNull(),
    intensity = session.intensity?.toEnumOrNull(),
    plannedSeconds = session.plannedSeconds,
    activeSeconds = session.activeSeconds,
    exercisesCompleted = session.exercisesCompleted,
    exercisesTotal = session.exercisesTotal,
    completed = session.completed,
    estimatedKcal = session.estimatedKcal,
    note = session.note,
    exercises = exercises.sortedBy { it.position }.map {
        WorkoutSessionExercise(
            exerciseKey = it.exerciseKey,
            name = Bilingual(it.nameId, it.nameEn),
            plannedSeconds = it.plannedSeconds,
            workedSeconds = it.workedSeconds,
            status = it.status.toEnumOrNull<StepStatus>() ?: StepStatus.NOT_REACHED,
        )
    },
)

fun HabitWithCheckIns.toModel(): CustomHabit = CustomHabit(
    id = habit.id,
    name = habit.name,
    targetDaysPerWeek = habit.targetDaysPerWeek,
    checkIns = checkIns.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet(),
)
