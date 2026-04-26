# Project Snapshot
- Android app `app.meeplebook` built with Kotlin + Compose + Hilt; modules are `:app` and `:lint-rules` (`settings.gradle.kts`).
- App entry: `app/src/main/java/app/meeplebook/MainActivity.kt` decides start route from `AuthRepository.getCurrentUser()` and then mounts `AppNavHost`.
- Navigation uses typed routes (`composable<Screen.X>`) in `ui/navigation/AppNavHost.kt` and tabbed home navigation in `feature/home/navigation/HomeNavHost.kt`.
- **Source of truth rule:** use the repo as it exists today when giving guidance or making changes. Multi-module is a planned future direction, but agents must not invent `:core:*` / `:feature:*` modules or rewrite docs as if they already exist unless the user explicitly asks for modularization work.

# Architecture That Matters
- Offline-first shape is implemented today: remote fetch -> local Room write -> UI observes flows from DB through repositories/use cases.
- Repositories are integration boundaries: `AuthRepositoryImpl`, `CollectionRepositoryImpl`, `PlaysRepositoryImpl` combine local/remote behavior and map exceptions to `AppResult` failures.
- Sync use cases (`core/sync/domain/*`) are the auth-gated sync entrypoints that workers should call; `SyncCollectionUseCase` and `SyncPlaysUseCase` wrap repository pull syncs, while `ObserveSyncStateUseCase` / `ObserveFullSyncStateUseCase` expose persisted sync status to UI/domain observers.
- Sync execution state is persisted in the single `sync_states` Room table keyed by `SyncType`; `SyncRunner` is the shared started/success/failed wrapper, `SyncDao` uses partial UPSERT queries for lifecycle updates, and `observeLastFullSync()` is derived from the collection/plays rows rather than stored separately.
- WorkManager workers live in `core/sync/work/`; keep them thin `@HiltWorker` `CoroutineWorker`s, resolve dependencies through constructor injection and `HiltWorkerFactory`, call the existing sync use cases/repository boundary, fail on max-retries-exceeded, retry only retryable network failures, and treat logged-out runs as `Result.success()`. Keep `androidx.hilt:hilt-compiler` on KSP alongside `hilt-work`; without it the worker factory map is empty and tests/runtime fall back to reflection.
- `SyncManager` is now the WorkManager orchestration boundary in `app/src/main/java/app/meeplebook/core/sync/manager/`; it owns unique work names, `NetworkType.CONNECTED` constraints, `ExistingWorkPolicy.KEEP`, and full-sync ordering of pending plays push -> plays pull -> collection pull.
- Trigger policy currently wired: `MainActivity` only resolves the logged-in start route. Overview / Collection / Plays now dispatch an explicit `ScreenOpened` event from their Composables instead of doing screen-entry work in ViewModel `init`. `OverviewViewModel` handles that event by always scheduling a daily periodic full sync and auto-enqueuing an immediate full sync only when Collection or Plays is stale (skip if either domain is already syncing or if both synced successfully within the last 15 minutes). Collection/Plays handle `ScreenOpened` with the same freshness gate per-domain, successful play saves enqueue pending-play upload sync, and manual refresh in Overview/Collection/Plays always routes through `SyncManager` without that gate.
- Room is central (`core/database/MeepleBookDatabase.kt`): DAOs expose `Flow`, local data sources map entities <-> domain models.

# BGG Integration Patterns
- XML endpoints are in `core/network/BggApi.kt`; responses are parsed manually with XmlPull (`CollectionXmlParser`, `PlaysXmlParser`).
- Retry/backoff is centralized in `core/network/Retry.kt` and used by remote data sources for `202`, `429`, and `5xx`.
- Keep BGG wire dates on `yyyy-MM-dd` (`parseBggDate` / `formatBggDate`) even though user-facing dates are EU formatted `dd/MM/yyyy`; do not reuse the UI formatter for network parsing.
- Collection sync fetches boardgames and expansions separately with a 5s delay between calls (`CollectionRemoteDataSourceImpl`).
- Plays sync is paginated (100/page), delays 5s between pages, then reconciles deletions via `retainByRemoteIds` (`PlaysRepositoryImpl`).
- Pending play uploads post `FormBody` data to `geekplay.php` using the authenticated cookie session from `CurrentCredentialsStore`; include `playid` only for edits, keep `playdate`/`dateinput` aligned as `yyyy-MM-dd`, and retry both `PENDING` and `FAILED` plays on later sync attempts.

# UI + State Conventions
- ViewModels expose immutable `StateFlow` and consume events (`CollectionViewModel.onEvent`, `PlaysViewModel.onEvent`).
- Search uses debounced flows (`searchableFlow` in `core/ui/flow/SearchableFlow.kt`, and direct debounce in collection).
- Avoid infinite wall-clock reactive flows with `while(true)+delay`; compute time-dependent values on demand (see `.github/copilot-instructions.md`).
- `UiText` is the app-level text abstraction (`core/ui/UiText.kt`); render via `UiTextText` or `asString()` helpers.
- Sync chrome on Overview/Collection/Plays should be derived from persisted `SyncState` observers (`ObserveFullSyncStateUseCase` / `ObserveSyncStateUseCase`), not from direct sync results or local refresh jobs. Pull-to-refresh indicators are stricter: show them only for user-initiated refresh actions, let them follow the observed work to completion, and do not auto-show them just because equivalent work was started elsewhere.
- Shared reducer/effect screen abstractions live in `core/ui/architecture/`:
  - `Reducer<State, Event>`
  - `EffectProducer<State, Event, DomainEffect, UiEffect>`
  - `ProducedEffects<DomainEffect, UiEffect>`
  - `ReducerViewModel<State, Event, DomainEffect, UiEffect>`
- Reuse those abstractions for reducer-driven screens, but keep feature-owned base state, query flows,
  external observers, and `combine(baseState, externalData) -> uiState` mapping inside the feature.
- For simple reducer-driven forms with no external observed data (for example Login), expose the
  reducer-owned state directly as `uiState` instead of inventing a `combine(...)` layer, and model
  successful navigation as a one-shot `UiEffect` rather than a persistent success flag.
- If a one-shot UI effect depends on **derived** screen data that only exists in the final `uiState`
  (for example Collection alphabet-jump indices), emit a domain effect from the `EffectProducer`
  and resolve it in the ViewModel against the latest derived `uiState`. Do not duplicate that
  derived data into base state and do not make the Composable look it up from captured state.

## Single Source of Truth (CRITICAL)
For any given piece of UI data, there must be exactly ONE source of truth.

In ViewModels:
- The reducer-owned `UiState` is the single source of truth for all UI-visible data.
- Flows (including debounce/search flows) MUST be derived from state.
- Derived data MUST NEVER override or replace reducer state.

Violating this leads to desynchronization bugs and non-deterministic UI.

## ❌ Forbidden Pattern: Dual State (Reducer + Flow)

Do NOT store the same data in both:
- a `MutableStateFlow` (e.g. raw query), AND
- the reducer state

Do NOT "override" reducer state with values from flows during `combine`.

Example (BAD):

```kotlin
// reducer owns name
state.copy(name = ...)

// but also:
rawNameQuery.value = ...

// then later:
state.copy(name = data.query.name) // ❌ overriding reducer
```
This creates two sources of truth and will cause UI inconsistencies.

---

## ✅ The correct patterns

### 1. State-driven (preferred)

#### ✅ Preferred: State-driven flows

- Reducer owns all UI data.
- Flows derive from state via `map + distinctUntilChanged`.
- External data (search results, suggestions) is merged into UI state.

Example:

```kotlin
val nameQueryFlow =
    baseState
        .map { it.dialog.name }
        .distinctUntilChanged()

val suggestions =
    searchableFlow(nameQueryFlow) { query -> ... }

uiState = combine(baseState, suggestions) { state, suggestions ->
    state.copy(dialog = state.dialog.copy(suggestions = suggestions))
}
```
---

### 2. Input-only flows (rare, explicit)

#### ⚠️ Allowed (rare): Input-only flows

A raw `MutableStateFlow` may be used ONLY if:

- The value is NOT stored in reducer state
- The value is NOT rendered from reducer state
- It is purely transient input (e.g. pre-submit typing buffer)

If the UI displays the value → it MUST come from reducer state.

## 🚫 Anti-Patterns Checklist

Do NOT:

- ❌ Override reducer state inside `combine(...)`
- ❌ Store the same field in both reducer and `MutableStateFlow`
- ❌ Mutate `baseState.value` outside the reducer (except for effect results)
- ❌ Use `init { launch { collect { ... } } }` for UI state updates
- ❌ Push flow-derived values back into state

If you see any of the above → STOP and refactor.

## Testability Rule

A ViewModel should be testable as:

Given:
- initial state
- sequence of events

Expect:
- resulting state

If behavior depends on hidden collectors or external mutable flows,
the design is incorrect.

## Heuristic: "Who owns this value?"

For every field, ask:

👉 "Where does the UI read this value from?"

- If the answer is UiState → reducer MUST own it
- If the answer is a Flow → it MUST NOT exist in UiState

Never both.

## Project Examples

### ❌ Bad (previous implementation)

- `location.value` stored in reducer
- `rawLocationQuery` stored separately
- UI uses `queries.locationQuery` to override state

### ✅ Good

- `location.value` stored ONLY in reducer
- `locationQueryFlow` derived from state
- search results merged into state

---

### ❌ Bad

- name stored in dialog state
- raw name flow also stored
- state overwritten in combine

### ✅ Good

- name stored ONLY in dialog state
- suggestions derived from state.name
- suggestions merged into state

# Architecture Direction

We follow a unidirectional data flow:

UI → Event → Reducer → State → (combine with external flows) → UI

- Reducer is the ONLY place where state changes
- External flows enrich state but do not replace it
- ViewModel must be deterministic
- Prefer the shared `core/ui/architecture` contracts/helper for the `onEvent -> reduce -> produce`
  pipeline; do not build a generic framework for the feature-specific `combine(...)` layer

Goal: eliminate hidden state, implicit sync, and lifecycle-driven logic

# Custom Lint Rules You Must Respect
- `:lint-rules` enforces two compile-time rules:
  - No passing `UiText` directly to Compose `Text` (`UiTextInTextComposableDetector`).
  - No passing `UiText` directly to `stringResource`/`pluralStringResource` (`UiTextInStringResourceDetector`).
- `:app` wires these checks through `lintChecks(project(":lint-rules"))` in `app/build.gradle.kts`.

# Security + Auth Details
- Credentials are currently stored encrypted in DataStore via Tink (`core/security/EncryptedPreferencesDataStore.kt`, `TinkAeadProvider.kt`).
- `OkHttpModule` always installs bearer, user-agent, and auth interceptors; bearer token comes from `BuildConfig.BGG_TOKEN`.
- `app/build.gradle.kts` reads token from `local.properties` (`bgg.bearer.token`) or env `BGG_BEARER_TOKEN`; release builds fail if missing.

# Testing + Verification Workflow
- Unit + lint baseline (matches CI):
  - `./gradlew testDebugUnitTest :lint-rules:test`
  - `./gradlew lint`
- Instrumented path used in CI when Android-affecting files change:
  - `./gradlew connectedDebugAndroidTest`
- For sync worker changes, it is fine to run targeted connected tests with
  `-Pandroid.testInstrumentationRunnerArguments.class=...` for the affected worker test classes
  before falling back to the full `connectedDebugAndroidTest` suite.
- For Android emulator/device UI automation, prefer **Maestro** (`maestro` CLI, Maestro MCP, or
  Maestro YAML flows) for app launch/stop, semantic interaction, hierarchy inspection,
  screenshots, and repeatable end-to-end flows.
- Keep normal Gradle commands for build/lint/unit/instrumented verification; do not recreate the
  deleted custom emulator helper scripts for those tasks.
- Test placement and style in this repo:
  - Pure Kotlin/domain/viewmodel: `app/src/test` (fake-first, Turbine for flow assertions).
  - Room/Compose/Hilt/instrumented: `app/src/androidTest`.
- Screenshot testing is not wired into the repo today. If screenshot guidance is needed, treat **Paparazzi** as the intended first choice once added; mention **Roborazzi** only as an optional future user decision for a small number of critical screens, never as an assumed dependency.
- CI definitions to mirror locally: `.github/workflows/verify-unit.yml`, `.github/workflows/verify-full.yml`, `.github/workflows/master-build.yml`.

# Agent Working Agreement For This Repo
- Read `progress.md` before changes; append learnings after changes (do not rewrite prior entries).
- Do not change dependency/library versions unless explicitly requested.
- Prefer existing patterns over introducing new architecture variants; copy nearby feature/test structure first.

# AI Learnings

## Progress Report Format
After completing each PR, you must document your learnings to help future iterations. This is critical for maintaining and improving the codebase over time.

APPEND to progress.md (never replace, always append) AT THE END OF THE FILE with the following format:

```
## [Date/Time]
PR Link: <link_here>
- What was implemented
- Files changed
- **Learnings for future iterations:**
    - Patterns discovered (e.g., "this codebase uses X for Y")
    - Gotchas encountered (e.g., "don't forget to update Z when changing W")
    - Useful context (e.g., "the evaluation panel is in component X")
---
```

The learnings section is critical - it helps future iterations avoid repeating mistakes and understand the codebase better.

## Consolidate Patterns
If you discover a **reusable pattern** that future iterations should know, add it to the `## Codebase Patterns` section at the TOP of progress.txt (create it if it doesn't exist). This section should consolidate the most important learnings:

```
## Codebase Patterns
- Example: Use `sql<number>` template for aggregations
- Example: Always use `IF NOT EXISTS` for migrations
- Example: Make Compose Previews for all new UI components light and dark mode
- Example: Use Turbine when testing StateFlows in ViewModels
- Example: Use Fake implementations for repositories in unit tests instead of Mockks
- Example: Use Retrofit with Simple XML converter for BGG API calls
```

Only add patterns that are **general and reusable**, not story-specific details.

## Update AGENTS.md Files
Before committing, check if any edited files have learnings worth preserving in nearby AGENTS.md files:

1. **Identify directories with edited files** - Look at which directories you modified
2. **Check for existing AGENTS.md** - Look for AGENTS.md in those directories or parent directories
3. **Add valuable learnings** - If you discovered something future developers/agents should know:
  - API patterns or conventions specific to that module
  - Gotchas or non-obvious requirements
  - Dependencies between files
  - Testing approaches for that area
  - Configuration or environment requirements

**Examples of good AGENTS.md additions:**
- "When modifying X, also update Y to keep them in sync"
- "This module uses pattern Z for all API calls"
- "Tests require the dev server running on PORT 3000"
- "Field names must match the template exactly"

**Do NOT add:**
- Story-specific implementation details
- Temporary debugging notes
- Information already in progress.txt

Only update AGENTS.md if you have **genuinely reusable knowledge** that would help future work in that directory.
