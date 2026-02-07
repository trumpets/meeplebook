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
## 2026-02-07T09:45:00Z
PR Link: https://github.com/trumpets/meeplebook/pull/TBD (Add Play Screen Implementation)
- **Task**: Implemented foundation for Add Play screen feature
- **Completed**:
  - Created domain models: NewPlay, NewPlayer, PlayerHistory, ColorHistory, GameSummary
  - Added DAO methods: getPlayerHistoryByLocation, getColorHistoryForGame, searchGamesForAutocomplete
  - Updated PlaysLocalDataSource, CollectionLocalDataSource with new methods
  - Updated PlaysRepository, CollectionRepository with new methods
  - Created use cases: SavePlayUseCase, ObservePlayerHistoryByLocationUseCase, ObserveColorHistoryForGameUseCase, SearchGamesForAutocompleteUseCase
  - Created UI State (AddPlayUiState) with Loading, Form, Saving, and Error states
  - Created Events (AddPlayEvent) for all user interactions
  - Designed comprehensive form state with game selection, player management, color picker, validation
- **Files changed**:
  - Domain models: NewPlay.kt, NewPlayer.kt, PlayerHistory.kt, ColorHistory.kt, GameSummary.kt
  - DAO updates: PlayDao.kt, CollectionItemDao.kt
  - Data source updates: PlaysLocalDataSource.kt, PlaysLocalDataSourceImpl.kt, CollectionLocalDataSource.kt, CollectionLocalDataSourceImpl.kt
  - Repository updates: PlaysRepository.kt, PlaysRepositoryImpl.kt, CollectionRepository.kt, CollectionRepositoryImpl.kt
  - Use cases: SavePlayUseCase.kt, ObservePlayerHistoryByLocationUseCase.kt, ObserveColorHistoryForGameUseCase.kt, SearchGamesForAutocompleteUseCase.kt
  - UI layer: AddPlayUiState.kt, AddPlayEvent.kt
- **Remaining work**:
  - ViewModel implementation with state management and business logic
  - Navigation integration (add routes, FAB on PlaysScreen)
  - Compose UI implementation (all form fields and components)
  - Comprehensive testing (unit tests for use cases, ViewModel tests, UI tests)
- **Learnings for future iterations:**
  - Add Play feature requires complex state management with nested player items
  - Player history and color history queries use GROUP BY and COUNT for aggregation
  - Form state uses string representations for numeric inputs (duration, score) for easier user input
  - Each player in form has temporary UUID for UI tracking before save
  - Auto-win detection and color history lookups will be implemented in ViewModel
  - SavePlayUseCase currently generates temporary negative IDs for local-only plays; future implementation needs BGG API POST
  - Game autocomplete searches collection using LIKE query on gameName field
---
