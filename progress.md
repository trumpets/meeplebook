## Codebase Patterns
- For reducer-driven screens, use the shared `core/ui/architecture` contracts/helper for `onEvent -> reduce -> produce -> handle effects`, but keep feature-owned base state, query flows, and `combine(baseState, externalData) -> uiState` mapping local to the feature.
- For simple reducer-driven forms with no external observed data, expose reducer-owned state directly as `uiState` and use one-shot `UiEffect`s for success navigation instead of persistent success flags.
- If a one-shot UI effect depends on derived render data that exists only in final `uiState`, emit a domain effect and resolve it in the ViewModel against the latest derived `uiState` rather than duplicating the data into base state or reading captured UI state in Compose.
- Sync use cases are the worker-facing/auth-gated orchestration boundary; repositories remain responsible for remote sync business logic and local persistence.
- Keep BGG wire dates on `yyyy-MM-dd` and UI dates on `dd/MM/yyyy`; do not reuse the EU UI formatter for remote XML parsing or serialization.
- Keep WorkManager workers in `core/sync/work` as thin `@HiltWorker` `CoroutineWorker`s that delegate to sync use cases/repositories, use `HiltWorkerFactory` from `MeepleBookApp`, retry only retryable network failures, fail on max-retries-exceeded, and treat logged-out runs as no-op success.
- Manual refresh and sync-status UI in Overview/Collection/Plays should go through `SyncManager` and observed persisted `SyncState`; do not drive those screens from direct sync use-case results or local refresh jobs.

## 2026-01-29T20:05:00Z
PR Link: https://github.com/trumpets/meeplebook/pull/71
- Extended `FakePlaysRepository` to implement missing `observeUniqueGamesCount()` method with test helper
- Created comprehensive test suite `ObservePlayStatsUseCaseTest` with 8 test cases covering aggregation, empty state, year calculation, and flow reactivity
- Files changed:
  - `app/src/test/java/app/meeplebook/core/plays/FakePlaysRepository.kt`
  - `app/src/test/java/app/meeplebook/core/plays/domain/ObservePlayStatsUseCaseTest.kt` (new)
- **Learnings for future iterations:**
    - Test use cases follow pattern: use `FakePlaysRepository` for isolation and fixed `Clock` for deterministic time-based testing
    - When adding repository methods, corresponding setter methods should be added to `FakePlaysRepository` for test control
    - Test suites should cover: normal operation, empty state, reactivity to data changes, and edge cases like different time periods
    - Fake repositories should compute derived values (like unique games count) in `updateComputedValues` to keep all counters consistent when `setPlays` is called
---

## 2026-01-29T22:20:00Z
PR Link: Sub-PR for https://github.com/trumpets/meeplebook/pull/69 (addressing review comment #2743783872)
- Created comprehensive test suite `ObservePlaysUseCaseTest` with 8 test cases covering query passthrough, mapping correctness, and flow reactivity
- Tests verify: null/blank query forwarding, non-blank query forwarding, Play->DomainPlayItem mapping, empty state, and reactive updates
- Files changed:
  - `app/src/test/java/app/meeplebook/core/plays/domain/ObservePlaysUseCaseTest.kt` (new)
- **Learnings for future iterations:**
    - Test use cases follow pattern: use `FakePlaysRepository` for isolation, `PlayTestFactory` for creating test data
    - FakePlaysRepository tracks `lastObservePlaysQuery` to verify query forwarding behavior
    - Test mapping by creating plays with full details (players, location, comments) and verifying all fields in DomainPlayItem
    - Test cases should cover: null/blank/non-blank query, empty list, mapping correctness, and flow reactivity
---
## 2026-01-29T22:40:00Z
PR Link: Sub-PR for https://github.com/trumpets/meeplebook/pull/69 (addressing review comments on PR #72)
- Addressed code review feedback on `ObservePlaysUseCaseTest`
- Enhanced player mapping test with comprehensive assertions for all fields (`startPosition`, `score`, `win`)
- Refactored flow reactivity test to use Turbine for true reactive testing (single collector receives multiple emissions)
- Updated boolean assertions to use idiomatic `assertTrue`/`assertFalse` instead of `assertEquals`
- Improved blank query test to verify behavior (returns all plays) instead of implementation details
- Fixed progress.md ordering to follow chronological append-only pattern
- Files changed:
  - `app/src/test/java/app/meeplebook/core/plays/domain/ObservePlaysUseCaseTest.kt` (improved)
  - `progress.md` (reordered)
- **Learnings for future iterations:**
    - When testing domain mapping, assert ALL fields to catch regressions, not just a subset
    - Use Turbine's `.test {}` to verify true flow reactivity (single collector getting multiple emissions)
    - Use `assertTrue`/`assertFalse` for boolean assertions instead of `assertEquals(true/false, ...)`
    - True reactivity testing requires observing the flow once and verifying subsequent emissions, not re-subscribing
    - Test behavior (what the function returns) rather than implementation details (what gets passed to dependencies)
    - Always append to progress.md, never prepend - maintain chronological order
---
## 2026-01-29T23:05:00Z
PR Link: Sub-PR for https://github.com/trumpets/meeplebook/pull/69 (addressing review comment #2743896458)
- Created comprehensive test suite `ObservePlaysScreenDataUseCaseTest` with 9 test cases
- Tests cover: empty state, non-empty grouping by month/year, stats combination, flow reactivity (plays and stats independently), query forwarding, and edge cases
- Verified tests follow established patterns from `ObserveCollectionDomainSectionsUseCaseTest` and `ObservePlaysUseCaseTest`
- Files changed:
  - `app/src/test/java/app/meeplebook/feature/plays/domain/ObservePlaysScreenDataUseCaseTest.kt` (new)
- **Learnings for future iterations:**
    - Test use cases that combine multiple flows follow pattern: inject all dependencies (use cases and repositories), use fixed Clock for time-based testing
    - When testing combined flows, verify both data streams independently update correctly using Turbine
    - Test section-based use cases should cover: empty state, grouping logic, section ordering, data preservation within sections
    - Always test reactive updates for use cases that combine flows - verify each source flow triggers updates independently
    - Test coverage should match feedback requests: empty, non-empty grouping, and reactivity
---
## 2026-01-29T23:07:00Z
PR Link: Sub-PR for https://github.com/trumpets/meeplebook/pull/69 (addressing review comment #2743896325)
- Created comprehensive test suite `BuildPlaysSectionsUseCaseTest` with 11 test cases covering grouping/sorting behavior
- Tests verify: YearMonth grouping, reverse chronological order (most recent first), stable in-section ordering
- Test coverage includes: empty list, single/multiple month grouping, year boundaries, same-day plays, edge cases (same instant, far past/future dates), full year of monthly plays
- Files changed:
  - `app/src/test/java/app/meeplebook/feature/plays/domain/BuildPlaysSectionsUseCaseTest.kt` (new)
- **Learnings for future iterations:**
    - Plays section tests follow same pattern as collection section tests but with YearMonth grouping and reverse chronological ordering
    - Use `PlayTestFactory.createPlay()` and `.toDomainPlayItem()` for creating test data
    - Test suites should verify: grouping logic, sort order (reverse chronological for plays), within-section ordering stability
    - Edge cases to test: empty list, single item, all items in one section, year boundaries, same instant, far past/future dates
    - Tests structured with Given/When/Then comments for clarity
---

## 2026-01-29T23:42:00Z
PR Link: Sub-PR for https://github.com/trumpets/meeplebook/pull/74 (addressing review comment #2743934926)
- Fixed flaky test in `ObservePlaysScreenDataUseCaseTest.invoke updates when plays change`
- Issue: `setPlays()` updates multiple StateFlows (_plays, _totalPlaysCount, _uniqueGamesCount), causing intermediate emissions in combined flow
- Solution: consume emissions in a loop until reaching expected stable state (sections.size == 2 && uniqueGamesCount == 2L)
- Verified other reactive test already follows correct pattern (updating one flow at a time)
- Files changed:
  - `app/src/test/java/app/meeplebook/feature/plays/domain/ObservePlaysScreenDataUseCaseTest.kt`
- **Learnings for future iterations:**
    - When testing combined flows where upstream updates multiple StateFlows at once, expect multiple intermediate emissions
    - Use a do-while loop to consume emissions until reaching the expected stable state
    - Check both conditions that define the stable state (e.g., sections.size AND stats values)
    - Alternative approach: update one flow at a time and assert each emission (as done in `invoke updates when stats change independently` test)
    - Combined flows with multiple upstream StateFlows are inherently prone to intermediate emissions - tests must account for this
---

## 2026-02-05T00:10:00Z
PR Link: Sub-PR for https://github.com/trumpets/meeplebook/pull/69 (addressing review comment #2748161760)
- Added comprehensive DAO-level test suite for `observeUniqueGamesCount()` method in PlayDaoTest
- Tests cover: empty database (0 count), single game with multiple plays (1 count), multiple distinct games with duplicates (correct distinct count)
- Tests verify reactive behavior: updates on insert (new game increases count, duplicate game doesn't), updates on delete (count goes to 0)
- Files changed:
  - `app/src/androidTest/java/app/meeplebook/core/database/dao/PlayDaoTest.kt` (5 new tests added)
- **Learnings for future iterations:**
  - DAO tests follow pattern: use in-memory Room database with `runTest` and `Flow.first()` for synchronous assertions
  - Test COUNT(DISTINCT ...) queries should verify: empty state, single distinct value with duplicates, multiple distinct values, reactive updates
  - Reactive Flow tests should check both insert and delete operations trigger emissions
  - Use existing helper functions (createTestPlay) for consistency in test data creation
  - DAO tests are instrumented tests (androidTest directory) and require Android environment to run
---

## 2026-02-05T00:11:00Z
PR Link: Sub-PR for https://github.com/trumpets/meeplebook/pull/69 (addressing review comment #2748350390)
- Added comprehensive instrumented DAO tests for `observePlaysWithPlayersByGameNameOrLocation` query method
- Created 9 test cases covering: game name matching, location matching, combined matching, ordering (date DESC), case-insensitive behavior, players inclusion, and edge cases
- Tests verify partial string matching using LIKE operator, null location handling, empty results, and proper @Transaction behavior
- Files changed:
  - `app/src/androidTest/java/app/meeplebook/core/database/dao/PlayDaoTest.kt` (+206 lines)
- **Learnings for future iterations:**
  - SQLite LIKE operator is case-insensitive by default for ASCII characters
  - Room @Transaction queries should be tested to ensure relations are properly loaded
  - DAO tests should cover: positive matches, negative matches (empty results), ordering, null field handling, and edge cases
  - Case-insensitive tests should verify both uppercase and lowercase variants match
  - When testing search queries with OR conditions, test each condition independently and combined
  - Follow existing test patterns in the file (helper methods, test structure, assertion style)
---

## 2026-02-05T00:20:00Z
PR Link: Sub-PR #80 for https://github.com/trumpets/meeplebook/pull/82 (addressing retry request)
- Fixed `ObservePlayStatsUseCase.observeCurrentYear()` infinite loop causing UncompletedCoroutinesError in all PlaysViewModelTest tests (16 failures)
- Fixed `SearchableFlow` debounce implementation to resolve timing issues in SearchableFlowTest (4 failures)
- Changes:
    - `ObservePlayStatsUseCase.kt`: Removed `while(true)` loop, year is now not flow
    - `SearchableFlow.kt`: Simplified to use `debounce { }` lambda with conditional duration (0ms for empty, specified for non-empty queries)
- Files changed:
    - `app/src/main/java/app/meeplebook/core/plays/domain/ObservePlayStatsUseCase.kt`
    - `app/src/main/java/app/meeplebook/core/ui/flow/SearchableFlow.kt`
- **Learnings for future iterations:**
    - Avoid `while(true)` loops in Flow builders - they prevent coroutines from completing and cause UncompletedCoroutinesError in runTest
    - When conditionally applying debounce, use `debounce { duration }` lambda instead of nested flatMapLatest to avoid timing issues
    - Test coroutines must complete within runTest timeout - any infinite loops or uncompleted coroutines cause failures
---

## 2026-02-05T10:20:00Z
PR Link: Sub-PR for https://github.com/trumpets/meeplebook/pull/69 (addressing review comment #2768119933)
- Added comprehensive test suite for `PlaysRepositoryImpl.observePlays()` method covering query-dependent behavior
- Added 8 new test cases covering: null query, empty query, blank query, filtering by game name, filtering by location, case-insensitive filtering, whitespace trimming, and no matches scenario
- Tests verify correct delegation to `local.observePlays()` for null/empty/blank queries and to `local.observePlaysByGameNameOrLocation()` for non-blank queries
- Files changed:
  - `app/src/test/java/app/meeplebook/core/plays/PlaysRepositoryImplTest.kt` (added 8 tests, +100 lines)
- **Learnings for future iterations:**
  - When a repository method has branching logic (e.g., query-dependent behavior), test all branches thoroughly
  - Test edge cases like null, empty string, blank string (whitespace-only) to verify proper handling
  - Test both positive cases (matching results) and negative cases (no matches)
  - Verify string transformation behavior (e.g., trimming) is working correctly
  - Use descriptive test data (e.g., "Catan", "Azul", "Game Store") to make tests more readable than generic values
---

## 2026-02-05T22:56:00Z
PR Link: https://github.com/trumpets/meeplebook/pull/86 (PlaysScreen implementation)
- Implemented complete PlaysScreen UI following CollectionScreen and OverviewScreen patterns
- Added ScreenPadding object to UIConstants.kt for standardized dimensions across screens
- Added string resources for plays screen (loading, search placeholder, stats labels, section count plurals)
- Created comprehensive UI test suite PlaysScreenRootTest with 16 test cases
- Files changed:
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysScreen.kt` (complete rewrite)
  - `app/src/main/java/app/meeplebook/ui/components/UIConstants.kt` (added ScreenPadding)
  - `app/src/main/res/values/strings.xml` (added plays screen strings)
  - `app/src/androidTest/java/app/meeplebook/feature/plays/PlaysScreenRootTest.kt` (new)
- **Learnings for future iterations:**
  - PlaysScreen uses unique naming: PlaysScreenRoot, PlaysScaffoldLayout, PlaysStatsDisplay, MonthSectionHeader, PlayItemCard, SyncStatusBadge
  - ScreenPadding object centralizes dimension constants for maintainability (Horizontal, SectionSpacing, ContentPadding, ItemSpacing, CardInternal, Small)
  - Sync status display uses colored circular badges: CheckCircle/green for SYNCED, Schedule/orange for PENDING, Error/red for FAILED
  - Month section headers show formatted month/year on left and play count (pluralized) on right with divider below
  - UI tests should cover: all states (Loading, Empty variants, Error, Content), component visibility, user interactions (search, card taps), and content display
  - Use testTag for key UI elements: playsScreen, playsLoadingIndicator, playsStatsCard, playsSearchInput, playsEmptyState, playsErrorState, playsListContent, playCard_{id}, monthHeader_{yearMonth}
---
## 2026-02-11T10:20:00Z
PR Link: Sub-PR for https://github.com/trumpets/meeplebook/pull/87 (addressing review comment #2792466918)
- Implemented `FakePlaysRepository.createPlay()` method to replace TODO placeholder
- Implementation follows same pattern as `FakePlaysLocalDataSource.insertPlay()`
- Uses `maxOfOrNull` for ID generation (more robust than size+1, handles arbitrary test data from setPlays)
- Maps CreatePlayCommand to Play with PlayId.Local, syncStatus=PENDING, and proper player mapping
- Updates internal _plays state and calls updateComputedValues() to maintain derived flow consistency
- Added missing imports: PlayId, PlaySyncStatus, Player
- Files changed:
  - `app/src/test/java/app/meeplebook/core/plays/FakePlaysRepository.kt` (+38 lines, removed TODO)
- **Learnings for future iterations:**
  - Fake repository methods that modify state must: update backing StateFlow, call updateComputedValues() to keep derived flows consistent
  - Use `(maxOfOrNull { it.playId.localId } ?: 0L) + 1L` for test ID generation instead of size+1 to handle arbitrary setPlays() test data
  - Player IDs follow pattern `playId * 100 + index` (matches FakePlaysLocalDataSource pattern, acceptable for tests)
  - New plays created via createPlay() should have syncStatus=PENDING (not SYNCED)
  - When implementing repository interface methods in fakes, match the behavior of the real implementation (PlaysRepositoryImpl.createPlay)
---
## 2026-02-11T10:47:00Z
PR Link: Sub-PR for https://github.com/trumpets/meeplebook/pull/87 (addressing review comment #3883642136)
- Fixed `FakePlaysLocalDataSource` local ID generation to use `maxOfOrNull` instead of `size + 1`
- Updated `PlayTestFactory.kt` KDoc to reference `localPlayId` instead of incorrect `id` parameter
- Changes:
    - `FakePlaysLocalDataSource.saveRemotePlays()`: Changed from `existingPlays.size + 1L` to `(existingPlays.maxOfOrNull { it.playId.localId } ?: 0L) + 1L`
    - `FakePlaysLocalDataSource.insertPlay()`: Changed from `existingPlays.size + 1L` to `(existingPlays.maxOfOrNull { it.playId.localId } ?: 0L) + 1L`
    - `PlayTestFactory.createPlay()`: Updated KDoc from "defaults to id * 100" to "defaults to localPlayId * 100"
- Files changed:
  - `app/src/test/java/app/meeplebook/core/plays/local/FakePlaysLocalDataSource.kt`
  - `app/src/test/java/app/meeplebook/core/plays/PlayTestFactory.kt`
- **Learnings for future iterations:**
  - Use `maxOfOrNull { it.playId.localId } ?: 0L` for local ID generation in test fakes to handle non-contiguous IDs from `setPlays()`
  - This pattern is consistent with `FakePlaysRepository.createPlay()` and prevents duplicate IDs when tests seed arbitrary test data
  - Always keep KDoc in sync with actual parameter names and implementation behavior
  - When `setPlays()` is called with arbitrary/non-contiguous localIds, `size + 1` can create duplicate IDs
---
## 2026-02-11T12:22:00Z
PR Link: Sub-PR for https://github.com/trumpets/meeplebook/pull/87 (addressing review comment #2792993063)
- Added comprehensive test suite for `PlaysRepositoryImpl.createPlay()` method with 9 test cases
- Tests verify: play inserted with `remoteId = null` (PlayId.Local), `syncStatus = PENDING`, correct player fields persistence/linking, observability via `getPlays()` and `observePlays()`, edge cases (null optional fields, multiple plays)
- Added helper functions `createPlayCommand()` and `createPlayerCommand()` for concise test data creation following existing patterns
- Fixed boolean assertions to use `assertFalse()` instead of `assertEquals(false, ...)` for consistency
- Files changed:
  - `app/src/test/java/app/meeplebook/core/plays/PlaysRepositoryImplTest.kt` (+245 lines, 9 new tests + 2 helper functions)
- **Learnings for future iterations:**
  - Repository method tests should verify all key properties: ID structure (Local vs Remote), sync status, field persistence, relationship linking, and observability
  - Test helper functions follow pattern: required params first, optional params with sensible defaults
  - Use `assertTrue/assertFalse` for boolean assertions instead of `assertEquals(true/false, ...)` for better readability and consistency
  - When testing `createPlay()`, verify both immediate retrieval (`getPlays()`) and reactive retrieval (`observePlays()`)
  - Test all player fields when verifying player persistence: name, username, userId, score, win, startPosition, color
  - Edge case testing should cover: null optional fields, empty collections, multiple insertions
---
## 2026-02-11T21:53:00Z
PR Link: Sub-PR for https://github.com/trumpets/meeplebook/pull/87 (addressing comment #3887424137)
- Added comprehensive test suite for newly added location-related repository methods: `observeLocations(query)` and `observeRecentLocations()`
- Created 14 new test cases covering all aspects of location queries:
  - `observeLocations(query)`: 8 tests covering query matching, case-insensitive filtering, distinct results, alphabetical sorting, limit of 10, no matches, null filtering, empty state
  - `observeRecentLocations()`: 6 tests covering ordering by most recent date, distinct results, limit of 10, null filtering, empty state, all-null locations
- Tests follow established patterns from DAO tests and existing repository tests
- Files changed:
  - `app/src/test/java/app/meeplebook/core/plays/PlaysRepositoryImplTest.kt` (+205 lines, 14 new tests)
- **Learnings for future iterations:**
  - Location query tests should verify: case-insensitive matching, distinct case-preserving results (deduplicated by lowercase), sorting behavior (alphabetical or date-based), result limits, null filtering
  - `observeLocations()` returns locations that start with query string, sorted alphabetically (case-insensitive), limited to 10 results
  - `observeRecentLocations()` returns locations ordered by most recent play date (DESC), deduplicated by lowercase, limited to 10 results
  - When testing methods that filter/sort/limit collections, cover: normal operation with multiple items, edge cases (empty, nulls, duplicates), limits, and sorting order
  - Tests should match the behavior of underlying DAO implementations to ensure consistency
---

## 2026-02-11T22:45:00Z
PR Link: Sub-PR for https://github.com/trumpets/meeplebook/pull/87 (addressing comment #3887628361)
- Implemented `observePlayersByLocation` in `FakePlaysLocalDataSource` and `FakePlaysRepository`
- Added 4 DAO-level tests for `observePlayersByLocation` in `PlayDaoTest` covering: ordered by play count, empty list for nonexistent location, empty list when no plays exist, grouping by name+username
- Added 5 repository-level tests for `observePlayersByLocation` in `PlaysRepositoryImplTest` covering: ordered by play count, empty list for nonexistent location, empty list when no plays exist, grouping by name+username, filtering by location
- Files changed:
  - `app/src/test/java/app/meeplebook/core/plays/local/FakePlaysLocalDataSource.kt`
  - `app/src/test/java/app/meeplebook/core/plays/FakePlaysRepository.kt`
  - `app/src/androidTest/java/app/meeplebook/core/database/dao/PlayDaoTest.kt`
  - `app/src/test/java/app/meeplebook/core/plays/PlaysRepositoryImplTest.kt`
- **Learnings for future iterations:**
  - DAO `observePlayersByLocation` uses SQL GROUP BY (name, username) and MAX(userId), ordering by COUNT(*) DESC
  - Fake implementations should match SQL logic: filter by location, flatMap to players, group by (name, username), take max userId, sort by count DESC
  - Test coverage should include: normal operation with ordering, empty results (no plays, nonexistent location), grouping behavior, filtering behavior
  - SQL MAX() on nullable column returns 0 in Room/SQLite when all values are NULL; fake implementations must mirror this behavior so repository tests do not diverge from production
---

## 2026-03-28T20:02:21Z
PR Link: N/A (local change)
- Added/confirmed KDoc comments for Add Play UI state models to improve readability and maintainability
- Files changed:
  - `app/src/main/java/app/meeplebook/feature/addplay/AddPlayUiState.kt`
- **Learnings for future iterations:**
    - Keep UI model KDoc close to feature state classes so intent is clear without opening reducers/viewmodels
    - Small documentation-only updates should still be logged in `progress.md` to preserve project history
---

## 2026-03-28T20:35:00Z
PR Link: N/A (local change)
- Created a new root `AGENTS.md` to guide AI coding agents with codebase-specific architecture, workflows, conventions, and CI/test expectations
- Consolidated discoverable patterns across app entry/navigation, repository/data flow, BGG XML integration, custom lint rules, security/token handling, and testing workflow
- Files changed:
  - `AGENTS.md`
  - `progress.md`
- **Learnings for future iterations:**
    - Keep root `AGENTS.md` focused on observed implementation details with concrete file references, not aspirational architecture notes
    - Include CI-aligned verification commands (`testDebugUnitTest`, `:lint-rules:test`, `lint`, `connectedDebugAndroidTest`) so local checks match GitHub workflows
    - Call out project-specific safeguards (UiText lint rules, BGG token build config behavior, retry/rate-limit handling) because these drive common regressions
---

## 2026-03-28T20:49:00Z
PR Link: N/A (local change)
- Added 42 unit tests covering all 7 reducer classes in `app.meeplebook.feature.addplay.reducer`
- Created a shared `AddPlayTestFactory` with `makeState()`, `makePlayer()`, and `makeIdentity()` helpers to avoid boilerplate across test files
- Files changed:
  - `app/src/test/java/app/meeplebook/feature/addplay/AddPlayTestFactory.kt`
  - `app/src/test/java/app/meeplebook/feature/addplay/reducer/MetaReducerTest.kt`
  - `app/src/test/java/app/meeplebook/feature/addplay/reducer/PlayerListReducerTest.kt`
  - `app/src/test/java/app/meeplebook/feature/addplay/reducer/PlayerScoreReducerTest.kt`
  - `app/src/test/java/app/meeplebook/feature/addplay/reducer/PlayerEditReducerTest.kt`
  - `app/src/test/java/app/meeplebook/feature/addplay/reducer/PlayerColorReducerTest.kt`
  - `app/src/test/java/app/meeplebook/feature/addplay/reducer/PlayersReducerTest.kt`
  - `app/src/test/java/app/meeplebook/feature/addplay/reducer/AddPlayReducerTest.kt`
- **Learnings for future iterations:**
  - Reducers are pure functions — no mocking needed; test directly by constructing state and asserting on the returned state
  - Unit tests in `app/src/test` use `org.junit.Assert.*` (`assertEquals`, `assertTrue`, `assertFalse`, `assertNull`) — Truth/`assertThat` is NOT on the test classpath here
  - `PlayerEntryUi` uses `PlayerIdentity` as its logical key for matching; there is no separate ID field
  - `AddPlayUiState` has many required constructor fields; always use a factory helper like `AddPlayTestFactory.makeState()` in tests to keep them readable
  - Test one reducer in isolation per file; use `PlayersReducerTest` and `AddPlayReducerTest` for integration/composition coverage
---

## 2026-03-28T21:47:39Z
- Fixed two bugs in `PlaysLocalDataSourceImpl.retainByRemoteIds`:
  1. **O(n×m) filter** — replaced `List<Long>` membership check with `HashSet<Long>` via `toHashSet()` → O(n) total
  2. **SQLite 999 bind-param limit** — `toDelete` list is now chunked into 500-item batches before each `playDao.deleteByRemoteIds(chunk)` call
- Added `SYNC_CHUNK_SIZE = 500` private companion constant
- Created `PlaysLocalDataSourceImplTest` (instrumented) with 5 tests: normal retain, empty retain (all remote deleted), local-only plays never deleted, all-retained no-op, large list >1000 items verifying chunking
- Files changed:
  - `app/src/main/java/app/meeplebook/core/plays/local/PlaysLocalDataSourceImpl.kt`
  - `app/src/androidTest/java/app/meeplebook/core/plays/local/PlaysLocalDataSourceImplTest.kt` (new)
- **Learnings for future iterations:**
  - Chunking `deleteByRemoteIds` (IN :list) calls at 500-item batches is the established pattern for any bulk DAO delete; never pass an unbounded list to a Room `IN` query
  - Use `toHashSet()` when checking `!in` on a list that could be large; List membership is O(m) per check
---

## 2026-03-28T22:24:50Z
- Addressed code review: restored observable-update behaviour in `FakePlaysRepository.syncPlays()`
- Added `var syncPlaysData: List<Play>? = null` — when non-null and sync succeeds, `_plays` and all derived flows are updated (mirrors `PlaysRepositoryImpl` production behaviour)
- Default is `null` for full backwards compatibility — all 8 existing tests that set `syncPlaysResult = AppResult.Success(Unit)` continue to pass unchanged
- Created `FakePlaysRepositoryTest` with 6 tests covering: success+data updates flows, success+data updates derived flows, success+null-data leaves plays unchanged, failure+data leaves plays unchanged, result passthrough, call count/username tracking
- Files changed:
  - `app/src/test/java/app/meeplebook/core/plays/FakePlaysRepository.kt`
  - `app/src/test/java/app/meeplebook/core/plays/FakePlaysRepositoryTest.kt` (new)
- **Learnings for future iterations:**
  - Fake repositories should mirror production observable side-effects, not just return values; use opt-in nullable fields to stay backwards-compatible with existing tests
  - When a fake method has configurable results, always add a companion `*Data` property if the real impl also mutates observable state on success
---

## 2026-03-29T20:24:50Z
PR Link: N/A (local session)
- Added `ValidationReducerTest.kt` (5 tests) for the new `ValidationReducer`
- Updated `AddPlayReducerTest.kt` — 2 new `canSave` pipeline tests  
- Updated `AddPlayEffectProducerTest.kt` — removed `oldState` param, updated `RefreshPlayerSuggestions` (removed, event deleted from sealed interface), aligned `SaveClicked` tests to field-based `canSave`
- Updated `AddPlayTestFactory` — `makeState(gameId: Long?)`, `makeState(gameName: String?)`, `makePlayer(startPosition: Int = 1)` to match current non-nullable `PlayerEntryUi.startPosition`
- **Learnings for future iterations:**
  - `canSave` is now a plain `Boolean` field on `AddPlayUiState` (defaults `false`); tests must set `.copy(canSave = true)` explicitly when testing `SaveClicked` success paths
  - `AddPlayEvent.SuggestionEvent` was removed — no `RefreshPlayerSuggestions` event exists anymore
  - `AddPlayUiState.toCreatePlayCommand()` is the mapping method (not `toDomain()`); lives inside the data class
  - `AddPlayEffectProducer.produce()` no longer takes `oldState`; signature is `produce(newState, event)`
---

## 2026-03-30T00:21:50Z
PR Link: (local commit — no PR yet)
- Wrote `AddPlayViewModelTest` with 7 test cases covering: initial state, GameSearchEvent handling, location suggestion debounce, CancelClicked, SaveClicked (can't save → ShowError, saveable → NavigateBack, failure → isSaving cycle)
- Wrote 4 new use case tests: `ObserveRecentLocationsUseCaseTest`, `SearchLocationsUseCaseTest`, `ObservePlayerSuggestionsUseCaseTest`, `CreatePlayUseCaseTest`
- Added `createPlayException: Throwable?` field to `FakePlaysRepository` for configurable save failures
- All tests pass (BUILD SUCCESSFUL)
- **Learnings for future iterations:**
  - `FakePlaysRepository` uses `_plays` as the single data source for observeLocations, observeRecentLocations, observePlayersByLocation — seed with `setPlays(...)` to test all three
  - `FakePlaysRepository` is not `open`; add `var createPlayException` style flags for configurable failure instead of subclassing
  - `AddPlayViewModel` canSave=true simply requires a non-null `gameId` and non-blank `gameName` (ValidationReducer); fire `GameSelected` event to make the VM saveable in tests
  - VM test entry pattern: `Dispatchers.setMain(StandardTestDispatcher)` + `advanceUntilIdle()` after events, `runTest` scope
  - `CreatePlayCommand.date` is `Instant` (not `LocalDate`) — use `Instant.parse(...)` or `Instant.now()` in tests
---
## 2026-03-30T00:33:00Z
PR Link: (local session — AddPlayUiState sealed interface test refactor)
- Refactored all AddPlay unit tests to work with the new sealed `AddPlayUiState` interface (`GameSearch` / `GameSelected` subclasses)
- Files changed:
  - `app/src/test/java/app/meeplebook/feature/addplay/AddPlayTestFactory.kt` — replaced `makeState()` with `makeGameSelectedState()` / `makeGameSearchState()`; added `requireGameSelected()` / `requireGameSearch()` throwing cast helpers
  - `app/src/test/java/app/meeplebook/feature/addplay/reducer/MetaReducerTest.kt` — typed factories + subclass casts
  - `app/src/test/java/app/meeplebook/feature/addplay/reducer/ValidationReducerTest.kt` — removed structurally-impossible null tests; added `GameSearch passes through unchanged`
  - `app/src/test/java/app/meeplebook/feature/addplay/reducer/PlayersReducerTest.kt` — typed factories + result casts
  - `app/src/test/java/app/meeplebook/feature/addplay/reducer/AddPlayReducerTest.kt` — typed factories; replaced null-gameId canSave test with `GameSearch stays GameSearch`
  - `app/src/test/java/app/meeplebook/feature/addplay/effect/AddPlayEffectProducerTest.kt` — typed factories; `SaveClicked with GameSearch state emits no effects`
  - `app/src/test/java/app/meeplebook/feature/addplay/AddPlayViewModelTest.kt` — `uiState` → `combinedUiState`; fixed query/GameSelected tests with `awaitUiStateMatching`; fixed `SaveClicked no-game` semantics
- Result: 621 tests pass, 1 skipped to user (`isSaving` VM test — StandardTestDispatcher conflation issue)
- **Learnings for future iterations:**
    - `combinedUiState` uses `stateIn(WhileSubscribed(5_000))`. Accessing `.value` without a subscriber returns stale initial value. ALWAYS use `test {}` to subscribe before sending events to the VM.
    - `awaitUiStateMatching<S, T>(stateFlow, debounceTime, predicate)` in `TurbineExtensions.kt` is the correct pattern for asserting a specific typed state from a debounced `WhileSubscribed` stateFlow.
    - `StateFlow` conflation with `StandardTestDispatcher`: if production code updates state twice in the same coroutine tick (no suspension between updates), the intermediate state may never be observed. Add `delay(1)` in the Fake or switch to `UnconfinedTestDispatcher` to test intermediate states.
    - `asGameSelected { }` (production, `AddPlayUiState.kt`) returns `null` silently; `requireGameSelected()` / `requireGameSearch()` (test-only, `AddPlayTestFactory.kt`) throw for assertion failures. Don't mix them.
    - `GameSelected.canSave` is now `!isSaving` only (gameName non-null is structural); no need to test `canSave=false` due to null gameName — that case is impossible in `GameSelected`.
---
## 2026-03-31
PR Link: (inline refinements, no PR)
- Replaced old `PlayersSection` + `PlayerEntryRow` with full player row overhaul
- Files changed:
  - `app/src/main/java/app/meeplebook/feature/addplay/AddPlayScreen.kt`
- **What was implemented:**
    - Rich player rows: position badge (circular), color dot from PlayerColor enum (or text fallback for custom colors), name + @username in column, score (tappable placeholder), winner star toggle (amber when winner), drag handle
    - `SwipeToDismissBox`: swipe right → delete with red background + trash icon; swipe left → edit no-op with secondary background + edit icon
    - Undo snackbar: deleted player queued in `pendingUndo`, `LaunchedEffect` shows snackbar, ActionPerformed fires `RestorePlayer`
    - Drag reorder: `detectDragGestures` on drag handle, `graphicsLayer` for visual feedback, `PlayerReordered` event on release
    - Winner rows get light `primaryContainer` tint background; winner name is bold
    - Added `graphicsLayer` import (`androidx.compose.ui.graphics.graphicsLayer`)
- **Learnings for future iterations:**
    - `graphicsLayer` Modifier needs `import androidx.compose.ui.graphics.graphicsLayer` (not `.ui.draw.*`)
    - `SwipeToDismissBox.backgroundContent` uses `swipeState.targetValue` (not `currentValue`) for directional icon display
    - Undo must be handled in `PlayersSection` not the row itself — row leaves composition after delete
    - `LaunchedEffect(pendingUndo)` with snackbar is the right pattern; reset `pendingUndo = null` after snackbar call
    - `android.graphics.Color.parseColor(hexColor)` works inside Compose; wrap result in `androidx.compose.ui.graphics.Color(…)`
---
## 2026-03-31
PR Link: N/A (local implementation)
- Implemented `ScoreInputDialog.kt` — calculator-style numpad dialog for entering player scores
- Implemented `ColorPickerDialog.kt` — colored circle grid dialog for picking player colors
- Both dialogs dismiss on outside tap and Android back button without side effects
- Color indicator in `PlayerEntryRow` is now always visible (dashed empty circle when no color assigned), tapping opens `ColorPickerDialog`
- Score text in `PlayerEntryRow` tapping now opens `ScoreInputDialog`
- `ScoreChanged.score` changed from `Double` to `Double?` (null = clear score)
- `PlayerScoreReducer` updated: null score clears it; auto-winner logic skips players with null scores
- Added 6 string resources for both dialogs
- Added tests: `ScoreInputDialogTest` (numpad logic helpers), `ColorPickerDialogTest` (sorting/split logic), expanded `PlayerScoreReducerTest` (null score cases)
- Files changed/created:
  - `feature/addplay/ScoreInputDialog.kt` (new)
  - `feature/addplay/ColorPickerDialog.kt` (new)
  - `feature/addplay/AddPlayEvent.kt` (ScoreChanged nullable)
  - `feature/addplay/AddPlayScreen.kt` (tappable color circle + score, dialog state in PlayersSection)
  - `feature/addplay/reducer/PlayerScoreReducer.kt` (nullable score handling)
  - `res/values/strings.xml` (6 new strings)
  - `test/.../ScoreInputDialogTest.kt` (new)
  - `test/.../ColorPickerDialogTest.kt` (new)
  - `test/.../reducer/PlayerScoreReducerTest.kt` (null score tests appended)
- **Learnings for future iterations:**
    - Dialog state (which player is being edited) lives in the composable that owns the player list (`PlayersSection`), not in the ViewModel — keeps dialog lifecycle purely local
    - `Dialog { Surface { ... } }` with `DialogProperties(dismissOnClickOutside=true, dismissOnBackPress=true)` is the correct pattern for custom dismiss-on-outside/back dialogs
    - Testable logic extracted to `internal` top-level functions (e.g. `handleNumpadKey`, `sortedHistoryColors`, `remainingColors`) so Compose-free unit tests can cover it
    - `+/-` toggle on "-0" input: result is `-{digit}` (keeps minus sign, replaces zero) — not stripping the sign
    - `isLightColor()` helper with WCAG luminance formula correctly decides checkmark tint (black on light colors, white on dark)
    - `FlowRow` (ExperimentalLayoutApi) is cleaner than nested Rows for a variable-count color grid
    - When `colorsHistory` is non-empty, smart-expand opens compact view + MORE button; when empty, opens expanded immediately — logic gated on `startExpanded = colorsHistory.isEmpty()`
---

## 2026-03-31
PR Link: N/A (local session)
- Added 22 instrumented Compose UI tests for `AddPlayScreenRoot` in `AddPlayScreenRootTest.kt`
- Fixed pre-existing `score: Int? → Double?` type mismatch in `PlayDaoTest` and `PlayerDaoTest` helpers
- Added `@Preview` blocks (light + dark, `PreviewParameterProvider`) to all split AddPlay UI composable files: `AddPlaySearchContent`, `PlayerEntryRow`, `LocationSection`, `DateDurationSection`, `QuantityIncompleteSection`, `SuggestedPlayersSection`, `PlayersSection`
- Added `@Preview` to `MorePlayersDialog`; added KDoc to `MorePlayersDialog`
- Created `AddPlayPreviewData.kt` with shared `internal` preview state builders
- Files changed: `AddPlayScreenRootTest.kt` (new), `PlayDaoTest.kt`, `PlayerDaoTest.kt`, all ui/ composable files, `MorePlayersDialog.kt`, `AddPlayPreviewData.kt`
- **Learnings for future iterations:**
    - `assertDoesNotExist()` in Compose UI tests does NOT need an explicit import — calling it as a method on `SemanticsNodeInteraction` works without `import androidx.compose.ui.test.assertDoesNotExist`
    - `BackHandler` in Compose works with `Espresso.pressBack()` in instrumented tests using `createComposeRule()`
    - DAO test helper functions with `score: Int?` must be updated to `score: Double?` when `PlayerEntity.score` changes type
    - `AddPlayScreenRoot` accepts state directly — tests pass pre-constructed `AddPlayUiState` (no Hilt needed)
---

## 2026-04-02
PR Link: N/A (local session)
- Added "Add New Player" button to `MorePlayersDialog` + updated title to "Add Player"
- Created `AddEditPlayerDialog` for adding new players or editing existing ones (name, username, team/color with live color swatch, autocomplete dropdowns for both name and username fields)
- Added player search to data layer: `PlayerDao.searchDistinctPlayersByName/searchDistinctPlayersByUsername` (DISTINCT + LIKE queries), `PlaysLocalDataSource`, `PlaysRepository`, `SearchPlayersByNameUseCase`, `SearchPlayersByUsernameUseCase`
- Added `AddEditPlayerDialogState` to `GameSelected` UiState and `AddEditPlayerDialogEvent` sealed interface to `AddPlayEvent`
- Created `AddEditPlayerDialogReducer` (handles open/dismiss/field edits/confirm for both add-new and edit-existing flows)
- Wired `AddEditPlayerDialogReducer` into `PlayersReducer` pipeline
- Updated `AddPlayViewModel` with two debounced search flows (`rawAddEditNameQuery`, `rawAddEditUsernameQuery`) following existing `rawLocationQuery` pattern; search results fold into dialog state suggestions
- Wired EndToStart swipe-to-edit in `PlayersSection` → `ShowEditPlayerDialog` (was previously unimplemented)
- Updated `SuggestedPlayersSection` to pass `onAddNewPlayer` → `ShowAddPlayerDialog`
- Updated `FakePlaysRepository` and `FakePlaysLocalDataSource` test fakes with stub implementations of new interface methods
- Fixed `PlayersReducerTest`, `AddPlayReducerTest`, `AddPlayViewModelTest` to pass `AddEditPlayerDialogReducer` to `PlayersReducer`
- **Learnings for future iterations:**
    - Reducers that need early-return logic (guard clauses) must use block body `fun f(): T { return when(...) {...} }` not expression body `fun f(): T = when(...) {...}` — Kotlin forbids `return` inside expression bodies
    - When adding new methods to `PlaysRepository`/`PlaysLocalDataSource` interfaces, always update `FakePlaysRepository` and `FakePlaysLocalDataSource` in `src/test/` or unit test compilation fails
    - Player search queries go in `PlayerDao` (direct players table), not `PlayDao` (which needs JOIN with plays)
    - The `AddEditPlayerDialogReducer` takes `GameSelected` directly (not just the player list) because it modifies both `addEditPlayerDialog` state AND `players.players` on confirm
    - `PlayerLocationProjection` (from `core/database/projection/`) can be reused for player search DAO queries — same shape (name, username, MAX(userId))
---

## 2026-04-10
- **Task:** AddEditPlayerDialogReducer tests + Fake search implementations
- **Files changed:**
  - `app/src/test/.../FakePlaysRepository.kt` — implemented `searchPlayersByName`/`searchPlayersByUsername` (was `flowOf(emptyList())`)
  - `app/src/test/.../local/FakePlaysLocalDataSource.kt` — same for local data source fake
  - `app/src/test/.../reducer/AddEditPlayerDialogReducerTest.kt` — new file, 28 tests covering all reducer branches
  - `app/src/test/.../AddPlayViewModelTest.kt` — 4 new tests for AddEdit dialog search debounce flows
  - Also fixed 3 test files with stale `PlayersReducer(addEditDialogReducer=...)` API references
- **Learnings for future iterations:**
  - Fake search methods now filter `_plays`/`playsFlow` by name/username contains (case-insensitive) with `distinctBy` — usable in ViewModel search debounce tests
  - `AddEditPlayerDialogReducer` clears suggestions on field changes; they are populated externally by the ViewModel debounce flows
  - `ConfirmAddEditPlayer` resets raw name/username query flows in the ViewModel — reopening the dialog shows empty suggestions
  - `PlayTestFactory.createPlay(localPlayId, gameName, ...)` — `gameName` is a required parameter (not defaulted)
---

## 2026-04-10
PR Link: https://github.com/trumpets/meeplebook/pull/103
- Replaced hardcoded suggestion-label interpolation in `AddEditPlayerDialog` with string-resource formatting for consistent localization using UiText.
- Files changed:
  - `app/src/main/java/app/meeplebook/feature/addplay/ui/dialogs/AddEditPlayerDialog.kt`
- **Learnings for future iterations:**
  - `player_name_with_username` should be reused for user-visible player labels, and a matching simple name resource keeps formatting/localization consistent across features.
---

## 2026-04-10
PR Link: https://github.com/trumpets/meeplebook/pull/103
- Updated repo guidance and skill docs so MeepleBook itself is the source of truth for architecture, navigation, networking, and test workflow guidance.
- Files changed:
  - `AGENTS.md`
  - `.github/copilot-instructions.md`
  - `.github/skills/architecture/android-architecture/SKILL.md`
  - `.github/skills/architecture/android-viewmodel/SKILL.md`
  - `.github/skills/architecture/android-data-layer/SKILL.md`
  - `.github/skills/build_and_tooling/android-gradle-logic/SKILL.md`
  - `.github/skills/concurrency_and_networking/android-retrofit/SKILL.md`
  - `.github/skills/ui/compose-navigation/SKILL.md`
  - `.github/skills/testing_and_automation/android-testing/SKILL.md`
  - `.github/skills/performance/gradle-build-performance/SKILL.md`
- **Learnings for future iterations:**
  - The repo must be documented from its current package-based `:app` structure first; multi-module remains a future target, not an assumed present state.
  - Skill docs drift quickly when they embed generic Android examples with hardcoded module names, task names, or library setup; MeepleBook-specific notes should be added near the top of those docs.
  - Screenshot guidance for this repo is Paparazzi-first if/when added; Roborazzi should only be mentioned as an explicit future user choice and never as an assumed dependency.
  - CI/source-of-truth verification commands are `./gradlew testDebugUnitTest :lint-rules:test`, `./gradlew lint`, and `./gradlew connectedDebugAndroidTest`.
---

## 2026-04-16
PR Link: <pending>
- Refactored the Plays feature to follow the AddPlay-style reducer/effect flow while keeping the current sealed `PlaysUiState` as the reducer-owned state shape.
- Added Plays reducer/effect production tests, updated Plays ViewModel/UI tests for nested action events and reducer-driven state, and added an in-package markdown note documenting the cleaner alternate base-state model.
- Files changed:
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysUiState.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysViewModel.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysScreen.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysEvent.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/effect/PlaysEffect.kt` (new)
  - `app/src/main/java/app/meeplebook/feature/plays/effect/PlaysEffects.kt` (new)
  - `app/src/main/java/app/meeplebook/feature/plays/effect/PlaysUiEffect.kt` (new)
  - `app/src/main/java/app/meeplebook/feature/plays/effect/PlaysEffectProducer.kt` (new)
  - `app/src/test/java/app/meeplebook/feature/plays/PlaysViewModelTest.kt`
  - `app/src/test/java/app/meeplebook/feature/plays/reducer/PlaysReducerTest.kt` (new)
  - `app/src/test/java/app/meeplebook/feature/plays/effect/PlaysEffectProducerTest.kt` (new)
  - `app/src/androidTest/java/app/meeplebook/feature/plays/PlaysScreenRootTest.kt`
- **Learnings for future iterations:**
  - If a sealed UI state is kept reducer-owned, convert singleton variants like `Loading` into data classes and use shared helpers such as `updateCommon(...)` to avoid variant-specific mutation boilerplate.
  - AddPlay-style architecture ports cleanly to simpler features when the ViewModel owns only reducer state + effect handling and all debounce pipes derive from reducer-owned state.
  - Keep alternate architecture notes close to the feature when a requested implementation path is knowingly less clean; it preserves the fallback design without mixing it into the active code.
  - Validation in this worktree is currently split: `:app:testDebugUnitTest` passes, while repo lint and Android-test compilation are blocked by pre-existing `:lint-rules` JDK target mismatch and unrelated Collection WIP in `CollectionScreenRootTest`.
---

## 2026-04-16
PR Link: <pending>
- Replaced the temporary Plays reducer-owned sealed-state approach with the documented `PlaysBaseState` architecture, so the reducer now owns only search/refresh state and `PlaysUiState` is derived from observed plays data.
- Updated Plays reducer/effect/ViewModel tests to target the base-state relationship and aligned the in-package architecture document with the implemented model.
- Files changed:
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysBaseState.kt` (new)
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysUiState.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysViewModel.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/reducer/PlaysReducer.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/effect/PlaysEffectProducer.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysScreen.kt`
  - `app/src/test/java/app/meeplebook/feature/plays/PlaysViewModelTest.kt`
  - `app/src/test/java/app/meeplebook/feature/plays/reducer/PlaysReducerTest.kt`
  - `app/src/test/java/app/meeplebook/feature/plays/effect/PlaysEffectProducerTest.kt`
  - `app/src/androidTest/java/app/meeplebook/feature/plays/PlaysScreenRootTest.kt`
- **Learnings for future iterations:**
  - For query/list screens, a small reducer-owned base state plus derived display state is easier to reason about than mutating a sealed UI state directly.
  - Reducer tests should target the base state only; ViewModel tests should verify the `combine(baseState, externalData) -> uiState` contract.
  - Keep `SharedFlow` UI effects separate from derived `StateFlow` UI state so navigation and snackbar behavior stay one-shot during architecture refactors.
  - `:app:testDebugUnitTest` validates this Plays migration successfully; broader lint / Android-test issues in this worktree remain unrelated repo blockers.
---

## 2026-04-16
PR Link: <pending>
- Updated KDoc across the Plays architecture files so the reducer/base-state/derived-ui-state flow is documented consistently.
- Files changed:
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysBaseState.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysEvent.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysUiState.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysMappers.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/effect/PlaysUiEffect.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/effect/PlaysEffect.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/effect/PlaysEffects.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/effect/PlaysEffectProducer.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/reducer/PlaysReducer.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysViewModel.kt`
- **Learnings for future iterations:**
  - When documenting reducer-driven screens in this repo, describe all four layers together: events, reducer-owned state, derived UI state, and one-shot effects.
  - KDoc for mapper functions should explain architectural responsibility, not just field mapping, when they are the boundary between base state and display state.
---

## 2026-04-16T20:17:56Z
PR Link: <pending>
- Added reusable reducer/effect architecture primitives and a lightweight `ReducerViewModel` base for future screen migrations.
- Updated agent guidance so future work reuses the shared contracts without forcing the feature-specific `combine(...)` layer into a generic framework.
- Files changed:
  - `app/src/main/java/app/meeplebook/core/ui/architecture/Reducer.kt` (new)
  - `app/src/main/java/app/meeplebook/core/ui/architecture/EffectProducer.kt` (new)
  - `app/src/main/java/app/meeplebook/core/ui/architecture/ProducedEffects.kt` (new)
  - `app/src/main/java/app/meeplebook/core/ui/architecture/ReducerViewModel.kt` (new)
  - `app/src/test/java/app/meeplebook/core/ui/architecture/ProducedEffectsTest.kt` (new)
  - `app/src/test/java/app/meeplebook/core/ui/architecture/ReducerViewModelTest.kt` (new)
  - `AGENTS.md`
  - `.github/copilot-instructions.md`
- **Learnings for future iterations:**
  - The reusable seam is the reducer/effect pipeline, not the full screen-state composition; keep `combine(baseState, externalData) -> uiState` inside each feature.
  - `ReducerViewModel` should stay lightweight: own reducer state and one-shot UI effects, while leaving query-flow derivation and domain-specific effect handling to subclasses.
  - `ProducedEffects.none()` is the generic replacement for per-feature `None` bundles when future screens adopt the shared contracts.
---

## 2026-04-16T22:05:27Z
PR Link: <pending>
- Fixed the Collection reducer migration so Collection now follows the same base-state architecture as Plays.
- Rebuilt `CollectionViewModel` around reducer-owned state and state-derived flows, and moved sort-sheet visibility out of UI effects into reducer/base state.
- Updated Collection screen, unit tests, and androidTest sources to the nested event model and singular `CollectionUiEffect` type.
- Files changed:
  - `app/src/main/java/app/meeplebook/feature/collection/CollectionBaseState.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/CollectionViewModel.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/CollectionScreen.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/effect/CollectionEffectProducer.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/effect/CollectionUiEffect.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/reducer/CollectionDisplayReducer.kt`
  - `app/src/test/java/app/meeplebook/feature/collection/CollectionViewModelTest.kt`
  - `app/src/androidTest/java/app/meeplebook/feature/collection/CollectionScreenRootTest.kt`
- **Learnings for future iterations:**
  - Persistent UI chrome such as sort-sheet visibility belongs in reducer-owned base state, not in `SharedFlow` UI effects.
  - For debounced search screens, fetch queries should derive from reducer-owned raw input, but renderable UI state should still read the raw reducer value so typing updates immediately.
  - When migrating a feature to nested event groups, update UI, unit tests, and android tests together; otherwise stale flat event references hide in one layer after another.
---

## 2026-04-16T22:36:12Z
PR Link: <pending>
- Updated Collection KDoc so the migrated reducer architecture is documented consistently across state, events, reducers, effects, and ViewModel orchestration.
- Files changed:
  - `app/src/main/java/app/meeplebook/feature/collection/CollectionBaseState.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/CollectionEvent.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/CollectionUiState.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/CollectionViewModel.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/effect/CollectionEffect.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/effect/CollectionUiEffect.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/effect/CollectionEffectProducer.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/reducer/CollectionDisplayReducer.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/reducer/CollectionFilterReducer.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/reducer/CollectionReducer.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/reducer/CollectionSearchReducer.kt`
- **Learnings for future iterations:**
  - After migrating a feature to reducer-owned base state, refresh KDoc across all state/event/reducer/effect files in the same pass so the feature doesn’t keep explaining the pre-migration architecture.
  - Collection docs should explicitly distinguish persistent UI state (sort sheet, view mode, filters) from one-shot UI effects (navigation, scroll, snackbar), because that boundary is easy to blur during refactors.
---

## 2026-04-17T08:18:34Z
PR Link: N/A
- Refactored Overview to use top-level `Loading / Error / Content` screen states and the shared reducer/effect architecture
- Replaced the old flag-based `OverviewUiState` and `_uiEffects` flow with `OverviewBaseState`, `OverviewEvent`, reducer/effect files, and a `ReducerViewModel`-based `OverviewViewModel`
- Updated Overview unit tests and Compose tests to target the new sealed state model, plus added focused reducer/effect producer tests
- Files changed:
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewBaseState.kt` (new)
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewEvent.kt` (new)
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewMappers.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewScreen.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewUiState.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewViewModel.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/effect/OverviewEffect.kt` (new)
  - `app/src/main/java/app/meeplebook/feature/overview/effect/OverviewEffectProducer.kt` (new)
  - `app/src/main/java/app/meeplebook/feature/overview/effect/OverviewUiEffect.kt` (new)
  - `app/src/main/java/app/meeplebook/feature/overview/reducer/OverviewReducer.kt` (new)
  - `app/src/test/java/app/meeplebook/feature/overview/OverviewViewModelTest.kt`
  - `app/src/test/java/app/meeplebook/feature/overview/effect/OverviewEffectProducerTest.kt` (new)
  - `app/src/test/java/app/meeplebook/feature/overview/reducer/OverviewReducerTest.kt` (new)
  - `app/src/androidTest/java/app/meeplebook/feature/overview/OverviewContentTest.kt`
- **Learnings for future iterations:**
    - When a feature adopts top-level `Loading / Error / Content`, content-only composables and their tests should take the `Content` subtype directly, while root screen tests should target the screen-state switch
    - In reducer-driven screens, async refresh/error handling belongs in reducer-owned base state plus domain effects, not in a parallel mutable UI-effects state flow
    - Migrating a screen to sealed render states often requires updating downstream Compose tests that were constructing the old flat state directly
---

## 2026-04-17T09:04:54Z
PR Link: N/A
- Added and refreshed KDoc across the Overview reducer-architecture files so the docs match the current sealed screen-state model
- Documented the distinction between reducer-owned base state, derived `OverviewUiState`, domain effects, and one-shot UI effects
- Files changed:
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewUiState.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewEvent.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewMappers.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewBaseState.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewViewModel.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/effect/OverviewEffect.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/effect/OverviewUiEffect.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/effect/OverviewEffectProducer.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/reducer/OverviewReducer.kt`
- **Learnings for future iterations:**
    - For reducer-driven screens, KDoc should document both layers explicitly: reducer-owned base state and the derived renderable sealed `UiState`
    - Effect producer docs are most useful when they explain which events become domain work versus one-shot navigation/UI effects
    - When a reducer is intentionally minimal, document that async effect results update state through `updateBaseState` so the no-op reduce path is clearly intentional
---

## 2026-04-17T09:14:55Z
PR Link: N/A
- Refactored Login to the shared reducer/effect architecture using a thin form-focused implementation
- Replaced direct mutator methods and persistent `isLoggedIn` state with `LoginEvent`, `LoginEffect`, `LoginUiEffect`, `LoginEffectProducer`, and `LoginReducer`
- Updated `LoginScreen` to emit events and collect the success UI effect, migrated Login error rendering to `UiText`, and refreshed Login tests to the new contract
- Files changed:
  - `AGENTS.md`
  - `app/src/main/java/app/meeplebook/feature/login/LoginEvent.kt` (new)
  - `app/src/main/java/app/meeplebook/feature/login/LoginUiState.kt`
  - `app/src/main/java/app/meeplebook/feature/login/LoginViewModel.kt`
  - `app/src/main/java/app/meeplebook/feature/login/LoginScreen.kt`
  - `app/src/main/java/app/meeplebook/feature/login/effect/LoginEffect.kt` (new)
  - `app/src/main/java/app/meeplebook/feature/login/effect/LoginUiEffect.kt` (new)
  - `app/src/main/java/app/meeplebook/feature/login/effect/LoginEffectProducer.kt` (new)
  - `app/src/main/java/app/meeplebook/feature/login/reducer/LoginReducer.kt` (new)
  - `app/src/test/java/app/meeplebook/feature/login/LoginViewModelTest.kt`
  - `app/src/androidTest/java/app/meeplebook/feature/login/LoginScreenContentTest.kt`
  - `app/src/androidTest/java/app/meeplebook/feature/overview/OverviewContentTest.kt`
- **Learnings for future iterations:**
    - Simple form screens can still use the shared reducer architecture without a separate base-state/derived-state split when there is no external observed data
    - Replace persistent success booleans with one-shot `UiEffect`s when the UI only needs navigation or another transient outcome
    - Migrating older screens to `UiText` often requires both ViewModel assertions and Compose tests to stop depending on raw string resource ids
---

## 2026-04-17T11:01:26Z
PR Link: N/A
- Fixed Collection alphabet-jump architecture so jump resolution happens in `CollectionViewModel` from the latest derived `CollectionUiState.Content`
- Changed `JumpToLetter` handling to emit a domain effect first, then resolve and emit a fully populated `ScrollToIndex` UI effect
- Updated Collection tests to verify resolved scroll effects and non-content no-op behavior
- Files changed:
  - `AGENTS.md`
  - `app/src/main/java/app/meeplebook/feature/collection/effect/CollectionEffect.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/effect/CollectionEffectProducer.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/CollectionViewModel.kt`
  - `app/src/test/java/app/meeplebook/feature/collection/CollectionViewModelTest.kt`
- **Learnings for future iterations:**
    - When a UI effect needs data from derived render state, route through a domain effect and resolve in the ViewModel, not in the `EffectProducer`
    - `EffectProducer` should express intent from reducer/base state, while the ViewModel may safely consult the latest derived `uiState` to produce a complete one-shot UI effect
    - Avoid pushing derived render-only data like section indices back into base state just to satisfy an effect payload
---

## 2026-04-19T16:03:49Z
PR Link: N/A
- Implemented Prompt 1 from `BACKGROUND_SYNC_PLAN.md` by normalizing sync boundaries without starting Room sync-state migration or WorkManager work
- Clarified repository/local-data-source KDoc around pull sync vs outbox responsibilities and refactored `SyncUserDataUseCase` to compose `SyncCollectionUseCase` and `SyncPlaysUseCase`
- Updated sync tests and the dependent `OverviewViewModelTest` constructor wiring to match the new full-sync composition seam
- Files changed:
  - `AGENTS.md`
  - `app/src/main/java/app/meeplebook/core/collection/CollectionRepository.kt`
  - `app/src/main/java/app/meeplebook/core/collection/local/CollectionLocalDataSource.kt`
  - `app/src/main/java/app/meeplebook/core/plays/PlaysRepository.kt`
  - `app/src/main/java/app/meeplebook/core/plays/local/PlaysLocalDataSource.kt`
  - `app/src/main/java/app/meeplebook/core/sync/domain/SyncCollectionUseCase.kt`
  - `app/src/main/java/app/meeplebook/core/sync/domain/SyncPlaysUseCase.kt`
  - `app/src/main/java/app/meeplebook/core/sync/domain/SyncUserDataUseCase.kt`
  - `app/src/test/java/app/meeplebook/core/sync/domain/SyncUserDataUseCaseTest.kt`
  - `app/src/test/java/app/meeplebook/feature/overview/OverviewViewModelTest.kt`
- **Learnings for future iterations:**
    - Keep repository sync methods focused on transport, mapping, and local persistence; auth gating and full-sync sequencing belong in sync use cases
    - `SyncUserDataUseCase` should compose narrower sync use cases instead of duplicating repository/auth/timestamp logic
    - Prompt 1 should stop at boundary cleanup so Prompt 2 can own Room sync-state migration cleanly
---

## 2026-04-19T16:54:36Z
PR Link: N/A (Prompt 2 from `BACKGROUND_SYNC_PLAN.md`)
- Implemented Room-backed sync execution state for collection and plays, including new sync entities, `SyncDao`, and a Room-based `SyncTimeRepositoryImpl`
- Updated sync use cases to persist started/success/failed state per domain and derived full-sync observation from the two per-domain records
- Added and updated tests for sync use cases, full-sync observation, and the new Room DAO
- Files changed:
  - `app/src/main/java/app/meeplebook/core/database/MeepleBookDatabase.kt`
  - `app/src/main/java/app/meeplebook/core/database/dao/SyncDao.kt`
  - `app/src/main/java/app/meeplebook/core/database/entity/CollectionSyncStateEntity.kt`
  - `app/src/main/java/app/meeplebook/core/database/entity/PlaysSyncStateEntity.kt`
  - `app/src/main/java/app/meeplebook/core/sync/SyncTimeRepository.kt`
  - `app/src/main/java/app/meeplebook/core/sync/SyncTimeRepositoryImpl.kt`
  - `app/src/main/java/app/meeplebook/core/sync/domain/SyncCollectionUseCase.kt`
  - `app/src/main/java/app/meeplebook/core/sync/domain/SyncPlaysUseCase.kt`
  - `app/src/main/java/app/meeplebook/core/sync/domain/SyncUserDataUseCase.kt`
  - `app/src/main/java/app/meeplebook/core/sync/model/SyncState.kt`
  - `app/src/test/java/app/meeplebook/core/sync/FakeSyncTimeRepository.kt`
  - `app/src/test/java/app/meeplebook/core/sync/domain/ObserveLastFullSyncUseCaseTest.kt`
  - `app/src/test/java/app/meeplebook/core/sync/domain/SyncCollectionUseCaseTest.kt`
  - `app/src/test/java/app/meeplebook/core/sync/domain/SyncPlaysUseCaseTest.kt`
  - `app/src/test/java/app/meeplebook/core/sync/domain/SyncUserDataUseCaseTest.kt`
  - `app/src/test/java/app/meeplebook/feature/overview/OverviewViewModelTest.kt`
  - `app/src/androidTest/java/app/meeplebook/core/database/dao/SyncDaoTest.kt`
  - `AGENTS.md`
  - `progress.md`
- **Learnings for future iterations:**
  - Sync execution state in this repo is stored as separate Room singleton rows for collection and plays, not as a single combined record
  - `observeLastFullSync()` now represents the latest moment both domains were synced by deriving the minimum of the two successful timestamps
  - Domain sync use cases own started/success/failed state transitions; `SyncUserDataUseCase` should stay a thin orchestration layer
  - DAO tests for singleton sync-state tables can follow the same in-memory Room pattern as other DAO tests, with `id = 0` rows replaced via `@Upsert`
---

## 2026-04-19T22:23:43.989+02:00
PR Link: N/A (sync refactor follow-up)
- Reworked sync persistence from two Room tables into a single `sync_states` table keyed by `SyncType`
- Moved sync lifecycle writes fully behind `SyncRunner`/generic `SyncTimeRepository` methods and replaced read-modify-write updates with partial UPSERT queries in `SyncDao`
- Added `SyncRunnerTest`, updated sync/use-case tests and DAO tests, and expanded KDoc across the sync package
- Files changed:
  - `app/src/main/java/app/meeplebook/core/database/converters/DateTimeConverters.kt`
  - `app/src/main/java/app/meeplebook/core/database/MeepleBookDatabase.kt`
  - `app/src/main/java/app/meeplebook/core/database/dao/SyncDao.kt`
  - `app/src/main/java/app/meeplebook/core/database/entity/SyncStateEntity.kt`
  - `app/src/main/java/app/meeplebook/core/sync/SyncRunner.kt`
  - `app/src/main/java/app/meeplebook/core/sync/SyncTimeModule.kt`
  - `app/src/main/java/app/meeplebook/core/sync/SyncTimeRepository.kt`
  - `app/src/main/java/app/meeplebook/core/sync/SyncTimeRepositoryImpl.kt`
  - `app/src/main/java/app/meeplebook/core/sync/domain/ObserveLastFullSyncUseCase.kt`
  - `app/src/main/java/app/meeplebook/core/sync/domain/SyncCollectionUseCase.kt`
  - `app/src/main/java/app/meeplebook/core/sync/domain/SyncPlaysUseCase.kt`
  - `app/src/main/java/app/meeplebook/core/sync/domain/SyncUserDataUseCase.kt`
  - `app/src/main/java/app/meeplebook/core/sync/model/SyncState.kt`
  - `app/src/main/java/app/meeplebook/core/sync/model/SyncType.kt`
  - `app/src/main/java/app/meeplebook/core/sync/model/SyncUserDataError.kt`
  - `app/src/test/java/app/meeplebook/core/sync/FakeSyncTimeRepository.kt`
  - `app/src/test/java/app/meeplebook/core/sync/SyncRunnerTest.kt`
  - `app/src/test/java/app/meeplebook/core/sync/domain/ObserveLastFullSyncUseCaseTest.kt`
  - `app/src/test/java/app/meeplebook/core/sync/domain/SyncCollectionUseCaseTest.kt`
  - `app/src/test/java/app/meeplebook/core/sync/domain/SyncPlaysUseCaseTest.kt`
  - `app/src/test/java/app/meeplebook/core/sync/domain/SyncUserDataUseCaseTest.kt`
  - `app/src/androidTest/java/app/meeplebook/core/database/dao/SyncDaoTest.kt`
  - `app/src/test/java/app/meeplebook/feature/collection/CollectionViewModelTest.kt`
  - `app/src/test/java/app/meeplebook/feature/overview/OverviewViewModelTest.kt`
  - `app/src/test/java/app/meeplebook/feature/plays/PlaysViewModelTest.kt`
  - `AGENTS.md`
  - `progress.md`
- **Learnings for future iterations:**
  - Sync state now has one Room row per `SyncType` in `sync_states`; do not reintroduce per-domain tables unless the storage model intentionally changes again
  - `SyncRunner` is the single place that should persist start/success/failure lifecycle transitions for sync work
  - Room partial lifecycle updates can avoid extra reads by using `INSERT ... ON CONFLICT DO UPDATE` queries that only touch the needed columns
  - Full-sync time remains a derived value: the older of the collection and plays success timestamps
---

## 2026-04-19T21:23:41Z
PR Link: <pending>
- Implemented Prompt 3 from `BACKGROUND_SYNC_PLAN.md`: pending play outbox upload support across repository, local, and remote layers
- Added BGG play-save POST support via `geekplay.php`, including aligned `playdate`/`dateinput`, indexed player form fields, and `playid` for remote edits
- Added retryable outbox selection for both `PENDING` and `FAILED` plays, with local transitions to `SYNCED` and `FAILED`
- Files changed:
  - `app/src/main/java/app/meeplebook/core/network/BggApi.kt`
  - `app/src/main/java/app/meeplebook/core/plays/PlaysRepository.kt`
  - `app/src/main/java/app/meeplebook/core/plays/PlaysRepositoryImpl.kt`
  - `app/src/main/java/app/meeplebook/core/plays/local/PlaysLocalDataSource.kt`
  - `app/src/main/java/app/meeplebook/core/plays/local/PlaysLocalDataSourceImpl.kt`
  - `app/src/main/java/app/meeplebook/core/plays/remote/PlaysRemoteDataSource.kt`
  - `app/src/main/java/app/meeplebook/core/plays/remote/PlaysRemoteDataSourceImpl.kt`
  - `app/src/main/java/app/meeplebook/core/plays/remote/PlayUploadException.kt`
  - `app/src/main/java/app/meeplebook/core/database/dao/PlayDao.kt`
  - `app/src/test/java/app/meeplebook/core/plays/PlaysRepositoryImplTest.kt`
  - `app/src/androidTest/java/app/meeplebook/core/plays/local/PlaysLocalDataSourceImplTest.kt`
  - test fakes and integration tests under `app/src/test/java/app/meeplebook/core/plays/`
- **Learnings for future iterations:**
  - Pending play uploads use the authenticated cookie session already tracked in `CurrentCredentialsStore`; treat missing or expired auth as a fatal sync failure, not a silent skip
  - `syncPendingPlays()` retries both `PENDING` and `FAILED` plays on each run, but only network, auth, and retry-exhaustion failures should stop the batch early
  - Per-play validation or parse failures should mark that play `FAILED` and continue so one bad play does not block the rest of the outbox
  - `geekplay.php` edits require `playid`, while new uploads omit it; always keep `playdate` and `dateinput` synchronized in `yyyy-MM-dd`
---

## 2026-04-19T21:55:51Z
PR Link: <pending>
- Refined pending-play upload response handling to match actual BGG `geekplay.php` payloads
- Logged-out 200 responses with `{"error":"You must login to save plays"}` now map to auth failure, and successful 200 JSON payloads parse the returned `playid`
- Added focused integration coverage for both upload response shapes
- Files changed:
  - `app/src/main/java/app/meeplebook/core/plays/remote/PlaysRemoteDataSourceImpl.kt`
  - `app/src/test/java/app/meeplebook/core/plays/remote/integration/PlaysRemoteDataSourceImplTest.kt`
- **Learnings for future iterations:**
  - `geekplay.php` can return HTTP 200 for both success and auth failure, so upload handling must inspect the JSON body rather than rely on status code alone
  - Treat `{"error":"You must login to save plays"}` as a not-logged-in failure so repository mapping reaches `PlayError.NotLoggedIn`
  - Successful upload payloads return `playid` in JSON; keep a direct parser path for that shape covered by tests
---

## 2026-04-19T22:28:14Z
PR Link: <pending>
- Fixed a regression where plays XML parsing returned empty results across parser, remote-data-source, and repository integration tests
- Restored separate BGG wire date formatting so remote play dates parse and serialize as `yyyy-MM-dd` while UI dates remain `dd/MM/yyyy`
- Files changed:
  - `app/src/main/java/app/meeplebook/core/util/DateUtils.kt`
  - `AGENTS.md`
  - `progress.md`
- **Learnings for future iterations:**
  - `parseBggDate` and `formatBggDate` must use BGG's ISO date format, not the user-facing EU formatter
  - When widespread play-sync tests suddenly return zero items, check date parsing first because invalid play dates cause `PlaysXmlParser` to drop whole plays
---

## 2026-04-20T11:28:40Z
PR Link: <pending>
- Implemented Prompt 4 from `BACKGROUND_SYNC_PLAN.md` by adding thin WorkManager workers for collection pull sync, plays pull sync, and pending-play upload sync
- Added shared worker result mapping so retryable sync failures return `Result.retry()`, unknown failures return `Result.failure()`, and logged-out runs no-op with `Result.success()`
- Added focused worker unit tests and the `work-runtime-ktx` dependency needed for the new workers
- Files changed:
  - `app/src/main/java/app/meeplebook/core/sync/work/SyncWorkerEntryPoint.kt`
  - `app/src/main/java/app/meeplebook/core/sync/work/SyncWorkerResultMapper.kt`
  - `app/src/main/java/app/meeplebook/core/sync/work/SyncCollectionWorker.kt`
  - `app/src/main/java/app/meeplebook/core/sync/work/SyncPlaysWorker.kt`
  - `app/src/main/java/app/meeplebook/core/sync/work/SyncPendingPlaysWorker.kt`
  - `app/src/test/java/app/meeplebook/core/sync/work/SyncCollectionWorkerTest.kt`
  - `app/src/test/java/app/meeplebook/core/sync/work/SyncPlaysWorkerTest.kt`
  - `app/src/test/java/app/meeplebook/core/sync/work/SyncPendingPlaysWorkerTest.kt`
  - `app/src/test/java/app/meeplebook/core/plays/FakePlaysRepository.kt`
  - `gradle/libs.versions.toml`
  - `app/build.gradle.kts`
  - `AGENTS.md`
  - `progress.md`
- **Learnings for future iterations:**
  - Prompt 4 can avoid `androidx.hilt:hilt-work` by keeping worker constructors at `(Context, WorkerParameters)` and resolving app dependencies through a Hilt `@EntryPoint`
  - Put WorkManager result mapping in one shared helper so retry/no-op/failure policy stays consistent across all sync workers
  - Worker tests can stay as plain unit tests by subclassing the worker and overriding the dependency accessor instead of bootstrapping real WorkManager state
---

## 2026-04-20T19:12:01Z
PR Link: N/A (post-review hardening)
- Replaced the temporary worker entry-point setup with proper Hilt WorkManager integration and aligned worker result policy with the review findings
- Wired `MeepleBookApp` as the `HiltWorkerFactory` provider, removed the default `WorkManagerInitializer`, and migrated worker coverage to `TestListenableWorkerBuilder` androidTests plus a focused mapper unit test
- Files changed:
  - `app/src/main/java/app/meeplebook/MeepleBookApp.kt`
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/java/app/meeplebook/core/sync/work/SyncCollectionWorker.kt`
  - `app/src/main/java/app/meeplebook/core/sync/work/SyncPlaysWorker.kt`
  - `app/src/main/java/app/meeplebook/core/sync/work/SyncPendingPlaysWorker.kt`
  - `app/src/main/java/app/meeplebook/core/sync/work/SyncWorkerResultMapper.kt`
  - `app/src/test/java/app/meeplebook/core/sync/work/SyncWorkerResultMapperTest.kt`
  - `app/src/androidTest/java/app/meeplebook/HiltTestRunner.kt`
  - `app/src/androidTest/java/app/meeplebook/core/sync/work/SyncWorkerTestDoubles.kt`
  - `app/src/androidTest/java/app/meeplebook/core/sync/work/SyncCollectionWorkerTest.kt`
  - `app/src/androidTest/java/app/meeplebook/core/sync/work/SyncPlaysWorkerTest.kt`
  - `app/src/androidTest/java/app/meeplebook/core/sync/work/SyncPendingPlaysWorkerTest.kt`
  - `app/build.gradle.kts`
  - `AGENTS.md`
  - `progress.md`
- **Learnings for future iterations:**
  - In this repo, sync workers should use `@HiltWorker` constructor injection and `MeepleBookApp`'s `HiltWorkerFactory` rather than a manual Hilt entry-point lookup
  - `MaxRetriesExceeded` is terminal for sync workers and should map to `ListenableWorker.Result.Failure`, while transient network failures still map to `Result.Retry`
  - Hilt-backed worker behavior is best covered here with instrumented `TestListenableWorkerBuilder` tests plus fake repositories bound through `@BindValue`
---

## 2026-04-20T23:03:10+02:00
PR Link: N/A (worker test fix)
- Fixed the Hilt worker androidTest setup so the generated test component and on-device worker instantiation succeed
- Added a fake `AuthLocalDataSource` binding for the worker tests, switched the worker test builders off the reflective `from(...)` path, and added the missing `androidx.hilt:hilt-compiler` KSP wiring required by `hilt-work`
- Files changed:
  - `app/src/androidTest/java/app/meeplebook/core/sync/work/SyncWorkerTestDoubles.kt`
  - `app/src/androidTest/java/app/meeplebook/core/sync/work/SyncCollectionWorkerTest.kt`
  - `app/src/androidTest/java/app/meeplebook/core/sync/work/SyncPlaysWorkerTest.kt`
  - `app/src/androidTest/java/app/meeplebook/core/sync/work/SyncPendingPlaysWorkerTest.kt`
  - `app/build.gradle.kts`
  - `gradle/libs.versions.toml`
  - `AGENTS.md`
  - `progress.md`
- **Learnings for future iterations:**
  - Removing `AuthModule` in Hilt androidTests also removes `AuthLocalDataSource`, so tests that still build the production OkHttp/auth graph need a replacement binding for that local datasource
  - `hilt-work` also needs `androidx.hilt:hilt-compiler` on KSP; otherwise `HiltWorkerFactory` is created with an empty worker map and falls back to reflection
  - When testing Hilt workers with `TestListenableWorkerBuilder`, avoid the `from(context, WorkerClass::class.java)` reflection path and use the builder with an injected worker factory instead
---

## 2026-04-20T23:26:00+02:00
PR Link: N/A (Prompt 5)
- Implemented Prompt 5 from `BACKGROUND_SYNC_PLAN.md` by introducing `SyncManager` and a WorkManager-backed orchestration implementation
- Centralized unique work names, `NetworkType.CONNECTED` constraints, `ExistingWorkPolicy.KEEP`, and the full-sync chain order of pending plays -> plays -> collection
- Added focused unit tests that verify unique work enqueueing, worker request constraints, and full-sync chaining order without touching trigger policy or UI wiring
- Files changed:
  - `app/src/main/java/app/meeplebook/core/sync/SyncManager.kt` (new)
  - `app/src/main/java/app/meeplebook/core/sync/SyncManagerModule.kt` (new)
  - `app/src/main/java/app/meeplebook/core/sync/WorkManagerSyncManager.kt` (new)
  - `app/src/test/java/app/meeplebook/core/sync/WorkManagerSyncManagerTest.kt` (new)
  - `AGENTS.md`
  - `progress.md`
- **Learnings for future iterations:**
  - Keep WorkManager orchestration details out of UI/viewmodels; enqueue through `SyncManager` so work names, constraints, and policy stay centralized
  - Full sync should remain push-before-pull in this repo: pending plays upload first, then plays pull, then collection pull
  - `ExistingWorkPolicy.KEEP` is the current de-duplication policy for one-shot sync requests and the unique full-sync chain
---

## 2026-04-20T23:56:31+02:00
PR Link: N/A (Prompt 6)
- Implemented Prompt 6 from `BACKGROUND_SYNC_PLAN.md` by wiring the automatic sync trigger policy through `SyncManager`
- App start now schedules a daily periodic full sync and enqueues an immediate full sync for logged-in users, Collection/Plays screen-open enqueue their domain syncs, and successful play saves enqueue pending-play upload sync
- Added a periodic orchestration worker plus tests covering the new periodic request, screen-open trigger calls, play-save trigger call, and the periodic worker itself
- Files changed:
  - `app/src/main/java/app/meeplebook/MainActivity.kt`
  - `app/src/main/java/app/meeplebook/core/sync/manager/SyncManager.kt`
  - `app/src/main/java/app/meeplebook/core/sync/manager/WorkManagerSyncManager.kt`
  - `app/src/main/java/app/meeplebook/core/sync/work/SyncPeriodicFullSyncWorker.kt` (new)
  - `app/src/main/java/app/meeplebook/feature/collection/CollectionViewModel.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysViewModel.kt`
  - `app/src/main/java/app/meeplebook/feature/addplay/AddPlayViewModel.kt`
  - `app/src/test/java/app/meeplebook/core/sync/manager/FakeSyncManager.kt` (new)
  - `app/src/test/java/app/meeplebook/core/sync/manager/WorkManagerSyncManagerTest.kt`
  - `app/src/test/java/app/meeplebook/feature/collection/CollectionViewModelTest.kt`
  - `app/src/test/java/app/meeplebook/feature/plays/PlaysViewModelTest.kt`
  - `app/src/test/java/app/meeplebook/feature/addplay/AddPlayViewModelTest.kt`
  - `app/src/androidTest/java/app/meeplebook/core/sync/work/SyncWorkerTestDoubles.kt`
  - `app/src/androidTest/java/app/meeplebook/core/sync/work/SyncPeriodicFullSyncWorkerTest.kt` (new)
  - `AGENTS.md`
  - `progress.md`
- **Learnings for future iterations:**
  - Keep automatic background triggers thin and routed through `SyncManager`; let Prompt 7 handle user-driven manual refresh migration separately
  - The periodic sync default is currently one daily full-sync trigger backed by a dedicated orchestration worker that simply re-enqueues the existing full-sync chain
  - Screen-open auto triggers are currently domain-specific: Collection opens enqueue collection pull, Plays opens enqueue plays pull, while play saves enqueue pending-play upload only
---

## 2026-04-21T10:52:20Z
PR Link: N/A (Prompt 7)
- Implemented Prompt 7 from `BACKGROUND_SYNC_PLAN.md` by routing Overview/Collection/Plays manual refresh through `SyncManager` and exposing persisted sync state to those screens
- Added shared sync-state observer use cases and short sync-status text mapping so Overview, Collection, and Plays now render status from Room-backed sync rows instead of direct sync-call results
- Updated ViewModel and UI tests to assert enqueue-driven refresh behavior and persisted-sync-driven refreshing/status rendering
- Files changed:
  - `app/src/main/java/app/meeplebook/core/sync/domain/ObserveSyncStateUseCase.kt` (new)
  - `app/src/main/java/app/meeplebook/core/sync/domain/ObserveFullSyncStateUseCase.kt` (new)
  - `app/src/main/java/app/meeplebook/core/sync/SyncStatusText.kt` (new)
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewUiState.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewMappers.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewScreen.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewViewModel.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/CollectionUiState.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/CollectionScreen.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/CollectionViewModel.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysUiState.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysMappers.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysScreen.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysViewModel.kt`
  - `app/src/test/java/app/meeplebook/feature/overview/OverviewViewModelTest.kt`
  - `app/src/test/java/app/meeplebook/feature/collection/CollectionViewModelTest.kt`
  - `app/src/test/java/app/meeplebook/feature/plays/PlaysViewModelTest.kt`
  - `app/src/androidTest/java/app/meeplebook/feature/overview/OverviewContentTest.kt`
  - `AGENTS.md`
  - `progress.md`
- **Learnings for future iterations:**
  - Manual refresh should only enqueue work; user-visible refreshing and status text must come from observed persisted sync rows so navigation or process changes do not desynchronize the UI
  - Overview should derive its status from a combined full-sync observer, while Collection and Plays can observe their own `SyncType` rows directly
  - When testing persisted-sync-driven UI, mutate `FakeSyncTimeRepository` and wait for the derived `UiState` instead of asserting immediately on the current `StateFlow` value
---

## 2026-04-22T00:16:31.026+02:00
PR Link: N/A (Prompt 8)
- Implemented Prompt 8 from `BACKGROUND_SYNC_PLAN.md` by hardening sync observer/worker/manager coverage and polishing the related sync docs
- Added direct unit coverage for `ObserveSyncStateUseCase` and `ObserveFullSyncStateUseCase`, expanded worker androidTests for retryable network failures and logged-out collection runs, and asserted the periodic full-sync cadence in `WorkManagerSyncManagerTest`
- Refined sync observer KDoc and added a repo note that targeted connected worker tests are an acceptable verification path for sync worker changes
- Files changed:
  - `app/src/test/java/app/meeplebook/core/sync/domain/ObserveSyncStateUseCaseTest.kt` (new)
  - `app/src/test/java/app/meeplebook/core/sync/domain/ObserveFullSyncStateUseCaseTest.kt` (new)
  - `app/src/test/java/app/meeplebook/core/sync/manager/WorkManagerSyncManagerTest.kt`
  - `app/src/androidTest/java/app/meeplebook/core/sync/work/SyncCollectionWorkerTest.kt`
  - `app/src/androidTest/java/app/meeplebook/core/sync/work/SyncPlaysWorkerTest.kt`
  - `app/src/androidTest/java/app/meeplebook/core/sync/work/SyncPendingPlaysWorkerTest.kt`
  - `app/src/main/java/app/meeplebook/core/sync/domain/ObserveSyncStateUseCase.kt`
  - `app/src/main/java/app/meeplebook/core/sync/domain/ObserveFullSyncStateUseCase.kt`
  - `AGENTS.md`
  - `progress.md`
- **Learnings for future iterations:**
  - The sync observer use cases are worth testing directly because they encode behavior, not just delegation: full-sync status uses OR semantics for `isSyncing`, requires both timestamps before exposing a full-sync time, and keeps collection-first error precedence
  - Worker result mapping should be covered both in pure unit tests (`SyncWorkerResultMapperTest`) and in Hilt-backed worker tests so retry/failure/success behavior stays verified at the actual worker entrypoint
  - For sync worker verification on emulator, targeted `connectedDebugAndroidTest` runs scoped by `android.testInstrumentationRunnerArguments.class` are a practical repo-approved path when the changed surface is limited to a few worker test classes
---

## 2026-04-22T11:21:12+02:00
PR Link: N/A
- Repaired unit tests after the sync/overview refactor removed obsolete sync APIs and reducer fields
- Deleted stale tests for removed `ObserveLastFullSyncUseCase` and `SyncUserDataUseCase`, updated reducer/ViewModel tests to the current state model, and added a replacement `ObserveFullSyncStateUseCase` assertion that full-sync time stays null until both domains complete
- Refreshed repo guidance so the sync notes match the current trigger and sync-entrypoint structure
- Files changed:
  - `app/src/test/java/app/meeplebook/core/sync/FakeSyncTimeRepository.kt`
  - `app/src/test/java/app/meeplebook/core/sync/domain/ObserveFullSyncStateUseCaseTest.kt`
  - `app/src/test/java/app/meeplebook/feature/collection/CollectionViewModelTest.kt`
  - `app/src/test/java/app/meeplebook/feature/overview/OverviewViewModelTest.kt`
  - `app/src/test/java/app/meeplebook/feature/overview/reducer/OverviewReducerTest.kt`
  - `app/src/test/java/app/meeplebook/feature/plays/reducer/PlaysReducerTest.kt`
  - `app/src/test/java/app/meeplebook/core/sync/domain/ObserveLastFullSyncUseCaseTest.kt` (deleted)
  - `app/src/test/java/app/meeplebook/core/sync/domain/SyncUserDataUseCaseTest.kt` (deleted)
  - `progress.md`
- **Learnings for future iterations:**
  - When a refactor removes a sync API entirely, update or delete the obsolete tests instead of reintroducing dead type names; keep replacement coverage focused on the behavior that still exists
  - `OverviewViewModel` now schedules periodic sync and enqueues an immediate full sync in `init`, so tests using `FakeSyncManager` must account for an initial full-sync count before asserting manual refresh behavior
  - Collection screen tests should assert the current `CollectionCommonState` surface only; sync status text is no longer part of collection common UI state, but subtitle mapping still needs `FakeStringProvider`
---

## 2026-04-23T12:18:26+02:00
PR Link: N/A
- Fixed tests after the manual-refresh-indicator refactor and updated the refresh KDoc to match the new behavior
- Updated sync fakes and observer tests for the new `SyncManager.observeFullSyncRunning()` dependency, removed stale sync assertions from `ObserveOverviewUseCaseTest`, and rewrote Overview/Collection/Plays manual-refresh tests to keep an active `StateFlow` subscription while asserting the spinner lifecycle
- Added explicit coverage that background sync state alone must not auto-show pull-to-refresh, while user-initiated refresh does show and stays visible until the observed work completes
- Files changed:
  - `app/src/test/java/app/meeplebook/core/sync/manager/FakeSyncManager.kt`
  - `app/src/test/java/app/meeplebook/core/sync/domain/ObserveFullSyncStateUseCaseTest.kt`
  - `app/src/test/java/app/meeplebook/feature/overview/domain/ObserveOverviewUseCaseTest.kt`
  - `app/src/test/java/app/meeplebook/feature/overview/OverviewViewModelTest.kt`
  - `app/src/test/java/app/meeplebook/feature/collection/CollectionViewModelTest.kt`
  - `app/src/test/java/app/meeplebook/feature/plays/PlaysViewModelTest.kt`
  - `app/src/main/java/app/meeplebook/core/sync/model/SyncExtensions.kt`
  - `app/src/main/java/app/meeplebook/core/sync/manager/SyncManager.kt`
  - `app/src/main/java/app/meeplebook/core/sync/domain/ObserveFullSyncStateUseCase.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/CollectionBaseState.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysBaseState.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewBaseState.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/domain/ObserveOverviewUseCase.kt`
  - `AGENTS.md`
  - `progress.md`
- **Learnings for future iterations:**
  - Refresh-indicator tests for `WhileSubscribed` `StateFlow`s must keep an active collector during the entire refresh sequence; asserting `uiState.value` after the collector has stopped can miss state updates
  - Background sync may update status text, but pull-to-refresh spinners are manual-only UI state and should be tested separately from sync-status observers
  - `ObserveFullSyncStateUseCase` now reads `isSyncing` from `SyncManager.observeFullSyncRunning()` rather than inferring it from persisted domain sync rows, so fakes/tests must model both the work-running signal and the persisted timestamps/errors
---

## 2026-04-23T12:30:00+02:00
PR Link: N/A
- Fixed the androidTest sync-worker fake after the `SyncManager.observeFullSyncRunning()` interface change so instrumented tests compile against the current sync contract
- Verified the full androidTest suite now runs green without further refresh-indicator fallout
- Files changed:
  - `app/src/androidTest/java/app/meeplebook/core/sync/work/SyncWorkerTestDoubles.kt`
  - `progress.md`
- **Learnings for future iterations:**
  - When `SyncManager` adds a new method, update both unit-test and androidTest fakes; worker test doubles can return a simple inert flow when the new signal is irrelevant to the scenario under test
  - After fixing androidTest compile drift, rerun the full connected suite rather than stopping at compile success because interface updates can hide runtime wiring regressions
---

## 2026-04-23T13:12:00+02:00
PR Link: N/A
- Added a reusable Turbine helper for waiting on the next typed emission from an already-active collector and refactored the OverviewViewModel sync-status tests to use it
- Replaced the hand-rolled `do/while` loops in the Overview refresh/sync-status tests with `awaitItemMatching(...)` so the tests stay concise while preserving the required active subscription
- Files changed:
  - `app/src/test/java/app/meeplebook/testutils/TurbineExtensions.kt`
  - `app/src/test/java/app/meeplebook/feature/overview/OverviewViewModelTest.kt`
  - `progress.md`
- **Learnings for future iterations:**
  - Use `awaitUiStateMatching(...)` for one-shot `StateFlow` assertions, but use `ReceiveTurbine.awaitItemMatching(...)` when the collector must stay open across subsequent state changes
  - If a ViewModel test needs to observe a transition after triggering an event, prefer a reusable Turbine helper over repeating local `do/while` drains in each test
---

## 2026-04-24T12:45:00+02:00
PR Link: N/A
- Fixed the `observeRefreshCompletion` timeout regression that left manual refresh indicators stuck in Overview, Collection, and Plays tests after adding `withTimeoutOrNull`
- Moved the completion callback out of the `withTimeoutOrNull` block so both the normal true->false transition and the timeout fallback clear the refresh state, and added dedicated unit coverage for both paths
- Files changed:
  - `app/src/main/java/app/meeplebook/core/sync/model/SyncExtensions.kt`
  - `app/src/test/java/app/meeplebook/core/sync/model/SyncExtensionsTest.kt` (new)
  - `progress.md`
- **Learnings for future iterations:**
  - When adding `withTimeoutOrNull` around shared async helpers, keep post-timeout cleanup outside the timeout block or the fallback path will silently skip required state resets
  - Shared refresh helpers deserve focused unit tests for both the success path and the timeout fallback because a small coroutine refactor can break multiple screen tests at once
---

## 2026-04-24T13:20:00+02:00
PR Link: N/A
- Added a shared screen-entry sync safeguard so Collection and Plays only auto-sync on screen entry when their domain is not already syncing and has not synced successfully in the last 15 minutes
- Kept manual pull-to-refresh and Overview/periodic full-sync behavior unchanged, and added unit coverage for both the guard use case and the “manual refresh still enqueues even when auto-sync is skipped” behavior
- Files changed:
  - `app/src/main/java/app/meeplebook/core/sync/domain/ShouldAutoSyncOnScreenEnterUseCase.kt` (new)
  - `app/src/main/java/app/meeplebook/feature/collection/CollectionViewModel.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysViewModel.kt`
  - `app/src/test/java/app/meeplebook/core/sync/domain/ShouldAutoSyncOnScreenEnterUseCaseTest.kt` (new)
  - `app/src/test/java/app/meeplebook/feature/collection/CollectionViewModelTest.kt`
  - `app/src/test/java/app/meeplebook/feature/plays/PlaysViewModelTest.kt`
  - `AGENTS.md`
  - `progress.md`
- **Learnings for future iterations:**
  - Screen-entry sync throttling belongs in shared sync-domain logic, not duplicated inside each ViewModel, so the eventual settings-backed interval can swap in without touching trigger sites
  - Use persisted `SyncState.isSyncing` plus `lastSyncedAt` to gate navigation-triggered syncs, but keep manual refresh unguarded so the user can always force a sync immediately
---

## 2026-04-26T23:25:00+02:00
PR Link: N/A
- Extended the shared screen-entry sync safeguard to Overview full sync, so the immediate auto full-sync now skips when both Collection and Plays are recent and runs when either domain is stale
- Added OverviewViewModel coverage for both-domain-recent skip, one-domain-stale enqueue, and manual refresh still forcing a full sync, plus new `SyncTimeRepositoryImplTest` coverage for the added `getSyncState()` mapping/default behavior
- Files changed:
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewViewModel.kt`
  - `app/src/test/java/app/meeplebook/feature/overview/OverviewViewModelTest.kt`
  - `app/src/test/java/app/meeplebook/core/sync/domain/ShouldAutoSyncOnScreenEnterUseCaseTest.kt`
  - `app/src/test/java/app/meeplebook/core/sync/SyncTimeRepositoryImplTest.kt` (new)
  - `AGENTS.md`
  - `progress.md`
- **Learnings for future iterations:**
  - Full-sync auto-trigger decisions should be based on the combined freshness of Collection and Plays: run when either domain is stale, skip only when both are fresh, and skip while any domain is already syncing
  - When adding a new repository method that is part of sync orchestration, add focused repository-level tests for default/null-row mapping in addition to the higher-level use case and ViewModel coverage
---

## 2026-04-26T23:55:00+02:00
PR Link: N/A
- Moved Overview, Collection, and Plays screen-entry sync orchestration out of ViewModel `init` and into a new explicit `ScreenOpened` event dispatched by each screen Composable
- Added new `ScreenOpened` action/domain-effect wiring for the three features, updated the screen call sites to dispatch it with `LaunchedEffect`, and rewrote the ViewModel tests so auto-sync assertions are driven by explicit events rather than construction side effects
- Files changed:
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewScreen.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewEvent.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/effect/OverviewEffect.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/effect/OverviewEffectProducer.kt`
  - `app/src/main/java/app/meeplebook/feature/overview/OverviewViewModel.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/CollectionScreen.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/CollectionEvent.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/effect/CollectionEffect.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/effect/CollectionEffectProducer.kt`
  - `app/src/main/java/app/meeplebook/feature/collection/CollectionViewModel.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysScreen.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysEvent.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/effect/PlaysEffect.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/effect/PlaysEffectProducer.kt`
  - `app/src/main/java/app/meeplebook/feature/plays/PlaysViewModel.kt`
  - `app/src/test/java/app/meeplebook/feature/overview/OverviewViewModelTest.kt`
  - `app/src/test/java/app/meeplebook/feature/collection/CollectionViewModelTest.kt`
  - `app/src/test/java/app/meeplebook/feature/plays/PlaysViewModelTest.kt`
  - `AGENTS.md`
  - `progress.md`
- **Learnings for future iterations:**
  - Screen-entry work should be triggered by an explicit event from the Composable (`ScreenOpened`), not hidden in ViewModel initialization, so lifecycle behavior stays testable and intentional
  - When moving work out of `init`, rewrite count-based tests to dispatch the new event explicitly; manual-refresh tests can then assert only the work they trigger instead of inheriting setup side effects
---

## 2026-04-27T18:24:10+0200
PR Link: N/A
- Implemented Step 1 of the Play Timer plan: added the pure `ActivePlayTimer` model, elapsed-time derivation, and pure start/pause/resume/reset state-machine logic.
- Added focused unit coverage for transition behavior, initial-state no-ops, and negative-duration guarding.
- Files changed:
  - `app/src/main/java/app/meeplebook/core/timer/model/ActivePlayTimer.kt` (new)
  - `app/src/main/java/app/meeplebook/core/timer/domain/PlayTimerStateMachine.kt` (new)
  - `app/src/test/java/app/meeplebook/core/timer/domain/PlayTimerStateMachineTest.kt` (new)
- **Learnings for future iterations:**
  - The timer model needs an explicit `hasStarted` flag so the app can distinguish "never started" from a paused timer with `0` accumulated duration.
    - Keep `computeElapsed(timer, now)` as the single elapsed-time derivation path; state-machine transitions should reuse it instead of duplicating duration math.
    - Step 1 stays intentionally pure: no Room, service, or UI wiring should be mixed into the timer domain slice.
---
## 2026-04-27T18:38:34+0200
PR Link: N/A
- Implemented Step 2 of the Play Timer plan: added Room persistence for the singleton timer row plus the `TimerRepository` boundary and Room-backed implementation.
- Added repository-level unit coverage and a focused DAO androidTest for the timer row; the DAO test compiled successfully but could not execute in this environment because no device/emulator was connected.
- Files changed:
  - `app/src/main/java/app/meeplebook/core/database/entity/ActivePlayTimerEntity.kt` (new)
  - `app/src/main/java/app/meeplebook/core/database/dao/PlayTimerDao.kt` (new)
  - `app/src/main/java/app/meeplebook/core/database/MeepleBookDatabase.kt`
  - `app/src/main/java/app/meeplebook/core/database/DatabaseModule.kt`
  - `app/src/main/java/app/meeplebook/core/timer/TimerRepository.kt` (new)
  - `app/src/main/java/app/meeplebook/core/timer/TimerRepositoryImpl.kt` (new)
  - `app/src/main/java/app/meeplebook/core/timer/TimerModule.kt` (new)
  - `app/src/test/java/app/meeplebook/core/timer/TimerRepositoryImplTest.kt` (new)
  - `app/src/androidTest/java/app/meeplebook/core/database/dao/PlayTimerDaoTest.kt` (new)
- **Learnings for future iterations:**
    - Model the persisted timer as a singleton Room row keyed by a constant primary key, not as a history table or multi-row log.
    - Keep Room simple here: the DAO only needs observe/get/upsert because start/pause/resume/reset semantics belong in the repository and reuse the pure state machine.
    - Repository mutations should be serialized with a `Mutex` so timer transitions stay deterministic even if multiple callers hit the boundary close together.
---
