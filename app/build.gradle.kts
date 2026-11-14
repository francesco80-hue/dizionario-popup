plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "it.francesco.dizionariopopup"
    compileSdk = 35
    defaultConfig {
        applicationId = "it.francesco.dizionariopopup"
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
    }
    signingConfigs {
        create("release") {
            val signingPassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
            storeFile = file("release-key.jks")
            storePassword = signingPassword
            keyAlias = "dizionario-popup"
            keyPassword = signingPassword
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
}
