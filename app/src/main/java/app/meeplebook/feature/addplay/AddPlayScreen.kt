package app.meeplebook.feature.addplay

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.meeplebook.R
import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.PlayerColor
import app.meeplebook.core.ui.asString
import app.meeplebook.feature.addplay.effect.AddPlayUiEffect
import app.meeplebook.ui.components.RowItemImage
import app.meeplebook.ui.components.ScreenPadding
import app.meeplebook.ui.theme.MeepleBookTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// EU date format: dd/MM/yyyy
private val EU_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())

@Composable
fun AddPlayScreen(
    gameId: Long? = null,
    gameName: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: AddPlayViewModel = hiltViewModel()
) {
    val uiState by viewModel.combinedUiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = LocalResources.current

    // Path 2 entry: if we already have a game, select it immediately.
    LaunchedEffect(gameId, gameName) {
        if (gameId != null && gameName != null) {
            viewModel.onEvent(
                AddPlayEvent.GameSearchEvent.GameSelected(
                    gameId = gameId,
                    gameName = gameName
                )
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                AddPlayUiEffect.NavigateBack -> onNavigateBack()
                is AddPlayUiEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message.asString(resources))
                }
            }
        }
    }

    AddPlayScreenRoot(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = { viewModel.onEvent(it) },
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayScreenRoot(
    uiState: AddPlayUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onEvent: (AddPlayEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = uiState is AddPlayUiState.GameSelected

    val onBackPressed: () -> Unit = {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onEvent(AddPlayEvent.ActionEvent.CancelClicked)
        }
    }

    BackHandler(enabled = hasUnsavedChanges) {
        onBackPressed()
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.add_play_discard_title)) },
            text = { Text(stringResource(R.string.add_play_discard_message)) },
            confirmButton = {
                TextButton(onClick = { onEvent(AddPlayEvent.ActionEvent.CancelClicked) }) {
                    Text(stringResource(R.string.add_play_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.add_play_discard_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState) {
                            is AddPlayUiState.GameSelected -> uiState.gameName
                            is AddPlayUiState.GameSearch -> stringResource(R.string.add_play_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.add_play_cancel)
                        )
                    }
                },
                actions = {
                    if (uiState is AddPlayUiState.GameSelected) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            TextButton(
                                onClick = { onEvent(AddPlayEvent.ActionEvent.SaveClicked) },
                                enabled = uiState.canSave
                            ) {
                                Text(stringResource(R.string.add_play_save))
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState is AddPlayUiState.GameSelected) {
                val hiddenFields = buildList {
                    if (!uiState.showQuantity) add(OptionalField.QUANTITY)
                    if (!uiState.showIncomplete) add(OptionalField.INCOMPLETE)
                    if (!uiState.showComments) add(OptionalField.COMMENTS)
                }
                if (hiddenFields.isNotEmpty()) {
                    AddFieldFab(hiddenFields = hiddenFields, onEvent = onEvent)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("addPlayScreen")
        ) {
            when (uiState) {
                is AddPlayUiState.GameSearch -> GameSearchContent(
                    state = uiState,
                    onEvent = onEvent
                )

                is AddPlayUiState.GameSelected -> GameSelectedContent(
                    state = uiState,
                    onEvent = onEvent,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Path 1 – Game Search
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GameSearchContent(
    state: AddPlayUiState.GameSearch,
    onEvent: (AddPlayEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.gameSearchQuery,
            onValueChange = {
                onEvent(AddPlayEvent.GameSearchEvent.GameSearchQueryChanged(it))
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text(stringResource(R.string.add_play_search_game_label)) },
            placeholder = { Text(stringResource(R.string.add_play_search_game_placeholder)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenPadding.Horizontal, vertical = ScreenPadding.Small)
                .testTag("gameSearchField")
        )

        if (state.gameSearchQuery.isNotEmpty() && state.gameSearchResults.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.add_play_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = ScreenPadding.Small),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("gameSearchResults")
            ) {
                items(
                    items = state.gameSearchResults,
                    key = { it.gameId }
                ) { game ->
                    GameSearchResultItem(
                        game = game,
                        onClick = {
                            onEvent(
                                AddPlayEvent.GameSearchEvent.GameSelected(
                                    gameId = game.gameId,
                                    gameName = game.name
                                )
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = ScreenPadding.Horizontal))
                }
            }
        }
    }
}

@Composable
private fun GameSearchResultItem(
    game: SearchResultGameItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = ScreenPadding.Horizontal,
                vertical = ScreenPadding.ItemSpacing
            )
            .testTag("gameSearchResult_${game.gameId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowItemImage(
            thumbnailUrl = game.thumbnailUrl,
            contentDescription = game.name
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = game.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            game.yearPublished?.let { year ->
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Path 2 – Full Form
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GameSelectedContent(
    state: AddPlayUiState.GameSelected,
    onEvent: (AddPlayEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("addPlayForm"),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            LocationSection(
                locationState = state.location,
                onEvent = onEvent
            )
        }

        item { DateDurationRow(date = state.date, durationMinutes = state.durationMinutes, onEvent = onEvent) }

        if (state.showQuantity || state.showIncomplete) {
            item { QuantityIncompleteRow(state = state, onEvent = onEvent) }
        }

        if (state.showComments) {
            item { CommentsSection(comments = state.comments, onEvent = onEvent) }
        }

        item { SuggestedPlayersSection(state = state, onEvent = onEvent) }

        item { PlayersSection(state = state, onEvent = onEvent, snackbarHostState = snackbarHostState) }
    }
}

// ─── Date + Duration ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDurationRow(
    date: Instant,
    durationMinutes: Int?,
    onEvent: (AddPlayEvent) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    val formatted = remember(date) {
        date.atZone(ZoneId.systemDefault()).toLocalDate().format(EU_DATE_FORMATTER)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding.Horizontal, vertical = ScreenPadding.Small),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        OutlinedTextField(
            value = formatted,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.add_play_date_label)) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .weight(1f)
                .clickable { showDatePicker = true }
                .testTag("dateField"),
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
            )
        )

        OutlinedTextField(
            value = durationMinutes?.toString() ?: "",
            onValueChange = { raw ->
                val parsed = if (raw.isEmpty()) null else raw.toIntOrNull()
                if (raw.isEmpty() || parsed != null) {
                    onEvent(AddPlayEvent.MetadataEvent.DurationChanged(parsed))
                }
            },
            label = { Text(stringResource(R.string.add_play_duration_label)) },
            placeholder = { Text(stringResource(R.string.add_play_duration_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .width(112.dp)
                .testTag("durationField")
        )
    }

    if (showDatePicker) {
        val initialMillis = remember(date) {
            date.atZone(ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedInstant = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                        onEvent(AddPlayEvent.MetadataEvent.DateChanged(selectedInstant))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationSection(
    locationState: LocationState,
    onEvent: (AddPlayEvent) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val showSuggestions = isFocused && locationState.suggestions.isNotEmpty()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = ScreenPadding.Horizontal,
                vertical = ScreenPadding.Small
            )
    ) {
        ExposedDropdownMenuBox(
            expanded = showSuggestions,
            onExpandedChange = {}
        ) {
            OutlinedTextField(
                value = locationState.value ?: "",
                onValueChange = { onEvent(AddPlayEvent.MetadataEvent.LocationChanged(it)) },
                label = { Text(stringResource(R.string.add_play_location_label)) },
                placeholder = { Text(stringResource(R.string.add_play_location_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        locationState.suggestions.firstOrNull()?.let {
                            onEvent(AddPlayEvent.MetadataEvent.LocationChanged(it))
                        }
                        focusManager.clearFocus(true)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                    .onFocusChanged { isFocused = it.isFocused }
                    .testTag("locationField")
            )

            ExposedDropdownMenu(
                expanded = showSuggestions,
                onDismissRequest = { focusManager.clearFocus() },
                modifier = Modifier.testTag("locationSuggestions")
            ) {
                locationState.suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            onEvent(AddPlayEvent.MetadataEvent.LocationChanged(suggestion))
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }

        // Recent / top locations chips (always visible when available)
        val topLocations = locationState.recentLocations.take(10)
        if (topLocations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.testTag("recentLocationChips")
            ) {
                items(topLocations) { loc ->
                    FilterChip(
                        selected = locationState.value == loc,
                        onClick = { onEvent(AddPlayEvent.MetadataEvent.LocationChanged(loc)) },
                        label = { Text(loc) }
                    )
                }
            }
        }
    }
}

// ─── Optional fields ─────────────────────────────────────────────────────────

@Composable
private fun QuantityIncompleteRow(
    state: AddPlayUiState.GameSelected,
    onEvent: (AddPlayEvent) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding.Horizontal, vertical = ScreenPadding.Small),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.showQuantity) {
            OutlinedTextField(
                value = if (state.quantity == 1) "" else state.quantity.toString(),
                onValueChange = { raw ->
                    val parsed = raw.toIntOrNull()
                    if (raw.isEmpty()) {
                        onEvent(AddPlayEvent.MetadataEvent.QuantityChanged(null))
                    } else if (parsed != null && parsed <= 999) {
                        onEvent(AddPlayEvent.MetadataEvent.QuantityChanged(parsed))
                    }
                },
                label = { Text(stringResource(R.string.add_play_quantity_label)) },
                placeholder = { Text("1") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .width(96.dp)
                    .testTag("quantityField")
            )
        }

        if (state.showIncomplete) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = stringResource(R.string.add_play_optional_incomplete),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = state.incomplete,
                    onCheckedChange = { onEvent(AddPlayEvent.MetadataEvent.IncompleteToggled(it)) },
                    modifier = Modifier.testTag("incompleteToggle")
                )
            }
        }
    }
}

@Composable
private fun CommentsSection(
    comments: String,
    onEvent: (AddPlayEvent) -> Unit
) {
    OutlinedTextField(
        value = comments,
        onValueChange = { onEvent(AddPlayEvent.MetadataEvent.CommentsChanged(it)) },
        label = { Text(stringResource(R.string.add_play_comments_label)) },
        placeholder = { Text(stringResource(R.string.add_play_comments_placeholder)) },
        minLines = 1,
        maxLines = 5,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding.Horizontal, vertical = ScreenPadding.Small)
            .testTag("commentsField")
    )
}

@Composable
private fun AddFieldFab(
    hiddenFields: List<OptionalField>,
    onEvent: (AddPlayEvent) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FloatingActionButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add_play_add_optional_field)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            hiddenFields.forEach { field ->
                val label = when (field) {
                    OptionalField.QUANTITY -> stringResource(R.string.add_play_optional_quantity)
                    OptionalField.INCOMPLETE -> stringResource(R.string.add_play_optional_incomplete)
                    OptionalField.COMMENTS -> stringResource(R.string.add_play_optional_comments)
                }
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onEvent(AddPlayEvent.MetadataEvent.ShowOptionalField(field))
                        expanded = false
                    }
                )
            }
        }
    }
}

// ─── Players ─────────────────────────────────────────────────────────────────

private val PLAYER_ROW_HEIGHT = 72.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayersSection(
    state: AddPlayUiState.GameSelected,
    onEvent: (AddPlayEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val players = state.players.players
    val undoLabel = stringResource(R.string.undo)
    val playerRemovedMsg = stringResource(R.string.player_removed)

    var pendingUndo by remember { mutableStateOf<Pair<PlayerEntryUi, Int>?>(null) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { PLAYER_ROW_HEIGHT.toPx() }

    LaunchedEffect(pendingUndo) {
        pendingUndo?.let { (player, atIndex) ->
            val result = snackbarHostState.showSnackbar(
                message = playerRemovedMsg,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                onEvent(AddPlayEvent.PlayerListEvent.RestorePlayer(player, atIndex))
            }
            pendingUndo = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding.Horizontal, vertical = ScreenPadding.Small)
    ) {
        Text(
            text = stringResource(R.string.add_play_players_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        players.forEachIndexed { index, player ->
            key(player.playerIdentity.name + (player.playerIdentity.username ?: "")) {
                val swipeState = rememberSwipeToDismissBoxState()

                LaunchedEffect(swipeState.currentValue) {
                    if (swipeState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
                        pendingUndo = player to index
                        onEvent(AddPlayEvent.PlayerListEvent.RemovePlayer(player.playerIdentity))
                    }
                }

                // Capture current index for drag lambdas; rememberUpdatedState keeps
                // them fresh after list reorders without restarting pointerInput.
                val currentIndex by rememberUpdatedState(index)
                val currentPlayers by rememberUpdatedState(players)

                val isDragging = draggingIndex == index
                SwipeToDismissBox(
                    state = swipeState,
                    enableDismissFromStartToEnd = true,
                    enableDismissFromEndToStart = true,
                    backgroundContent = {
                        val isDeleteDir = swipeState.targetValue == SwipeToDismissBoxValue.StartToEnd
                        val isEditDir = swipeState.targetValue == SwipeToDismissBoxValue.EndToStart
                        val bgColor = when {
                            isDeleteDir -> MaterialTheme.colorScheme.errorContainer
                            isEditDir -> MaterialTheme.colorScheme.secondaryContainer
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(bgColor)
                                .padding(horizontal = 16.dp),
                        ) {
                            if (isDeleteDir) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.align(Alignment.CenterStart),
                                )
                            } else if (isEditDir) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer { if (isDragging) translationY = dragOffsetY },
                ) {
                    PlayerEntryRow(
                        player = player,
                        onWinnerToggle = {
                            onEvent(
                                AddPlayEvent.PlayerScoreEvent.WinnerToggled(
                                    playerIdentity = player.playerIdentity,
                                    isWinner = !player.isWinner,
                                )
                            )
                        },
                        onDragStart = {
                            draggingIndex = currentIndex
                            dragOffsetY = 0f
                        },
                        onDrag = { deltaY -> dragOffsetY += deltaY },
                        onDragEnd = {
                            val rawDelta = dragOffsetY / rowHeightPx
                            val targetIndex = (draggingIndex + rawDelta.roundToInt())
                                .coerceIn(0, currentPlayers.lastIndex)
                            if (targetIndex != draggingIndex) {
                                onEvent(
                                    AddPlayEvent.PlayerListEvent.PlayerReordered(
                                        fromIndex = draggingIndex,
                                        toIndex = targetIndex,
                                    )
                                )
                            }
                            draggingIndex = -1
                            dragOffsetY = 0f
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestedPlayersSection(
    state: AddPlayUiState.GameSelected,
    onEvent: (AddPlayEvent) -> Unit
) {
    var showMorePlayersDialog by remember { mutableStateOf(false) }

    val addedIdentities = state.players.players.map { it.playerIdentity }.toSet()
    val availableSuggestions = state.playersByLocation
        .filter { it.playerIdentity !in addedIdentities }

    if (availableSuggestions.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ScreenPadding.Horizontal,
                    vertical = ScreenPadding.Small
                )
        ) {
            Text(
                text = stringResource(R.string.add_play_suggested_players_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            val chipSuggestions = availableSuggestions.take(10)
            val hasMore = availableSuggestions.size > 10

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.testTag("playerSuggestionChips")
            ) {
                items(
                    items = chipSuggestions,
                    key = { it.playerIdentity.name + (it.playerIdentity.username ?: "") }
                ) { suggestion ->
                    SuggestionChip(
                        onClick = {
                            onEvent(
                                AddPlayEvent.PlayerListEvent.AddPlayerFromSuggestion(
                                    playerIdentity = suggestion.playerIdentity,
                                    startPosition = state.players.players.size + 1
                                )
                            )
                        },
                        label = { Text(suggestion.playerIdentity.name) }
                    )
                }

                if (hasMore) {
                    item {
                        AssistChip(
                            onClick = { showMorePlayersDialog = true },
                            label = { Text(stringResource(R.string.add_play_more_players)) },
                            modifier = Modifier.testTag("morePlayersChip")
                        )
                    }
                }
            }
        }
    }

    if (showMorePlayersDialog) {
        val allAvailable = state.playersByLocation
            .filter { it.playerIdentity !in addedIdentities }

        MorePlayersDialog(
            suggestions = allAvailable,
            onPlayerSelected = { playerIdentity ->
                onEvent(
                    AddPlayEvent.PlayerListEvent.AddPlayerFromSuggestion(
                        playerIdentity = playerIdentity,
                        startPosition = state.players.players.size + 1
                    )
                )
                showMorePlayersDialog = false
            },
            onDismiss = { showMorePlayersDialog = false }
        )
    }
}

@Composable
private fun PlayerEntryRow(
    player: PlayerEntryUi,
    onWinnerToggle: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (deltaY: Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val colorEnum = PlayerColor.fromString(player.color)
    val winnerTint = Color(0xFFFFAB40)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(PLAYER_ROW_HEIGHT)
            .background(
                if (player.isWinner) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surface,
            )
            .padding(horizontal = 4.dp)
            .testTag("playerEntry_${player.playerIdentity.name}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Position badge
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = player.startPosition.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Color indicator
        if (colorEnum != null) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(colorEnum.hexColor)))
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
        } else if (!player.color.isNullOrBlank()) {
            Text(
                text = player.color,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Name + username
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = player.playerIdentity.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (player.isWinner) FontWeight.Bold else FontWeight.Normal,
            )
            player.playerIdentity.username?.let { uname ->
                Text(
                    text = "@$uname",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Score (tappable — future dialog)
        Text(
            text = player.score?.toString() ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clickable { /* TODO: score dialog */ }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )

        // Winner star toggle
        IconButton(onClick = onWinnerToggle) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (player.isWinner) winnerTint else MaterialTheme.colorScheme.outlineVariant,
            )
        }

        // Drag handle — use rememberUpdatedState so pointerInput(Unit) always
        // calls the latest lambdas even after list reorders
        val latestOnDragStart by rememberUpdatedState(onDragStart)
        val latestOnDrag by rememberUpdatedState(onDrag)
        val latestOnDragEnd by rememberUpdatedState(onDragEnd)

        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { latestOnDragStart() },
                        onDrag = { change, amount ->
                            change.consume()
                            latestOnDrag(amount.y)
                        },
                        onDragEnd = { latestOnDragEnd() },
                        onDragCancel = { latestOnDragEnd() },
                    )
                },
        )
    }
}

@Composable
private fun MorePlayersDialog(
    suggestions: List<PlayerSuggestion>,
    onPlayerSelected: (PlayerIdentity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_play_more_players_title)) },
        text = {
            if (suggestions.isEmpty()) {
                Text(
                    text = stringResource(R.string.add_play_no_player_suggestions),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn {
                    items(
                        items = suggestions,
                        key = { it.playerIdentity.name + (it.playerIdentity.username ?: "") }
                    ) { suggestion ->
                        Text(
                            text = suggestion.playerIdentity.name +
                                    (suggestion.playerIdentity.username
                                        ?.let { " (@$it)" } ?: ""),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayerSelected(suggestion.playerIdentity) }
                                .padding(vertical = 12.dp)
                                .testTag("morePlayerItem_${suggestion.playerIdentity.name}")
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

private fun previewGameSearchState(query: String = "", hasResults: Boolean = false) =
    AddPlayUiState.GameSearch(
        gameSearchQuery = query,
        gameSearchResults = if (hasResults) listOf(
            SearchResultGameItem(1L, "Catan", 1995, null),
            SearchResultGameItem(2L, "Ticket to Ride", 2004, null),
            SearchResultGameItem(3L, "Pandemic", 2008, null),
        ) else emptyList()
    )

private fun previewGameSelectedState(
    isSaving: Boolean = false,
    hasPlayers: Boolean = true,
    hasSuggestions: Boolean = true
): AddPlayUiState.GameSelected {
    val players = if (hasPlayers) listOf(
        PlayerEntryUi.empty("Alice", 1),
        PlayerEntryUi.empty("Bob", 2)
    ) else emptyList()

    val suggestions = if (hasSuggestions) listOf(
        PlayerSuggestion(PlayerIdentity("Charlie", null, null)),
        PlayerSuggestion(PlayerIdentity("Diana", "diana_bgg", null)),
        PlayerSuggestion(PlayerIdentity("Eve", null, null)),
        PlayerSuggestion(PlayerIdentity("Frank", null, null)),
        PlayerSuggestion(PlayerIdentity("Grace", null, null)),
        PlayerSuggestion(PlayerIdentity("Heidi", null, null)),
        PlayerSuggestion(PlayerIdentity("Ivan", null, null)),
        PlayerSuggestion(PlayerIdentity("Judy", null, null)),
        PlayerSuggestion(PlayerIdentity("Kyle", null, null)),
        PlayerSuggestion(PlayerIdentity("Laura", null, null)),
        PlayerSuggestion(PlayerIdentity("Mallory", null, null)),
    ) else emptyList()

    return AddPlayUiState.GameSelected(
        gameId = 13,
        gameName = "Catan",
        date = Instant.parse("2026-03-30T18:00:00Z"),
        durationMinutes = 90,
        location = LocationState(
            value = "Home",
            suggestions = emptyList(),
            recentLocations = listOf("Home", "Game Café", "Bob's place"),
            isFocused = false
        ),
        players = PlayersState(
            players = players,
            colorsHistory = emptyList()
        ),
        playersByLocation = suggestions,
        isSaving = isSaving
    )
}

class AddPlayUiStatePreviewProvider : PreviewParameterProvider<AddPlayUiState> {
    override val values: Sequence<AddPlayUiState> = sequenceOf(
        previewGameSearchState(),
        previewGameSearchState(query = "Cat", hasResults = true),
        previewGameSelectedState(),
        previewGameSelectedState(isSaving = true),
        previewGameSelectedState(hasPlayers = false, hasSuggestions = false),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddPlayScreenPreview(
    @PreviewParameter(AddPlayUiStatePreviewProvider::class) previewState: AddPlayUiState
) {
    MeepleBookTheme {
        AddPlayScreenRoot(
            uiState = previewState,
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
