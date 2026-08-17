import com.google.gms.googleservices.GoogleServicesPlugin

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

// google-services.json only lives under src/gms (see below) — it has one client entry for the
// unsuffixed gms applicationId, which would never match the foss flavor's applicationIdSuffix
// even if the file were visible to it. WARN (not the default ERROR) lets every foss variant, and
// a from-source (F-Droid/IzzyOnDroid) or gms-without-secrets checkout, configure and build
// cleanly with nothing found for that variant, instead of failing with "No matching client" /
// "File google-services.json is missing".
googleServices {
    missingGoogleServicesStrategy = GoogleServicesPlugin.MissingGoogleServicesStrategy.WARN
}

android {
    namespace = "com.gigapingu.neon"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gigapingu.neon"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "1.4.0"
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("gms") {
            dimension = "distribution"
        }
        create("foss") {
            dimension = "distribution"
            applicationIdSuffix = ".foss"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

composeCompiler {
    stabilityConfigurationFile = rootProject.layout.projectDirectory.file("compose_stability_config.conf")
}

configurations.configureEach {
    val tink = "com.google.crypto.tink:tink-android:1.20.0"
    resolutionStrategy {
        force(tink)
        dependencySubstitution {
            substitute(module("com.google.crypto.tink:tink")).using(module(tink))
        }
    }
}


kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.feature.auth)
    implementation(projects.feature.timeline)
    implementation(projects.feature.explore)
    implementation(projects.feature.notifications)
    implementation(projects.feature.messages)
    implementation(projects.feature.thread)
    implementation(projects.feature.composer)
    implementation(projects.feature.profile)
    implementation(projects.feature.settings)
    implementation(projects.feature.widget)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // For the app-wide ImageLoader (crossfade) built in NeonApplication.
    implementation(libs.coil.compose)

    // In-app updates via Play (see update/PlayAppUpdateController.kt) — gms only.
    "gmsImplementation"(libs.play.app.update)
    "gmsImplementation"(libs.play.app.update.ktx)

    debugImplementation(libs.compose.ui.tooling)

}
