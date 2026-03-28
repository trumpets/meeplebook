# AGENTS.md

## Project Snapshot
- Android app `app.meeplebook` built with Kotlin + Compose + Hilt; modules are `:app` and `:lint-rules` (`settings.gradle.kts`).
- App entry: `app/src/main/java/app/meeplebook/MainActivity.kt` decides start route from `AuthRepository.getCurrentUser()` and then mounts `AppNavHost`.
- Navigation uses typed routes (`composable<Screen.X>`) in `ui/navigation/AppNavHost.kt` and tabbed home navigation in `feature/home/navigation/HomeNavHost.kt`.

## Architecture That Matters
- Offline-first shape is implemented today: remote fetch -> local Room write -> UI observes flows from DB through repositories/use cases.
- Repositories are integration boundaries: `AuthRepositoryImpl`, `CollectionRepositoryImpl`, `PlaysRepositoryImpl` combine local/remote behavior and map exceptions to `AppResult` failures.
- Sync use cases (`core/sync/domain/*`) gate on logged-in user, then invoke repository sync and persist timestamps via `SyncTimeRepository`.
- Room is central (`core/database/MeepleBookDatabase.kt`): DAOs expose `Flow`, local data sources map entities <-> domain models.

## BGG Integration Patterns
- XML endpoints are in `core/network/BggApi.kt`; responses are parsed manually with XmlPull (`CollectionXmlParser`, `PlaysXmlParser`).
- Retry/backoff is centralized in `core/network/Retry.kt` and used by remote data sources for `202`, `429`, and `5xx`.
- Collection sync fetches boardgames and expansions separately with a 5s delay between calls (`CollectionRemoteDataSourceImpl`).
- Plays sync is paginated (100/page), delays 5s between pages, then reconciles deletions via `retainByRemoteIds` (`PlaysRepositoryImpl`).

## UI + State Conventions
- ViewModels expose immutable `StateFlow` and consume events (`CollectionViewModel.onEvent`, `PlaysViewModel.onEvent`).
- Search uses debounced flows (`searchableFlow` in `core/ui/flow/SearchableFlow.kt`, and direct debounce in collection).
- Avoid infinite wall-clock reactive flows with `while(true)+delay`; compute time-dependent values on demand (see `.github/copilot-instructions.md`).
- `UiText` is the app-level text abstraction (`core/ui/UiText.kt`); render via `UiTextText` or `asString()` helpers.

## Custom Lint Rules You Must Respect
- `:lint-rules` enforces two compile-time rules:
  - No passing `UiText` directly to Compose `Text` (`UiTextInTextComposableDetector`).
  - No passing `UiText` directly to `stringResource`/`pluralStringResource` (`UiTextInStringResourceDetector`).
- `:app` wires these checks through `lintChecks(project(":lint-rules"))` in `app/build.gradle.kts`.

## Security + Auth Details
- Credentials are currently stored encrypted in DataStore via Tink (`core/security/EncryptedPreferencesDataStore.kt`, `TinkAeadProvider.kt`).
- `OkHttpModule` always installs bearer, user-agent, and auth interceptors; bearer token comes from `BuildConfig.BGG_TOKEN`.
- `app/build.gradle.kts` reads token from `local.properties` (`bgg.bearer.token`) or env `BGG_BEARER_TOKEN`; release builds fail if missing.

## Testing + Verification Workflow
- Unit + lint baseline (matches CI):
  - `./gradlew testDebugUnitTest :lint-rules:test`
  - `./gradlew lint`
- Instrumented path used in CI when Android-affecting files change:
  - `./gradlew connectedDebugAndroidTest`
- Test placement and style in this repo:
  - Pure Kotlin/domain/viewmodel: `app/src/test` (fake-first, Turbine for flow assertions).
  - Room/Compose/Hilt/instrumented: `app/src/androidTest`.
- CI definitions to mirror locally: `.github/workflows/verify-unit.yml`, `.github/workflows/verify-full.yml`, `.github/workflows/master-build.yml`.

## Agent Working Agreement For This Repo
- Read `progress.md` before changes; append learnings after changes (do not rewrite prior entries).
- Do not change dependency/library versions unless explicitly requested.
- Prefer existing patterns over introducing new architecture variants; copy nearby feature/test structure first.

