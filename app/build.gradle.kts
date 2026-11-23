import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Keystore tetap untuk menandatangani APK (disimpan sebagai teks base64 di app/signing/).
// Dengan kunci yang sama di setiap build, APK baru bisa dipasang menimpa versi lama (update).
// Tanpa ini, tiap build di GitHub Actions memakai kunci debug acak yang berbeda-beda.
val signingB64 = file("signing/txd-debug.keystore.b64")
val signingStore = layout.buildDirectory.file("signing/txd-debug.keystore").get().asFile
if (signingB64.exists()) {
    signingStore.parentFile.mkdirs()
    signingStore.writeBytes(Base64.getMimeDecoder().decode(signingB64.readText()))
}

android {
    namespace = "com.tutorialallgames.txdstudio"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.tutorialallgames.txdstudio"
        minSdk = 24; targetSdk = 35; versionCode = 3; versionName = "1.1.1"
    }
    signingConfigs {
        getByName("debug") {
            if (signingStore.exists()) {
                storeFile = signingStore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
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
