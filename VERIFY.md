# VERIFY.md

> Note: Copilot agents cannot execute code locally.
> All verification is based on CI results.

## Purpose

This file defines how changes must be validated in the MeepleBook Android project.
Copilot agents and contributors **must follow these steps** before considering work complete.

---

## Project Overview

* App name: MeepleBook
* Package: app.meeplebook
* Language: Kotlin
* UI: Jetpack Compose
* Architecture: Offline-first, Room as source of truth
* DI: Hilt
* Tests are mandatory and enforced by CI

---

## Verification Rules (DO NOT SKIP)

### 1. Unit Tests (always required)

**Requirement:**
All CI jobs that execute unit tests must pass.

Relevant Gradle tasks:

```
./gradlew testDebugUnitTest
```

**For Copilot agents:**
You cannot run these tasks directly.
You must ensure that:

* CI is triggered for your changes
* The unit-test jobs complete successfully
* Any failures are fixed by updating production code or tests

If production code is changed, at least one unit test must exist that validates the new or modified behavior.

---

### 2. Instrumented Tests (when applicable)

**Requirement:**
All CI jobs that execute instrumented tests must pass.

Relevant Gradle task:

```
./gradlew connectedDebugAndroidTest
```

Required if changes affect:

* Compose UI
* ViewModels using Android components
* Room database
* Navigation
* Hilt modules

**For Copilot agents:**
Only rely on CI results. Do not assume correctness without CI confirmation.

---

### 3. Test Scope Rules

* Pure Kotlin logic → `src/test`
* Android, Compose, Room, Context → `src/androidTest`
* Do not move Android logic into unit tests to “make CI green”

---

### 4. Naming Conventions

All tests must follow:

Unit tests:

```
Use the Kotlin/JUnit backtick style with descriptive names.
Recommended pattern: methodUnderTest + context/condition + expected result
Example: fun `login with valid credentials returns success`()
```

UI tests:

```
subjectUnderTest_stateOrScenario_expectedOutcome
```

Avoid generic names like:

* `testSomething`
* `worksCorrectly`

---

### 5. Architectural Constraints

* UI must not perform IO
* ViewModels expose immutable UiState
* Repositories hide data source logic
* Room is the source of truth for all data.

Tests must respect these boundaries.

---

### 6. CI Awareness

If CI fails:

1. Read the failing task name
2. Fix the smallest scope possible
3. Do not modify unrelated modules

---

## Definition of Done

A change is complete when:

* All relevant tests pass
* New behavior is tested
* Architecture rules are preserved
* No dependency versions were changed without reason

---

## Notes for Copilot Agents

* Prefer fixing code over weakening tests
* Never delete tests to make CI pass
* When unsure, add a test that describes the intended behavior
