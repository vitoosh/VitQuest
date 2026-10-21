plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

// Food photo AI (see docs/AI_SETUP.md). Both values are public by design; empty disables the feature.
fun buildSetting(env: String, property: String): String =
    providers.environmentVariable(env).orElse(providers.gradleProperty(property)).getOrElse("").trim()
val supabaseUrl = buildSetting("VITQUEST_SUPABASE_URL", "vitquest.supabaseUrl")
val supabasePublishableKey = buildSetting("VITQUEST_SUPABASE_PUBLISHABLE_KEY", "vitquest.supabasePublishableKey")

// Optional shared debug keystore (see README "Debug signing"). When the
// environment variable is not set, AGP falls back to its default debug key.
val sharedDebugKeystore = providers.environmentVariable("VITQUEST_DEBUG_KEYSTORE").orNull
    ?.let(::file)
    ?.takeIf { it.isFile }

android {
    namespace = "io.github.vitoosh.vitquest"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.vitoosh.vitquest"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.environmentVariable("GITHUB_RUN_NUMBER").orNull?.toIntOrNull() ?: 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrl.replace("\"", "")}\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${supabasePublishableKey.replace("\"", "")}\"")
    }

    signingConfigs {
        getByName("debug") {
            if (sharedDebugKeystore != null) {
                storeFile = sharedDebugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            // R8 shrinking is on so CI proves the release build works. Signing is not configured
            // here: see docs/RELEASE.md. An unsigned release APK cannot be installed.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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

    androidResources {
        generateLocaleConfig = true
        // Only ship the languages VitQuest is translated into, so library strings stay consistent.
        localeFilters += listOf("en", "in")
    }

    // The language can be switched inside the app, so a future App Bundle must keep every language.
    bundle {
        language {
            enableSplit = false
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
        // Dependency updates are a deliberate decision, not a build failure.
        disable += setOf("NewerVersionAvailable", "GradleDependency", "AndroidGradlePluginVersion")
        // targetSdk stays on 36 deliberately; raising it is a separate, tested change.
        disable += "OldTargetApi"
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

// Restoring a backup made by an older version rebuilds that version's schema, then migrates it.
// Instrumented migration tests read the same files through MigrationTestHelper.
android.sourceSets.getByName("main").assets.directories.add("$projectDir/schemas")
android.sourceSets.getByName("androidTest").assets.directories.add("$projectDir/schemas")

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.health.connect.client)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.junit4.accessibility)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4.accessibility)
}
