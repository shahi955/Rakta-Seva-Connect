/**
 * Paste into app/build.gradle.kts (or apply as convention plugin).
 *
 * Top-level build.gradle.kts must include:
 *   plugins { id("com.google.gms.google-services") version "4.4.2" apply false }
 * app/build.gradle.kts:
 *   plugins { id("com.google.gms.google-services") }
 */

dependencies {
    // Firebase BoM — keeps Firebase libs on compatible versions
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Coroutines + Play Services Task await()
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
}
