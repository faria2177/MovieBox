
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mboxpro.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mboxpro.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 16
        versionName = "2.1.0"
    }

    val hasReleaseKey = file("mboxpro.jks").let { it.exists() && it.length() > 0 } &&
        !System.getenv("KEYSTORE_PASSWORD").isNullOrBlank()

    signingConfigs {
        create("release") {
            storeFile = file("mboxpro.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Secrets না থাকলে debug key দিয়ে সাইন হবে, যাতে build fail না করে
            signingConfig = if (hasReleaseKey) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    // আলাদা APK: Android TV 32-bit (armeabi-v7a), 64-bit (arm64-v8a) এবং সব ডিভাইসের জন্য universal
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
