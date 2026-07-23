import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Relay base URL is kept out of source control (see secrets.properties.example).
// Falls back to a placeholder / RELAY_BASE_URL env var so clean checkouts + CI still build.
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
    namespace = "com.gigapingu.neon.core.data"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        buildConfigField("String", "RELAY_BASE_URL", "\"$relayBaseUrl\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.network)
    implementation(projects.core.database)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.datastore.preferences)
    implementation(libs.hilt.android)

    // Encrypted storage for the on-device Web Push private key + auth secret.
    implementation(libs.androidx.security.crypto)

    ksp(libs.hilt.compiler)
}
