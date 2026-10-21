package io.github.vitoosh.vitquest.feature.workouts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import io.github.vitoosh.vitquest.core.model.WorkoutRequest
import io.github.vitoosh.vitquest.feature.workouts.generator.GeneratorTab
import io.github.vitoosh.vitquest.feature.workouts.history.HistoryTab
import io.github.vitoosh.vitquest.feature.workouts.library.LibraryTab

private enum class WorkoutsTab(val labelRes: Int) {
    PLAN(R.string.workouts_tab_plan),
    LIBRARY(R.string.workouts_tab_library),
    HISTORY(R.string.workouts_tab_history),
}

/** Workouts tab: generator, exercise library with tutorials, and history. */
@Composable
fun WorkoutsScreen(
    onOpenSettings: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenProgress: () -> Unit,
    onStartWorkout: (WorkoutRequest) -> Unit,
    onOpenExercise: (String) -> Unit,
) {
    var selected by rememberSaveable { mutableIntStateOf(WorkoutsTab.PLAN.ordinal) }
    Scaffold(
        topBar = {
            VqTopAppBar(title = stringResource(R.string.nav_workouts)) {
                IconButton(onClick = onOpenHabits) {
                    Icon(VqIcons.Checklist, contentDescription = stringResource(R.string.habits_title))
                }
                IconButton(onClick = onOpenProgress) {
                    Icon(VqIcons.Insights, contentDescription = stringResource(R.string.progress_title))
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = selected) {
                WorkoutsTab.entries.forEach { tab ->
                    Tab(
                        selected = selected == tab.ordinal,
                        onClick = { selected = tab.ordinal },
                        text = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
            when (WorkoutsTab.entries[selected]) {
                WorkoutsTab.PLAN -> GeneratorTab(onStart = onStartWorkout, onOpenExercise = onOpenExercise)
                WorkoutsTab.LIBRARY -> LibraryTab(onOpenExercise = onOpenExercise)
                WorkoutsTab.HISTORY -> HistoryTab()
            }
        }
    }
}
