buildscript {
    dependencies {
        // AGP 9 ships built-in Kotlin with a minimum Kotlin Gradle plugin version.
        // Pin it explicitly so the Compose compiler plugin and Kotlin stay in sync.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
}
