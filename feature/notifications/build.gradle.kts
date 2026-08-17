import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Relay base URL is kept out of source control (see secrets.properties.example).
// Falls back to a placeholder / RELAY_BASE_URL env var so clean checkouts + CI still build.
// gms-only: the foss flavor's UnifiedPush transport posts straight to the distributor's own
// endpoint and has no relay to configure.
val relayBaseUrl: String = run {
    val secretsFile = rootProject.file("secrets.properties")
    val secrets = Properties().apply {
        if (secretsFile.exists()) secretsFile.inputStream().use { load(it) }
    }
    secrets.getProperty("RELAY_BASE_URL")
        ?: System.getenv("RELAY_BASE_URL")
        ?: "https://relay.example.com"
}

android {
    namespace = "com.gigapingu.neon.feature.notifications"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("gms") {
            dimension = "distribution"
            buildConfigField("String", "RELAY_BASE_URL", "\"$relayBaseUrl\"")
        }
        create("foss") {
            dimension = "distribution"
        }
    }

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
    implementation(projects.core.data)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)

    // FCM: the messaging service + endpoint provider for push delivery — gms only.
    "gmsImplementation"(platform(libs.firebase.bom))
    "gmsImplementation"(libs.firebase.messaging)

    // UnifiedPush: the messaging receiver + endpoint provider for push delivery — foss only.
    "fossImplementation"(libs.unifiedpush.connector)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    debugImplementation(libs.compose.ui.tooling)
}
