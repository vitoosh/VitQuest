package io.github.vitoosh.vitquest.core.domain.workout

import io.github.vitoosh.vitquest.core.model.Muscle

/** Parts of the stick figure that can be coloured to show the working muscles. */
enum class FigureSegment { TORSO, UPPER_ARMS, FOREARMS, HIPS, THIGHS, SHINS }

object MuscleHighlight {
    fun segments(muscles: Collection<Muscle>): Set<FigureSegment> = muscles.flatMap { muscle ->
        when (muscle) {
            Muscle.CHEST, Muscle.UPPER_BACK, Muscle.LOWER_BACK, Muscle.CORE, Muscle.HEART_LUNGS -> listOf(FigureSegment.TORSO)
            Muscle.SHOULDERS, Muscle.TRICEPS -> listOf(FigureSegment.UPPER_ARMS)
            Muscle.BICEPS -> listOf(FigureSegment.UPPER_ARMS, FigureSegment.FOREARMS)
            Muscle.GLUTES -> listOf(FigureSegment.HIPS)
            Muscle.QUADS, Muscle.HAMSTRINGS, Muscle.HIP_FLEXORS -> listOf(FigureSegment.THIGHS)
            Muscle.CALVES -> listOf(FigureSegment.SHINS)
        }
    }.toSet()
}
