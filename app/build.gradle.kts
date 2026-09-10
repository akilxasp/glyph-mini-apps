plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.akil.glyphlife"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.akil.glyphlife"
        minSdk = 34            // Glyph Matrix SDK needs Android 14+
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // Any Glyph Matrix SDK .aar dropped in app/libs/ (version-agnostic).
    implementation(fileTree("libs") { include("*.aar") })
}
