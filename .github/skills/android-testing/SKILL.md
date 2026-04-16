---
name: android-testing
description: Comprehensive testing strategy involving Unit, Integration, Hilt, and Screenshot tests.
---

# Android Testing Strategies

This skill provides expert guidance on testing modern Android applications, inspired by "Now in Android". It covers **Unit Tests**, **Hilt Integration Tests**, and **Screenshot Testing**.

## MeepleBook repo note

Use the repo's current test stack as the source of truth:

1. **Unit tests**: `./gradlew testDebugUnitTest :lint-rules:test`
2. **Lint**: `./gradlew lint`
3. **Instrumented tests**: `./gradlew connectedDebugAndroidTest`

Placement in this repo:

- Pure Kotlin / ViewModel / reducer / repository tests live in `app/src/test`
- Compose UI and Android integration tests live in `app/src/androidTest`

Screenshot testing is **not configured yet** in this repo. If the user wants screenshot guidance, prefer **Paparazzi** as the default future direction. Mention **Roborazzi** only as an optional future user choice for a small number of critical screens, and only if/when the dependency has actually been added to the project.

## Testing Pyramid

1. **Unit Tests**: Fast, isolate logic (ViewModels, reducers, repositories).
2. **Integration Tests**: Test interactions (Room DAOs, Retrofit vs MockWebServer).
3. **UI / Instrumented Tests**: Verify Compose behaviour and Android integration.

## Dependencies (`libs.versions.toml`)

Ensure you have the right testing dependencies for the strategy you are actually using.

```toml
[libraries]
junit4 = { module = "junit:junit", version = "4.13.2" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version = "1.1.5" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version = "3.5.1" }
compose-ui-test = { group = "androidx.compose.ui", name = "ui-test-junit4" }
hilt-android-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hilt" }
```

## Screenshot Testing with Paparazzi (future / optional)

Screenshot tests ensure UI doesn't regress visually. For MeepleBook, prefer **Paparazzi** if and when screenshot testing is added.

> Do not assume screenshot tooling is already available in this repo.
> If the user explicitly wants **Roborazzi** for a handful of critical screens, treat that as an optional user decision and only document/use it once the dependency has actually been added.

### Setup

1. Add Paparazzi only when the user chooses to introduce screenshot testing.
2. Keep the initial scope small and focused on the most valuable screens.
3. If Roborazzi is later added for select critical screens, treat that as an explicit repo change, not as the default path.

### Writing a Screenshot Test

```kotlin
class MyScreenScreenshotTest {

    @Test
    fun captureMyScreen() {
        // Capture with the screenshot tool intentionally chosen for the repo.
    }
}
```

## Hilt Testing

Use `HiltAndroidRule` to inject dependencies in tests when the test actually needs DI wiring.

```kotlin
@HiltAndroidTest
class MyDaoTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: MyDatabase
    private lateinit var dao: MyDao

    @Before
    fun init() {
        hiltRule.inject()
        dao = database.myDao()
    }

    // ...
}
```

## Running Tests

* **Unit**: `./gradlew testDebugUnitTest :lint-rules:test`
* **Lint**: `./gradlew lint`
* **Instrumented**: `./gradlew connectedDebugAndroidTest`
