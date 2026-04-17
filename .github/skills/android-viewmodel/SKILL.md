---
name: android-viewmodel
description: Best practices for implementing Android ViewModels, specifically focused on StateFlow for UI state and SharedFlow for one-off events.
---

# Android ViewModel & State Management

## MeepleBook repo note

MeepleBook uses read-only `StateFlow` for UI state and `SharedFlow` for one-off UI effects, but
some screens — especially complex ones like Add Play — model state as:

- reducer-owned `baseState`
- state-derived query/search flows (`map + distinctUntilChanged`)
- external flows combined back into the exposed `uiState`

Do **not** introduce duplicate mutable query state just to follow the simple `_uiState` template.
In this repo, if the UI reads a value from `UiState`, that value must be owned by reducer state and
all debounce/search flows must derive from that state.

## Instructions

Use `ViewModel` to hold state and business logic. It must outlive configuration changes.

### 1. UI State (StateFlow)
*   **What**: Represents the persistent state of the UI (e.g., `Loading`, `Success(data)`, `Error`).
*   **Type**: `StateFlow<UiState>`.
*   **Initialization**: Must have an initial value.
*   **Exposure**: Expose as a read-only `StateFlow`, typically from a private backing state flow or
    from a combined state pipeline when the screen has reducer-owned state plus external flows.
    ```kotlin
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    ```
*   **Updates**: Update state using `.update { oldState -> ... }` or a reducer-driven state
    transition pipeline, depending on the screen's architecture.

### 2. One-Off Events (SharedFlow)
*   **What**: Transient events like "Show Toast", "Navigate to Screen", "Show Snackbar".
*   **Type**: `SharedFlow<UiEvent>`.
*   **Configuration**: Must use `replay = 0` to prevent events from re-triggering on screen rotation.
    ```kotlin
    private val _uiEvent = MutableSharedFlow<UiEvent>(replay = 0)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()
    ```
*   **Sending**: Use `.emit(event)` (suspend) or `.tryEmit(event)`.

### 3. Collecting in UI
*   **Compose**: Use `collectAsStateWithLifecycle()` for `StateFlow`.
    ```kotlin
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ```
    For `SharedFlow`, use `LaunchedEffect` with `LocalLifecycleOwner`.
*   **Views (XML)**: Use `repeatOnLifecycle(Lifecycle.State.STARTED)` within a coroutine.

### 4. Scope
*   Use `viewModelScope` for all coroutines started by the ViewModel.
*   Ideally, specific operations should be delegated to UseCases or Repositories.
