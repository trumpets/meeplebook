# Meeple Book

IMPORTANT: Always use Context7 MCP when you or I need library/API documentation, code generation, setup or configuration steps without me having to explicitly ask.

IMPORTANT: Don't change library version numbers. You don't have proper build tools access and you think the versions here are not even released. All the versions are proper.

This will be an Android app that will allow for a user to log in to BGG, view his collection, view his plays and record plays.

Those would be the initial functionalities.

It is separate than the existing BGG4Android app. That one is too old, not maintained. We fixed it a bit, but it is just a unnecessary large project.

The app will be written for native Android. Probably Kotlin and Compose although my 20y experience in Android is with Java and XML UI.

It is imperative to have a modern design (you come here a lot), AND TESTS. So many tests. In 10 years time I'll need to verify that changes aren't breaking. If TDD applies well, we'll do TDD.

Future proof as much as possible.

Initially only friends will use it but I will distribute it through Google Play (I already have an account). Need to figure out a unique package name and app name.

Decided on a name: MeepleBook
Decided on a package: app.meeplebook

## When implementing a task (in addition to the task instructions):

1. Read the progress log at `progress.md` (check Codebase Patterns section first)
2. Implement the task
3. Update AGENTS.md files if you discover reusable patterns (see below)
4. Append your progress to `progress.md`

Follow Google's NowInAndroid design and code and theming principles:

## 1 — Modules & folders (start single-module, plan multi-module)
Short-term (one Gradle module :app):

```
app/
src/main/java/app/meeplebook/
core/
model/             // shared data classes (Game, Play, User, AuthToken)
network/           // retrofit interfaces, xml parsers
database/          // Room entities, DAOs, Mappers
util/              // helpers, date utils, xml utils
feature/
login/
LoginScreen.kt
LoginViewModel.kt
LoginRepository.kt (interface)
LoginUiState.kt
collection/
CollectionScreen.kt
CollectionViewModel.kt
CollectionRepository.kt (interface)
CollectionUiState.kt
plays/
...
ui/
components/         // reusable composables (PrimaryButton, ListItem)
theme/              // MeepleBookTheme, color, typography
navigation/
AppNavHost.kt
Screen.kt
work/                 // WorkManager workers (sync)
App.kt                // @HiltAndroidApp
```

## 2 — Component responsibilities (concrete)
### UI / Feature layer
Composable screen: pure UI, no IO or heavy logic. Reads UiState and emits UiEvent.

LoginScreen(state: LoginUiState, onEvent: (LoginEvent)->Unit)

UiState: immutable data class representing all info the screen needs.

UiEvent: sealed class of user actions (object SubmitLogin, data class UsernameChanged(String)).

!!! IMPORTANT !!! No hardcoded strings throughout the app. Use string resources. Use stringResource(id = R.string.xxx) in composables. Don't resolve strings in ViewModel.

!!! IMPORTANT !!! No direct navigation calls in composables. Emit events to ViewModel.

!!! IMPORTANT !!! User facing dates (in UI) should follow EU format dd/MM/yyyy. Time is HH:MM 24h format.


### ViewModel
Exposes val uiState: StateFlow<UiState> (read-only).

Accepts events fun onEvent(e: UiEvent) and processes them (validate, call use-cases).

Uses viewModelScope for coroutine work.

Owns ephemeral UI-only flows (e.g., one-shot navigation via SharedFlow if needed).

### Repositories (interfaces in features or core, model classes are samples currently, check code)
AuthRepository: suspend fun login(username): Result<AuthToken>; fun currentUser(): Flow<User?>

CollectionRepository: fun observeCollection(): Flow<List<Game>>, suspend fun refreshCollection(), suspend fun saveGame(...)

PlaysRepository: similar

Repositories hide data source merging logic (Room + network).

### Data Sources
LocalDataSource: Room DAOs & Entities. Use Flow for queries.

RemoteDataSource: Retrofit (or OkHttp) clients. BGG provides XML — use Retrofit with a converter (Moshi works for JSON, for XML use SimpleXML or custom parsing). Use suspend methods.

### Mappers
DTO/Entity/Domain mappers: NetworkGameDto.toEntity() and Entity.toDomain().

### DI
Hilt modules in core/di provide Retrofit instance, OkHttp client, Room database, DAOs, repositories, coroutine dispatchers.

### Background sync
WorkManager used for periodic syncs. Worker calls CollectionRepository.refreshCollection() which fetches remote, writes to DB.

Use Constraints (unmetered network if large images).

### Security
For auth tokens or cookies: use EncryptedSharedPreferences or Android Keystore + symmetric key. Abstract via AuthLocalDataSource.

## 3 — Data flow patterns & rules
Source of truth: Room database for offline-first features (collection, plays). Remote sync writes to DB.

ViewModels observe Room (via Flow → .stateIn() or combine) and expose UiState.

Network for refreshes and write-through: repository triggers network, then updates DB.

Unidirectional flow:

UI emits events -> ViewModel -> Repository -> DataSource -> DB -> Flow emits -> ViewModel collects/write state -> UI

## 4 — Concrete sequences
### Login (simple username-based session)
UI: user types username -> LoginEvent.UsernameChanged.

ViewModel updates _uiState.update { copy(username = ...) }.

On submit: viewModelScope.launch { _uiState.update{loading=true}; val result = repo.login(username); if success -> save token in secure storage, update user in DB, navigate; else error }.

### Collection sync (auto + manual)
#### Auto (WorkManager):

Periodic Worker calls collectionRepo.refreshCollection() which fetches XML, parses items, upserts into Room.

#### Manual (pull-to-refresh):

UI event -> viewModelScope.launch -> collectionRepo.refreshCollection() -> updates DB -> UI automagically updates.

### Record a Play (local-first)
User fills play UI, taps Save.

ViewModel validates, calls playRepo.insertPlay(play).

Repository inserts into Room (local source of truth). Mark play syncState = PENDING.

Background sync worker picks up PENDING plays, tries to push to BGG if reachable, updates syncState -> SYNCED or FAILED. UI reads sync state from DB.

## 5 — Persistence: Room schema
DAOs return Flow<List<...>>.

## 6 — Networking: BGG XML specifics
BGG has XML endpoints. Use Retrofit with a converter:

Option A: Retrofit + Simple XML converter (if available/compatible).

Option B (recommended for control): Retrofit returning ResponseBody and parse with kotlinx-serialization-xml or a SAX/XmlPull parser to DTOs.

Keep the parsing isolated in core/network/parsers.

Implement rate limiting / caching.

## 7 — State management & concurrency
For UI state: MutableStateFlow in VM -> _uiState.update{...} -> val uiState = _uiState.asStateFlow().

For DB flows: use .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue) when converting to StateFlow for UI consumption if you need to keep a snapshot. Or collect DB Flow and update _uiState.

Use Dispatchers.IO for DB & network. Provide a @IoDispatcher via DI.

## 8 — DI & qualifiers (Hilt)
Hilt modules:

NetworkModule: provide OkHttp (logging, timeouts), Retrofit, BggApi

DatabaseModule: provide Room DB, DAOs

RepositoryModule: bind interfaces -> implementations

CoroutineModule: provide @IoDispatcher and @DefaultDispatcher

Mark ViewModels with @HiltViewModel and inject repositories.

## 9 — Error handling & UX rules
Map technical exceptions into domain errors; surface friendly messages in UI state.

Retry strategies for network; bubble offline status to UI.

Use Result<T> or sealed Either type from repository to differentiate success/failure.

AsyncImage whenever we have a remote URL.

## 10 — Testing strategy

Prefer fakes to Mockks!!! Only use Mockks for 3rd party classes. If a class can't be faked, abstract it to an interface so it can be!
When adding a new feature, examine the types of tests of similar existing features and replicate the strategy.

Use `assertTrue()` instead of `assert()`.

### Unit tests

ViewModel: provide fake repositories (pure Kotlin). Test state transitions. If ViewModel uses SharingStarted.WhileSubscribed() or other complex logic, use Turbine for StateFlow testing as
in the current ViewModel tests.

Repositories: test mappers & business rules.

### Instrumented tests

Room DAO tests (in-memory database).

### UI tests

Compose UI tests using createComposeRule.

### Integration

Repository + fake network server (MockWebServer) + in-memory DB.

### CI

GitHub Actions -> run ./gradlew test and connectedAndroidTest (or use emulator in CI). Also run lint & ktlint/detekt.

## 11 — CI / Release pipeline
### CI (GH Actions):

assembleDebug, unit tests, lint, ktlint, detekt

run Compose UI tests with emulator matrix or headless using android-emulator-runner

### CD

On release tag, build release AAB, sign with secrets stored in GitHub Secrets or Artifact Registry; optionally upload to Play.

## 12 — Security & storage
Tokens: use EncryptedSharedPreferences or Keystore-backed encryption.

Store minimal personal data locally; obfuscate sensitive logs.

## 13 — Metrics & analytics (optional)
Add optional pluggable analytics interface for events (not baked in core).

## 14 - AI Learnings

### Progress Report Format
After completing each PR, you must document your learnings to help future iterations. This is critical for maintaining and improving the codebase over time.

APPEND to progress.md (never replace, always append):

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

### Consolidate Patterns
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

### Update AGENTS.md Files
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

# ⚠️ Avoid Wall-Clock Reactive Flows (Lessons Learned)

## Problem

Do **not** model long-term wall-clock changes (e.g. year rollover, date boundaries) as infinite `Flow`s with `delay()` inside domain or ViewModel logic.

Example of **overkill**:

```kotlin
flow {
    while (true) {
        emit(currentYear)
        delay(untilNextYear)
    }
}
```

This pattern:

* Introduces **infinite flows**
* Requires **virtual time control** in tests
* Can silently **block or hang tests**
* Makes state propagation harder to reason about
* Solves an edge case affecting a **negligible number of users**

---

## Why This Is a Bad Trade-off

The only problem this solves is:

> “User keeps the app open across a year boundary and expects live UI updates at midnight.”

In real Android usage:

* Apps are backgrounded, restarted, or refreshed frequently
* Users accept stats updating on resume or interaction
* Midnight live updates are not expected behavior

The **complexity cost outweighs the UX benefit**.

---

## Recommended Approach

✔️ **Compute time-based values on demand**, not reactively:

```kotlin
val year = Year.now(clock)
val range = yearRangeFor(year)
```

Recalculate when:

* Data changes
* User refreshes
* Screen is re-entered
* App resumes
* Process restarts

This aligns with:

* Android lifecycle reality
* User expectations
* Testability
* Maintainable Flow graphs

---

## If Time-Boundary Handling Is Truly Required

Prefer **explicit lifecycle or system triggers**:

* Recalculate on `ON_RESUME`
* Daily `WorkManager` job
* Cache invalidation on date change

Avoid infinite `Flow { while(true) delay(...) }` patterns.

---

## Rule of Thumb

> **If a Flow needs `delay()` to model real time, it is probably the wrong abstraction.**

Boring, finite flows are good flows.
