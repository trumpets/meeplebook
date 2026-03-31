# Background Sync Plan — WorkManager Integration

## Context

The app is offline-first. Plays are written locally with `PlaySyncStatus.PENDING` the moment the user saves them. Collection and plays can be pulled from BGG on demand (sync use cases already exist). What is missing:

- A **scheduler** that runs pulls periodically without user interaction
- A **push mechanism** that uploads locally-created plays to BGG
- **User-configurable settings** for sync behaviour
- **Observability** so the user can see what is happening

This plan is organised into six phases. Each phase is independent enough to be implemented and reviewed separately.

---

## Phase 1 — WorkManager Foundation

**Goal:** Wire WorkManager into the project with Hilt injection so that future workers can receive their dependencies automatically.

### 1.1 Dependencies

Add to `gradle/libs.versions.toml` and `app/build.gradle.kts`:

| Library | Role |
|---|---|
| `androidx.work:work-runtime-ktx` | WorkManager core |
| `androidx.hilt:hilt-work` | `@HiltWorker` / `@AssistedInject` support |
| `androidx.hilt:hilt-compiler` (ksp) | Generates worker factory |

The `hiltCompose` version alias already exists; `hilt-work` uses the same group and version.

### 1.2 Hilt WorkerFactory

- Create `core/work/WorkManagerModule.kt` — a `@Module` that provides and configures the `HiltWorkerFactory` as the `WorkManager` configuration (implement `Configuration.Provider` in `App.kt`).

### 1.3 SyncManager

Create `core/work/SyncManager.kt` — a singleton `@Inject`-able class responsible for:

- `schedulePeriodicSync()` — enqueues all periodic workers using `ExistingPeriodicWorkPolicy.UPDATE` so rescheduling is idempotent.
- `schedulePushPendingPlays()` — enqueues a one-time push worker.
- `cancelAll()` — cancels all sync work (used on logout).

`SyncManager` is the **only** place that calls `WorkManager.enqueue*`. The rest of the app calls `SyncManager`.

### 1.4 Lifecycle hooks

- Call `syncManager.schedulePeriodicSync()` after a successful login (in `LoginViewModel` or `AuthRepository`).
- Call `syncManager.cancelAll()` on logout.
- Re-schedule on app upgrade: call `schedulePeriodicSync()` in `App.onCreate` if the user is already logged in.

### Tests

- Unit test `SyncManager` with a `TestWorkManager` stub (or `WorkManagerTestInitHelper`).

---

## Phase 2 — Pull Sync Workers

**Goal:** Implement periodic pull of collection and plays using the existing use cases.

### Workers

All workers are `@HiltWorker` classes in `core/work/workers/`:

| Worker | Use Case Called | Periodic Interval |
|---|---|---|
| `SyncCollectionWorker` | `SyncCollectionUseCase` | configurable (default 24 h) |
| `SyncPlaysWorker` | `SyncPlaysUseCase` | configurable (default 24 h) |
| `SyncAllWorker` | `SyncUserDataUseCase` | configurable (default 24 h) |

All workers:
- Require `NetworkType.CONNECTED` (upgrade to `UNMETERED` if the user enables wifi-only sync in Phase 3).
- Return `Result.retry()` on network failure / BGG 5xx so WorkManager back-off handles retries.
- Return `Result.failure()` only for unrecoverable errors (not logged in, etc.).

The default **daily sync** uses `SyncAllWorker` with a `PeriodicWorkRequest` of 24 hours minimum flex.

### Constraints

- `NetworkType.CONNECTED` always required.
- Optional `requiresCharging` and `requiresBatteryNotLow` (exposed as settings later).

### Tests

- Unit tests using `TestListenableWorkerBuilder` for each worker, injecting `Fake*` use cases.
- Verify `Result.success()` when the use case returns `AppResult.Success`.
- Verify `Result.retry()` when the use case returns a network failure.
- Verify `Result.failure()` when the user is not logged in.

---

## Phase 3 — Sync Preferences

**Goal:** Allow users to control sync behaviour through a settings screen. WorkManager constraints are updated whenever preferences change.

### 3.1 Data Model

Create `core/preferences/` (new package):

- `SyncPreferences` — data class:
  - `autoSyncEnabled: Boolean` (default `true`)
  - `syncIntervalHours: Int` (default `24`; options: 6, 12, 24, 48)
  - `wifiOnlySync: Boolean` (default `false`)
  - `pushOnWifiOnly: Boolean` (default `false`)
  - `requireCharging: Boolean` (default `false`)

- `SyncPreferencesRepository` (interface) + `SyncPreferencesRepositoryImpl` — backed by the existing plain (non-encrypted) DataStore (`meeplebook_prefs`), same DataStore instance already used for sync timestamps.

### 3.2 Reactive Rescheduling

`SyncManager` observes `SyncPreferencesRepository.observe()` via a coroutine in `App.onCreate` (or in a ViewModel that lives as long as the app). Whenever preferences change, `schedulePeriodicSync()` is called with the new constraints — WorkManager's `UPDATE` policy replaces the existing work chain transparently.

### 3.3 Settings UI

Add a **Settings** section to the existing Profile screen (or a dedicated Settings screen accessible from Profile):

- Toggle: *Auto-sync* (maps to `autoSyncEnabled`)
- Dropdown/Segmented: *Sync interval* — 6 h / 12 h / 24 h / 48 h
- Toggle: *Wi-Fi only for sync* (maps to `wifiOnlySync`)
- Toggle: *Wi-Fi only for pushing plays* (maps to `pushOnWifiOnly`)
- Read-only: *Last synced* — rendered from `ObserveLastFullSyncUseCase`
- Button: *Sync now* — calls `SyncManager.scheduleImmediateSync()` (one-time worker with the same constraints as periodic but run immediately)

Use string resources for all labels (existing app convention). Follow EU date format for "Last synced" timestamp.

### Tests

- Unit test `SyncPreferencesRepositoryImpl` with a fake DataStore.
- Unit test `SyncManager` reactive rescheduling by simulating preference changes.

---

## Phase 4 — Push Pending Plays to BGG

**Goal:** Upload plays that were saved locally (PENDING) to BGG.

This is the most significant phase because it also requires new network infrastructure.

### 4.1 BGG API — Save Play

BGG XML API2 exposes a play-save endpoint:

```
POST https://api.geekdo.com/xmlapi2/play
```

Form fields: `objectid`, `objecttype=thing`, `playdate`, `comments`, `length`, `location`, `quantity`, `action=save`, `id` (0 for new plays), and an XML-formatted `players` field.

Add to `BggApi.kt`:

```kotlin
@FormUrlEncoded
@POST("xmlapi2/play")
suspend fun savePlay(
    @Field("objectid") gameId: Long,
    @Field("objecttype") objectType: String = "thing",
    @Field("playdate") playDate: String,   // "YYYY-MM-DD"
    @Field("comments") comments: String?,
    @Field("length") lengthMinutes: Int,
    @Field("location") location: String?,
    @Field("quantity") quantity: Int,
    @Field("action") action: String = "save",
    @Field("id") id: Long = 0,             // 0 = new play
    @Field("players") playersXml: String?  // XML encoded
): Response<ResponseBody>
```

Authentication is already handled by `OkHttpModule`'s auth interceptors (bearer token + cookie).

The response is XML that includes the assigned BGG play ID. Parse this to extract the `remoteId`.

### 4.2 Local Infrastructure

Add to `PlayDao` / `PlaysLocalDataSource`:

- `getPendingPlays(): List<Play>` — queries `WHERE syncStatus = 'PENDING'`.
- `updateSyncStatus(localId: Long, status: PlaySyncStatus, remoteId: Long?)` — sets status and remoteId after a successful push.
- `getFailedPlays(): List<Play>` — for surfacing in the UI.

### 4.3 Remote Data Source

Add `pushPlay(play: Play): Long` to `PlaysRemoteDataSource` / `PlaysRemoteDataSourceImpl`:

- Converts `Play` to form fields + players XML.
- Calls `BggApi.savePlay(...)`.
- Parses the response to extract the BGG-assigned `remoteId`.
- Reuses the existing `retryWithBackoff` pattern for 202/429/5xx.

### 4.4 Use Case

Create `core/plays/domain/PushPendingPlaysUseCase.kt`:

1. Requires logged-in user (return early if not).
2. Fetches `getPendingPlays()`.
3. For each pending play:
   - Call `pushPlay(play)`.
   - On success: update `syncStatus = SYNCED`, set `remoteId`.
   - On failure: update `syncStatus = FAILED` (will be retried next push cycle).
4. Return count of successfully pushed plays.

### 4.5 Worker

`PushPendingPlaysWorker` in `core/work/workers/`:

- One-time worker.
- Network required (plus `requiresUnmetered` if `pushOnWifiOnly` is set).
- Enqueued by `SyncManager.schedulePushPendingPlays()`.
- Called from `PlaysRepositoryImpl.createPlay()` after local insert (fire-and-forget enqueue).
- Also enqueued periodically alongside the pull workers (so failed plays are retried).

### 4.6 Status Handling

`PlaySyncStatus.FAILED` plays:

- Displayed with a warning indicator in the Plays list (small badge/icon).
- A manual *Retry* action available from the play detail or a global *Retry failed plays* button in Settings.
- Automatically retried on the next push cycle.

### Tests

- Unit test `PushPendingPlaysUseCase` with fake repo returning various PENDING/FAILED combinations.
- Unit test `PushPendingPlaysWorker` with `TestListenableWorkerBuilder`.
- Unit test BGG response XML parsing for play ID extraction.
- Instrumented test for `PlayDao.getPendingPlays()` + `updateSyncStatus()`.

---

## Phase 5 — Edit Play (Future)

When play editing is added, the push mechanism extends naturally:

- A local edit sets `syncStatus = PENDING` and preserves the existing `remoteId`.
- `PushPendingPlaysUseCase` detects a non-null `remoteId` with `PENDING` status and calls `BggApi.savePlay(id = remoteId)` instead of `id = 0`.
- No new infrastructure needed — just the edit UI and the `PlaysRepository.editPlay(command)` method.

Delete is similar: a `PlaySyncStatus.PENDING_DELETE` state would be added, and the push use case calls `action=delete`.

---

## Phase 6 — Observability

**Goal:** Surface sync state to the user so they always know what is happening.

### Sync Status Indicator

- In the Home/Overview screen: a small *"Syncing…"* indicator when a sync worker is actively running (observe WorkManager `WorkInfo` by tag).
- Or: a pull-to-refresh gesture on Collection and Plays screens that triggers an immediate sync.

### Pending / Failed Plays Count

- Add `observePendingPlaysCount(): Flow<Long>` and `observeFailedPlaysCount(): Flow<Long>` to `PlaysRepository`.
- Show a badge on the Plays tab / list item when there are pending or failed plays.

### Last Synced Timestamp

- Already available via `ObserveLastFullSyncUseCase`.
- Display in the Settings/Profile screen in EU format (`dd/MM/yyyy HH:mm`).

---

## Implementation Order

1. **Phase 1** — foundation (no business value yet, but unblocks everything)
2. **Phase 2** — daily pull (immediate user value: automatic refresh)
3. **Phase 3** — settings (users can tune behaviour)
4. **Phase 4** — push pending plays (completes the offline-first loop)
5. **Phase 6** — observability (polish)
6. **Phase 5** — edit/delete plays (future, after edit UI exists)

---

## Open Questions

1. **BGG Bearer token for write operations** — the current token (`BGG_BEARER_TOKEN`) is used for read requests. Confirm it authorises `POST /xmlapi2/play` as well, or whether a different auth flow (e.g. OAuth, cookie session) is required for write operations.
2. **Players XML format** — the exact XML schema for the `players` field in the BGG save-play endpoint needs to be confirmed from API docs or reverse-engineered from the BGG website network traffic.
3. **Sync frequency defaults** — 24 h is the proposed default. Consider whether 12 h is more appropriate given that BGG play data changes frequently.
4. **Conflict resolution** — if a play exists locally as PENDING but was also created on BGG.com directly, the next pull will create a duplicate. A de-duplication strategy is needed (match on date + gameId + duration, or accept duplicates and let the user resolve).
5. **WorkManager in tests** — confirm CI can run WorkManager-dependent instrumented tests (`connectedDebugAndroidTest`) without changes to the test runner configuration.
