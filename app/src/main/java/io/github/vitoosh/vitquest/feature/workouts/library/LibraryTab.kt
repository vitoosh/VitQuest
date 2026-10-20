package io.github.vitoosh.vitquest.feature.workouts.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.workout.ExerciseCatalog
import io.github.vitoosh.vitquest.core.model.Exercise
import io.github.vitoosh.vitquest.core.model.ExerciseKind
import io.github.vitoosh.vitquest.core.model.FitnessExperience
import io.github.vitoosh.vitquest.core.ui.isIndonesian
import io.github.vitoosh.vitquest.core.ui.labelRes
import io.github.vitoosh.vitquest.feature.workouts.ExerciseRow
import io.github.vitoosh.vitquest.feature.workouts.exerciseSummary
import io.github.vitoosh.vitquest.feature.workouts.labelRes

/** Browsable list of every built-in exercise, filterable by type and level. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryTab(onOpenExercise: (String) -> Unit, exercises: List<Exercise> = ExerciseCatalog.all) {
    var query by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf<ExerciseKind?>(null) }
    var level by rememberSaveable { mutableStateOf<FitnessExperience?>(null) }
    val indonesian = isIndonesian()
    val filtered = exercises.filter { exercise ->
        val name = if (indonesian) exercise.name.id else exercise.name.en
        (kind == null || exercise.kind == kind) &&
            (level == null || exercise.level == level) &&
            (query.isBlank() || name.contains(query.trim(), ignoreCase = true))
    }
    CenteredContent {
        LazyColumn(
            contentPadding = PaddingValues(VqSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VqSpacing.xs),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.library_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                    ExerciseKind.entries.forEach { option ->
                        FilterChip(
                            selected = kind == option,
                            onClick = { kind = if (kind == option) null else option },
                            label = { Text(stringResource(option.labelRes)) },
                        )
                    }
                    FitnessExperience.entries.forEach { option ->
                        FilterChip(
                            selected = level == option,
                            onClick = { level = if (level == option) null else option },
                            label = { Text(stringResource(option.labelRes)) },
                        )
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.library_tutorial_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (filtered.isEmpty()) {
                item { Text(stringResource(R.string.library_empty), style = MaterialTheme.typography.bodyMedium) }
            }
            items(filtered, key = { it.key }) { exercise ->
                ExerciseRow(exercise = exercise, supporting = exerciseSummary(exercise), onClick = { onOpenExercise(exercise.key) })
            }
        }
    }
}
