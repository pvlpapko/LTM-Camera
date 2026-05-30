plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pvlpapko.lowlatencycam"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pvlpapko.lowlatencycam"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // RTSP/SRT low-latency encoder stack. RootEncoder uses Android MediaCodec hardware encoding.
    implementation("com.github.pedroSG94.RootEncoder:library:2.7.2")
    implementation("com.github.pedroSG94:RTSP-Server:1.4.1")

    // WebRTC dependency is included for the next step: WHIP/signaling integration.
    // The current Activity exposes RTSP server + RTSP/SRT push modes first.
    implementation("io.github.webrtc-sdk:android:144.7559.05")
}
