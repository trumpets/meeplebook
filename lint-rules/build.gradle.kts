plugins {
    kotlin("jvm")
    id("java-library")
}

group = "app.meeplebook.lint"
version = "1.0"

dependencies {
    compileOnly(libs.lint.api)
    compileOnly(libs.lint.checks)

    testImplementation(libs.junit)
}