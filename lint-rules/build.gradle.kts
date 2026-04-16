plugins {
    kotlin("jvm")
    id("java-library")
}

group = "app.meeplebook.lint"
version = "1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.lint.api)
    compileOnly(libs.lint.checks)

    testImplementation(libs.junit)
    testImplementation(libs.lint.api)
    testImplementation(libs.lint.checks)
    testImplementation(libs.lint.tests)
}