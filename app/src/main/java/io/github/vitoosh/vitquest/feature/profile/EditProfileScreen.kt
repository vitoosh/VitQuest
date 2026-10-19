package io.github.vitoosh.vitquest.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing

@Composable
fun EditProfileRoute(
    onNavigateBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onNavigateBack()
    }
    EditProfileScreen(
        uiState = uiState,
        onUpdateForm = viewModel::updateForm,
        onSave = viewModel::save,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun EditProfileScreen(
    uiState: EditProfileUiState,
    onUpdateForm: FormUpdate,
    onSave: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            VqTopAppBar(title = stringResource(R.string.edit_profile_title), onNavigateBack = onNavigateBack)
        },
        modifier = Modifier.imePadding(),
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        CenteredContent(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(VqSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(VqSpacing.lg),
            ) {
                Text(
                    text = stringResource(R.string.edit_profile_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SectionHeader(stringResource(R.string.section_body))
                BodyMetricsSection(form = uiState.form, errors = uiState.errors, onUpdate = onUpdateForm)
                HorizontalDivider()
                SectionHeader(stringResource(R.string.section_experience))
                ExperienceSection(form = uiState.form, onUpdate = onUpdateForm)
                HorizontalDivider()
                SectionHeader(stringResource(R.string.section_preferences))
                WorkoutPreferencesSection(form = uiState.form, onUpdate = onUpdateForm)
                HorizontalDivider()
                SectionHeader(stringResource(R.string.section_goals))
                GoalsSection(form = uiState.form, errors = uiState.errors, onUpdate = onUpdateForm)

                if (!uiState.errors.isEmpty) {
                    Text(
                        text = stringResource(R.string.error_fix_fields),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (uiState.saveFailed) {
                    Text(
                        text = stringResource(R.string.error_save_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Button(
                    onClick = onSave,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(if (uiState.isSaving) R.string.saving else R.string.action_save))
                }
            }
        }
    }
}
