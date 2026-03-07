import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.samsung.android.seamless"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.samsung.android.seamless"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    defaultConfig {
        val secretsPropertiesFile = rootProject.file("secrets.properties")
        val secretsProperties = Properties().apply {
            if (secretsPropertiesFile.exists()) {
                load(secretsPropertiesFile.inputStream())
            }
        }

        // Resolve key with local file first, then Gradle property, then environment variable.
        val sarvamApiKey = secretsProperties.getProperty("sarvamApiKey")
            ?.takeIf { it.isNotBlank() }
            ?: (project.findProperty("sarvamApiKey") as? String)?.takeIf { it.isNotBlank() }
            ?: System.getenv("SARVAM_API_KEY").orEmpty()
        buildConfigField("String", "SARVAM_API_KEY", "\"$sarvamApiKey\"")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Jetpack Glance (App Widgets)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    //speech-to-text
    implementation(libs.okhttp) // Latest as of 2025
    implementation(libs.kotlinx.coroutines.android)

    implementation("com.airbnb.android:lottie-compose:6.4.1")

}
