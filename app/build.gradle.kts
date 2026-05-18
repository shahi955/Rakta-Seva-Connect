plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.raktaseva.connect"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.raktaseva.connect"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {


        val composeBom = platform("androidx.compose:compose-bom:2024.10.01")

        implementation(composeBom)
        androidTestImplementation(composeBom)

        // Core Android
        implementation("androidx.core:core-ktx:1.15.0")
        implementation("androidx.activity:activity-compose:1.9.3")

        // Lifecycle
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
        implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
        implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

        // Navigation
        implementation("androidx.navigation:navigation-compose:2.8.4")

        // Compose UI
        implementation("androidx.compose.ui:ui")
        implementation("androidx.compose.ui:ui-tooling-preview")

        // Material Design
        implementation("androidx.compose.material3:material3")
        implementation("androidx.compose.material:material-icons-extended")

        // Firebase BOM
        implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

        // Firebase Services
        implementation("com.google.firebase:firebase-auth-ktx")
        implementation("com.google.firebase:firebase-firestore-ktx")
        implementation("com.google.firebase:firebase-messaging-ktx")
        implementation("com.google.firebase:firebase-storage-ktx")

        // Coroutines
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

        // Location Services
        implementation("com.google.android.gms:play-services-location:21.3.0")

        // Debug
        debugImplementation("androidx.compose.ui:ui-tooling")
        debugImplementation("androidx.compose.ui:ui-test-manifest")
    }

