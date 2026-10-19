package io.github.vitoosh.vitquest.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.BuildConfig
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.locale.AppLanguage
import io.github.vitoosh.vitquest.core.locale.AppLocales
import io.github.vitoosh.vitquest.core.model.ThemeMode
import io.github.vitoosh.vitquest.core.ui.labelRes

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    onEditProfile: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    var language by remember { mutableStateOf(AppLocales.current()) }
    SettingsScreen(
        themeMode = themeMode,
        language = language,
        onThemeModeSelected = viewModel::setThemeMode,
        onLanguageSelected = { selected ->
            language = selected
            // AppCompat recreates the activity with the new resources.
            AppLocales.apply(selected)
        },
        onEditProfile = onEditProfile,
        onDeleteAllData = viewModel::deleteAllData,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun SettingsScreen(
    themeMode: ThemeMode?,
    language: AppLanguage,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onEditProfile: () -> Unit,
    onDeleteAllData: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { VqTopAppBar(title = stringResource(R.string.settings_title), onNavigateBack = onNavigateBack) },
    ) { padding ->
        CenteredContent(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = VqSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(VqSpacing.sm),
            ) {
                SettingsSectionHeader(R.string.settings_section_profile)
                ListItem(
                    headlineContent = { Text(stringResource(R.string.edit_profile_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_profile_description)) },
                    leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable(role = Role.Button, onClick = onEditProfile),
                )

                HorizontalDivider()
                SettingsSectionHeader(R.string.settings_section_theme)
                OptionGroup(
                    options = ThemeMode.entries,
                    selected = themeMode,
                    onSelect = onThemeModeSelected,
                    label = { stringResource(it.labelRes) },
                )

                HorizontalDivider()
                SettingsSectionHeader(R.string.settings_section_language)
                OptionGroup(
                    options = AppLanguage.entries,
                    selected = language,
                    onSelect = onLanguageSelected,
                    label = { stringResource(it.labelRes) },
                )

                HorizontalDivider()
                SettingsSectionHeader(R.string.settings_section_privacy)
                Row(
                    modifier = Modifier.padding(horizontal = VqSpacing.lg, vertical = VqSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(VqSpacing.md),
                ) {
                    Icon(VqIcons.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(R.string.settings_privacy_description),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_delete_all_data)) },
                    supportingContent = { Text(stringResource(R.string.settings_delete_all_data_description)) },
                    leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    colors = ListItemDefaults.colors(
                        headlineColor = MaterialTheme.colorScheme.error,
                        leadingIconColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.clickable(role = Role.Button) { showDeleteDialog = true },
                )

                HorizontalDivider()
                SettingsSectionHeader(R.string.settings_section_about)
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = VqSpacing.lg),
                )
                Text(
                    text = stringResource(R.string.settings_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = VqSpacing.lg),
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.settings_delete_dialog_title)) },
            text = { Text(stringResource(R.string.settings_delete_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAllData()
                    },
                ) {
                    Text(stringResource(R.string.settings_delete_dialog_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SettingsSectionHeader(titleRes: Int) {
    SectionHeader(
        text = stringResource(titleRes),
        modifier = Modifier.padding(horizontal = VqSpacing.lg, vertical = VqSpacing.xs),
    )
}

@Composable
private fun <T> OptionGroup(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
) {
    Column(modifier = Modifier.selectableGroup()) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = option == selected, onClick = { onSelect(option) }, role = Role.RadioButton)
                    .padding(horizontal = VqSpacing.lg, vertical = VqSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VqSpacing.md),
            ) {
                RadioButton(selected = option == selected, onClick = null)
                Text(text = label(option), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
