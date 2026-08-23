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
        versionCode = 11
        versionName = "1.6.0"
    }

    signingConfigs {
        create("fossRelease") {
            val keystorePath = (project.findProperty("FOSS_KEYSTORE_FILE") as String?) ?: System.getenv("FOSS_KEYSTORE_FILE")
            val keystoreFile = keystorePath?.let { file(it) }
            if (keystoreFile != null && keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = (project.findProperty("FOSS_KEYSTORE_PASSWORD") as String?) ?: System.getenv("FOSS_KEYSTORE_PASSWORD") ?: ""
                keyAlias = (project.findProperty("FOSS_KEY_ALIAS") as String?) ?: System.getenv("FOSS_KEY_ALIAS") ?: "neon-foss"
                keyPassword = (project.findProperty("FOSS_KEY_PASSWORD") as String?) ?: System.getenv("FOSS_KEY_PASSWORD") ?: ""
            } else {
                initWith(getByName("debug"))
            }
        }
        create("gmsRelease") {
            val keystorePath = (project.findProperty("GMS_KEYSTORE_FILE") as String?)
                ?: (project.findProperty("KEYSTORE_FILE") as String?)
                ?: System.getenv("GMS_KEYSTORE_FILE")
                ?: System.getenv("KEYSTORE_FILE")
            val keystoreFile = keystorePath?.let { file(it) }
            if (keystoreFile != null && keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = (project.findProperty("GMS_KEYSTORE_PASSWORD") as String?)
                    ?: (project.findProperty("KEYSTORE_PASSWORD") as String?)
                    ?: System.getenv("GMS_KEYSTORE_PASSWORD")
                    ?: System.getenv("KEYSTORE_PASSWORD")
                    ?: ""
                keyAlias = (project.findProperty("GMS_KEY_ALIAS") as String?)
                    ?: (project.findProperty("KEY_ALIAS") as String?)
                    ?: System.getenv("GMS_KEY_ALIAS")
                    ?: System.getenv("KEY_ALIAS")
                    ?: "neon-upload"
                keyPassword = (project.findProperty("GMS_KEY_PASSWORD") as String?)
                    ?: (project.findProperty("KEY_PASSWORD") as String?)
                    ?: System.getenv("GMS_KEY_PASSWORD")
                    ?: System.getenv("KEY_PASSWORD")
                    ?: ""
            } else {
                initWith(getByName("debug"))
            }
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("gms") {
            dimension = "distribution"
            signingConfig = signingConfigs.getByName("gmsRelease")
        }
        create("foss") {
            dimension = "distribution"
            applicationIdSuffix = ".foss"
            signingConfig = signingConfigs.getByName("fossRelease")
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

    lint {
        disable += "Instantiatable"
        checkReleaseBuilds = false
        abortOnError = false
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
