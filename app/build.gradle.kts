plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.tutorialallgames.txdstudio"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.tutorialallgames.txdstudio"
        minSdk = 24; targetSdk = 35; versionCode = 3; versionName = "1.1.1"
    }
    // Tanda tangan memakai keystore dari environment (diisi workflow lewat secret).
    // Kalau KEYSTORE_FILE kosong, build tetap jalan dengan kunci debug bawaan.
    val ksFile = System.getenv("KEYSTORE_FILE")
    val useKeystore = !ksFile.isNullOrEmpty()
    signingConfigs {
        if (useKeystore) {
            create("txd") {
                storeFile = file(ksFile!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        debug {
            if (useKeystore) signingConfig = signingConfigs.getByName("txd")
        }
        release {
            if (useKeystore) signingConfig = signingConfigs.getByName("txd")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
