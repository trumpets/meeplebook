import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

/**
 * Retrieves the BGG bearer token from local.properties or environment variable.
 * For local builds: add `bgg.bearer.token=YOUR_TOKEN` to local.properties
 * For CI builds: set BGG_BEARER_TOKEN environment variable (from GitHub Secrets)
 * Returns null when missing (caller decides whether it's required).
 */
fun getBggBearerTokenOrNull(): String? {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        val properties = Properties().apply {
            localPropertiesFile.inputStream().use { load(it) }
        }
        properties.getProperty("bgg.bearer.token")?.let { return it }
    }

    // Fall back to environment variable (for CI builds)
    return System.getenv("BGG_BEARER_TOKEN")?.takeIf { it.isNotBlank() }
}

android {
    namespace = "app.meeplebook"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "app.meeplebook"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        val token = getBggBearerTokenOrNull()
        val tokenForBuildConfig = token ?: ""
        buildConfigField("String", "BGG_TOKEN", "\"$tokenForBuildConfig\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        animationsDisabled = true
        unitTests.isReturnDefaultValues = true
    }

    room {
        schemaDirectory("$projectDir/schemas")
        generateKotlin = true
    }
}

kotlin {
    jvmToolchain(17) // recommended for Kotlin 2.2
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xcontext-receivers"
        )
    }
}

gradle.taskGraph.whenReady {
    val isReleaseBuild = allTasks.any {
        it.name.contains("Release", ignoreCase = true)
    }

    if (isReleaseBuild && getBggBearerTokenOrNull() == null) {
        throw GradleException(
            "Missing BGG token. Set `bgg.bearer.token` or env `BGG_BEARER_TOKEN`."
        )
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.navigation)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi.converter)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Security
    implementation(libs.tink.android)
    implementation(libs.androidx.datastore.preferences)

    // Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Image loading
    implementation(libs.coil)
    implementation(libs.coil.okhttp3)

    // Testing
    // Unit tests (src/test)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.room.testing)
    testImplementation(libs.xmlpull)
    testImplementation(libs.kxml2)
    testImplementation(libs.turbine)
    
    // Instrumented tests (src/androidTest)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test) // For runTest in androidTest
    androidTestImplementation(libs.room.testing) // For in-memory Room database in androidTest
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}