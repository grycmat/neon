plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.gigapingu.neon.feature.widget"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    // Glance composes on the Compose runtime; the compiler plugin is still required.
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
    implementation(projects.core.data)
    // NeonPalette (colors) + relativeTime/htmlToPlainText, and the shared Coil artifacts it `api`s.
    implementation(projects.core.designsystem)

    implementation(libs.glance.appwidget)

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
