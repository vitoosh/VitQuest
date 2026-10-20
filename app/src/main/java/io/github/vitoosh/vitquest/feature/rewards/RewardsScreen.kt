package io.github.vitoosh.vitquest.feature.rewards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.model.BudgetEntry
import io.github.vitoosh.vitquest.core.model.BudgetEntryKind
import io.github.vitoosh.vitquest.core.model.ObtainedReward
import io.github.vitoosh.vitquest.core.model.Reward

private enum class RewardsTab(val labelRes: Int) {
    OVERVIEW(R.string.rewards_tab_overview),
    CATALOG(R.string.rewards_tab_catalog),
    HISTORY(R.string.rewards_tab_history),
    BUDGET(R.string.rewards_tab_budget),
}

data class RewardActions(
    val saveReward: (Long?, RewardForm) -> RewardFormErrors = { _, _ -> RewardFormErrors() },
    val setWishlist: (Reward, Boolean) -> Unit = { _, _ -> },
    val deleteReward: (Reward) -> Unit = {},
    val markObtained: (String, Reward, String) -> FieldError? = { _, _, _ -> null },
    val undoObtained: (ObtainedReward) -> Unit = {},
    val addBudgetEntry: (BudgetEntryKind, String, Long?, String) -> FieldError? = { _, _, _, _ -> null },
    val deleteBudgetEntry: (BudgetEntry) -> Unit = {},
    val setMonthlyBudget: (String) -> FieldError? = { null },
)

@Composable
fun RewardsRoute(onOpenSettings: () -> Unit, viewModel: RewardsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    RewardsScreen(
        uiState = uiState,
        message = message,
        onMessageShown = viewModel::messageShown,
        actions = RewardActions(
            saveReward = viewModel::saveReward,
            setWishlist = viewModel::setWishlist,
            deleteReward = viewModel::deleteReward,
            markObtained = viewModel::markObtained,
            undoObtained = viewModel::undoObtained,
            addBudgetEntry = viewModel::addBudgetEntry,
            deleteBudgetEntry = viewModel::deleteBudgetEntry,
            setMonthlyBudget = viewModel::setMonthlyBudget,
        ),
        onOpenSettings = onOpenSettings,
    )
}

/** Rewards tab: XP and level, real rewards to save for, what the user got, and the real-money budget. */
@Composable
fun RewardsScreen(
    uiState: RewardsUiState?,
    actions: RewardActions,
    onOpenSettings: () -> Unit,
    message: RewardMessage? = null,
    onMessageShown: () -> Unit = {},
) {
    var selected by rememberSaveable { mutableIntStateOf(RewardsTab.OVERVIEW.ordinal) }
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(message) {
        val text = message?.let { resources.getString(it.textRes) } ?: return@LaunchedEffect
        onMessageShown()
        snackbar.showSnackbar(text)
    }
    Scaffold(
        topBar = {
            VqTopAppBar(title = stringResource(R.string.nav_rewards)) {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryScrollableTabRow(selectedTabIndex = selected, edgePadding = 0.dp) {
                RewardsTab.entries.forEach { tab ->
                    Tab(selected = selected == tab.ordinal, onClick = { selected = tab.ordinal }, text = { Text(stringResource(tab.labelRes)) })
                }
            }
            if (uiState == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
            when (RewardsTab.entries[selected]) {
                RewardsTab.OVERVIEW -> OverviewTab(uiState, onOpenCatalog = { selected = RewardsTab.CATALOG.ordinal })
                RewardsTab.CATALOG -> CatalogTab(uiState, actions)
                RewardsTab.HISTORY -> RewardHistoryTab(uiState, actions)
                RewardsTab.BUDGET -> BudgetTab(uiState, actions)
            }
        }
    }
}

private val RewardMessage.textRes: Int
    get() = when (this) {
        RewardMessage.OBTAINED -> R.string.rewards_obtained
        RewardMessage.ALREADY_OBTAINED -> R.string.rewards_already_obtained
        RewardMessage.REWARD_MISSING -> R.string.rewards_missing
        RewardMessage.UNDONE -> R.string.rewards_undone
    }
