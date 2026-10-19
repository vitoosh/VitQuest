package io.github.vitoosh.vitquest.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.FeaturePreviewRow
import io.github.vitoosh.vitquest.core.designsystem.component.IconBubble
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.feature.profile.BodyMetricsSection
import io.github.vitoosh.vitquest.feature.profile.ExperienceSection
import io.github.vitoosh.vitquest.feature.profile.FormUpdate
import io.github.vitoosh.vitquest.feature.profile.GoalsSection
import io.github.vitoosh.vitquest.feature.profile.WorkoutPreferencesSection

@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingScreen(
        uiState = uiState,
        onUpdateForm = viewModel::updateForm,
        onNext = viewModel::next,
        onSkip = viewModel::skip,
        onBack = viewModel::back,
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onUpdateForm: FormUpdate,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(enabled = uiState.step != OnboardingStep.WELCOME, onBack = onBack)

    Scaffold(
        topBar = {
            if (uiState.step != OnboardingStep.WELCOME) {
                Column {
                    VqTopAppBar(
                        title = stringResource(
                            R.string.onboarding_step_counter,
                            uiState.stepNumber,
                            uiState.totalQuestionSteps,
                        ),
                        onNavigateBack = onBack,
                    )
                    LinearProgressIndicator(
                        progress = { uiState.stepNumber.toFloat() / uiState.totalQuestionSteps },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        bottomBar = {
            OnboardingBottomBar(uiState = uiState, onNext = onNext, onSkip = onSkip)
        },
        modifier = Modifier.imePadding(),
    ) { padding ->
        CenteredContent(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = VqSpacing.lg, vertical = VqSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(VqSpacing.lg),
            ) {
                when (uiState.step) {
                    OnboardingStep.WELCOME -> WelcomeStep()
                    OnboardingStep.BODY -> {
                        StepHeader(R.string.onboarding_body_title, R.string.onboarding_body_description)
                        BodyMetricsSection(form = uiState.form, errors = uiState.errors, onUpdate = onUpdateForm)
                    }
                    OnboardingStep.EXPERIENCE -> {
                        StepHeader(R.string.onboarding_experience_title, R.string.onboarding_experience_description)
                        ExperienceSection(form = uiState.form, onUpdate = onUpdateForm)
                    }
                    OnboardingStep.PREFERENCES -> {
                        StepHeader(R.string.onboarding_preferences_title, R.string.onboarding_preferences_description)
                        WorkoutPreferencesSection(form = uiState.form, onUpdate = onUpdateForm)
                    }
                    OnboardingStep.GOALS -> {
                        StepHeader(R.string.onboarding_goals_title, R.string.onboarding_goals_description)
                        GoalsSection(form = uiState.form, errors = uiState.errors, onUpdate = onUpdateForm)
                        if (uiState.saveFailed) {
                            Text(
                                text = stringResource(R.string.error_save_failed),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    uiState: OnboardingUiState,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = VqSpacing.lg, vertical = VqSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val canSkip = uiState.step == OnboardingStep.BODY ||
                uiState.step == OnboardingStep.EXPERIENCE ||
                uiState.step == OnboardingStep.PREFERENCES
            if (canSkip) {
                TextButton(onClick = onSkip) { Text(stringResource(R.string.action_skip)) }
            }
            val savingDescription = stringResource(R.string.saving)
            Button(onClick = onNext, enabled = !uiState.isSaving) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(18.dp)
                            .semantics { contentDescription = savingDescription },
                    )
                } else {
                    Text(
                        stringResource(
                            when (uiState.step) {
                                OnboardingStep.WELCOME -> R.string.onboarding_start
                                OnboardingStep.GOALS -> R.string.onboarding_finish
                                else -> R.string.action_continue
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.lg)) {
        IconBubble(icon = VqIcons.Bolt, size = 64)
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.onboarding_welcome_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column {
            FeaturePreviewRow(
                icon = VqIcons.Walk,
                title = stringResource(R.string.onboarding_welcome_point_move_title),
                description = stringResource(R.string.onboarding_welcome_point_move_description),
                trailing = null,
            )
            FeaturePreviewRow(
                icon = VqIcons.Restaurant,
                title = stringResource(R.string.onboarding_welcome_point_eat_title),
                description = stringResource(R.string.onboarding_welcome_point_eat_description),
                trailing = null,
            )
            FeaturePreviewRow(
                icon = VqIcons.Trophy,
                title = stringResource(R.string.onboarding_welcome_point_reward_title),
                description = stringResource(R.string.onboarding_welcome_point_reward_description),
                trailing = null,
            )
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Row(
                modifier = Modifier.padding(VqSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(VqSpacing.md),
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = stringResource(R.string.onboarding_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun StepHeader(titleRes: Int, descriptionRes: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(descriptionRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
