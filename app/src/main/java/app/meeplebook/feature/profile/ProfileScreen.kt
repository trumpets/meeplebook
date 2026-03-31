package app.meeplebook.feature.profile

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.meeplebook.R
import app.meeplebook.core.collection.model.CollectionViewMode
import app.meeplebook.core.preferences.StartingScreen
import app.meeplebook.ui.components.ScreenPadding
import app.meeplebook.ui.theme.MeepleBookTheme

/**
 * Profile / Settings screen entry point.
 *
 * @param onLogout Called when the user confirms logout; the caller is responsible for
 *                 navigating to the Login screen and clearing the back stack.
 * @param viewModel The ProfileViewModel (injected by Hilt).
 */
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ProfileUiEffect.NavigateToLogin -> onLogout()
                ProfileUiEffect.OpenSourceLicenses -> { /* TODO: navigate to open-source licenses screen */ }
            }
        }
    }

    ProfileContent(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.profile_title)) })
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // — User info header —
            if (uiState.username.isNotBlank()) {
                ListItem(
                    headlineContent = {
                        Text(uiState.username, style = MaterialTheme.typography.titleMedium)
                    },
                    leadingContent = {
                        Icon(Icons.Default.AccountCircle, contentDescription = null)
                    }
                )
                HorizontalDivider()
            }

            Spacer(modifier = Modifier.height(8.dp))

            // — App settings section —
            SettingsSectionHeader(stringResource(R.string.profile_section_app_settings))

            // Starting screen preference
            SettingsSegmentedRow(
                title = stringResource(R.string.profile_setting_starting_screen),
                options = StartingScreen.entries,
                selectedOption = uiState.startingScreen,
                labelFor = { screen ->
                    when (screen) {
                        StartingScreen.OVERVIEW -> stringResource(R.string.profile_starting_screen_overview)
                        StartingScreen.COLLECTION -> stringResource(R.string.profile_starting_screen_collection)
                    }
                },
                onOptionSelected = { onEvent(ProfileEvent.StartingScreenSelected(it)) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = ScreenPadding.Horizontal))

            // — Collection settings section —
            SettingsSectionHeader(stringResource(R.string.profile_section_collection))

            // Collection view mode preference
            SettingsSegmentedRow(
                title = stringResource(R.string.profile_setting_collection_view),
                options = CollectionViewMode.entries,
                selectedOption = uiState.collectionViewMode,
                labelFor = { mode ->
                    when (mode) {
                        CollectionViewMode.LIST -> stringResource(R.string.collection_view_list)
                        CollectionViewMode.GRID -> stringResource(R.string.collection_view_grid)
                    }
                },
                iconFor = { mode ->
                    when (mode) {
                        CollectionViewMode.LIST -> Icons.AutoMirrored.Filled.List
                        CollectionViewMode.GRID -> Icons.Default.GridView
                    }
                },
                onOptionSelected = { onEvent(ProfileEvent.CollectionViewModeSelected(it)) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = ScreenPadding.Horizontal))

            // Alphabet jump toggle
            ListItem(
                headlineContent = { Text(stringResource(R.string.profile_setting_alphabet_jump)) },
                supportingContent = { Text(stringResource(R.string.profile_setting_alphabet_jump_desc)) },
                trailingContent = {
                    Switch(
                        checked = uiState.collectionAlphabetJumpVisible,
                        onCheckedChange = { visible ->
                            onEvent(ProfileEvent.CollectionAlphabetJumpVisibilityChanged(visible))
                        }
                    )
                }
            )

            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // — About section —
            SettingsSectionHeader(stringResource(R.string.profile_section_about))

            ListItem(
                headlineContent = { Text(stringResource(R.string.profile_open_source_licenses)) },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.clickable { onEvent(ProfileEvent.OpenSourceLicensesClicked) }
            )

            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // — Logout button —
            Button(
                onClick = { onEvent(ProfileEvent.LogoutClicked) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPadding.Horizontal)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.profile_logout))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (uiState.isLogoutConfirmVisible) {
        AlertDialog(
            onDismissRequest = { onEvent(ProfileEvent.LogoutDismissed) },
            title = { Text(stringResource(R.string.profile_logout_confirm_title)) },
            text = { Text(stringResource(R.string.profile_logout_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { onEvent(ProfileEvent.LogoutConfirmed) }) {
                    Text(stringResource(R.string.profile_logout_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(ProfileEvent.LogoutDismissed) }) {
                    Text(stringResource(R.string.profile_logout_cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = ScreenPadding.Horizontal,
            end = ScreenPadding.Horizontal,
            top = 8.dp,
            bottom = 4.dp
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingsSegmentedRow(
    title: String,
    options: List<T>,
    selectedOption: T,
    labelFor: @Composable (T) -> String,
    onOptionSelected: (T) -> Unit,
    iconFor: (@Composable (T) -> ImageVector)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding.Horizontal, vertical = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selectedOption,
                    onClick = { onOptionSelected(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    icon = {
                        if (iconFor != null) {
                            Icon(
                                imageVector = iconFor(option),
                                contentDescription = labelFor(option)
                            )
                        } else {
                            SegmentedButtonDefaults.Icon(active = option == selectedOption)
                        }
                    }
                ) {
                    Text(text = labelFor(option))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfileContentPreview() {
    MeepleBookTheme {
        ProfileContent(
            uiState = ProfileUiState(
                username = "meeple_fan",
                startingScreen = StartingScreen.OVERVIEW,
                collectionViewMode = CollectionViewMode.LIST,
                collectionAlphabetJumpVisible = true
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileContentLogoutDialogPreview() {
    MeepleBookTheme {
        ProfileContent(
            uiState = ProfileUiState(
                username = "meeple_fan",
                isLogoutConfirmVisible = true
            )
        )
    }
}
