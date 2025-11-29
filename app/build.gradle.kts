import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

/**
 * Retrieves the BGG bearer token from local.properties or environment variable.
 * For local builds: add `bgg.bearer.token=YOUR_TOKEN` to local.properties
 * For CI builds: set BGG_BEARER_TOKEN environment variable (from GitHub Secrets)
 */
fun getBggBearerToken(): String {
    // First try local.properties
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        val properties = Properties().apply {
            localPropertiesFile.inputStream().use { load(it) }
        }
        properties.getProperty("bgg.bearer.token")?.let { return it }
    }
    // Fall back to environment variable (for CI builds)
    return System.getenv("BGG_BEARER_TOKEN") ?: ""
}

/**
 * Obfuscates a token using XOR with a random key.
 * This makes it harder to extract the token from a decompiled APK.
 * Returns a pair of (obfuscatedToken, key) as hex strings.
 */
fun obfuscateToken(token: String): Pair<String, String> {
    if (token.isEmpty()) return "" to ""
    val tokenBytes = token.toByteArray(Charsets.UTF_8)
    val keyBytes = ByteArray(tokenBytes.size)
    java.security.SecureRandom().nextBytes(keyBytes)
    val obfuscatedBytes = ByteArray(tokenBytes.size)
    for (i in tokenBytes.indices) {
        obfuscatedBytes[i] = (tokenBytes[i].toInt() xor keyBytes[i].toInt()).toByte()
    }
    return obfuscatedBytes.joinToString("") { "%02x".format(it) } to
           keyBytes.joinToString("") { "%02x".format(it) }
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

        // BGG Bearer Token - obfuscated for security
        val (obfuscatedToken, tokenKey) = obfuscateToken(getBggBearerToken())
        buildConfigField("String", "BGG_TOKEN_OBFUSCATED", "\"$obfuscatedToken\"")
        buildConfigField("String", "BGG_TOKEN_KEY", "\"$tokenKey\"")
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

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}