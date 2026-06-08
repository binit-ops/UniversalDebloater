plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.debloater"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.debloater"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    // Shizuku API
    implementation("dev.rikka.shizuku:api:13.1.0")
    implementation("dev.rikka.shizuku:provider:13.1.0")
    
    // libsu for Root
    implementation("com.github.topjohnwu.libsu:core:5.2.2")
}

