---
name: android-gradle-logic
description: Expert guidance on setting up scalable Gradle build logic using Convention Plugins and Version Catalogs.
---

# Android Gradle Build Logic & Convention Plugins

## MeepleBook repo note

MeepleBook already uses `gradle/libs.versions.toml`, but it does **not** currently use a
`build-logic/` composite build or convention plugins. The source of truth today is:

- `settings.gradle.kts` with `:app` and `:lint-rules`
- direct plugin aliases in `app/build.gradle.kts`
- shared dependency/plugin versions in `gradle/libs.versions.toml`

Treat convention plugins as a future improvement only if the user explicitly wants build
reorganization.

This skill helps you configure a scalable, maintainable build system for Android apps using **Gradle Convention Plugins** and **Version Catalogs**, following the "Now in Android" (NiA) architecture.

## Goal
Stop copy-pasting code between `build.gradle.kts` files. Centralize build logic (Compose setup, Kotlin options, Hilt, etc.) in reusable plugins.

## Project Structure

If your project is ready for that level of build refactoring, use a `build-logic` directory included
in `settings.gradle.kts` as a composite build. Do not assume that structure already exists in
MeepleBook.

```text
root/
├── build-logic/
│   ├── convention/
│   │   ├── src/main/kotlin/
│   │   │   └── AndroidApplicationConventionPlugin.kt
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── app/
│   └── build.gradle.kts
└── settings.gradle.kts
```

## Step 1: Configure `settings.gradle.kts`

Include the `build-logic` as a plugin management source.

```kotlin
// settings.gradle.kts
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```

## Step 2: Define Dependencies in `libs.versions.toml`

Use the Version Catalog for both libraries *and* plugins.

```toml
[versions]
androidGradlePlugin = "<use the repo's current AGP version>"
kotlin = "<use the repo's current Kotlin version>"

[libraries]
# ...

[plugins]
android-application = { id = "com.android.application", version.ref = "androidGradlePlugin" }
android-library = { id = "com.android.library", version.ref = "androidGradlePlugin" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
# Define your own plugins here
nowinandroid-android-application = { id = "nowinandroid.android.application", version = "unspecified" }
```

## Step 3: Create a Convention Plugin

Inside `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt`:

```kotlin
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<ApplicationExtension> {
                defaultConfig.targetSdk = 34
                // Configure common options here
            }
        }
    }
}
```

Don't forget to register it in `build-logic/convention/build.gradle.kts`:

```kotlin
gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "nowinandroid.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
    }
}
```

## Usage

Apply your custom plugin in your modules (e.g., `app/build.gradle.kts`):

```kotlin
plugins {
    alias(libs.plugins.nowinandroid.android.application)
}
```

This drastically cleans up module-level build files.
