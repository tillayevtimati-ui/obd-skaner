plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "uz.avtoyordamchi.skaner"
    compileSdk = 34

    defaultConfig {
        applicationId = "uz.avtoyordamchi.skaner"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "0.3"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
