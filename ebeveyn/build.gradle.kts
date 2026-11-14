// surum: 0.1.0 - ebeveyn uygulamasi
import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Cocuk uygulamasiyla ayni sabit imza anahtari
val keystoreFile = file("aile.jks")
if (!keystoreFile.exists()) {
    val b64File = file("../app/aile.jks.b64")
    if (b64File.exists()) {
        keystoreFile.writeBytes(Base64.getDecoder().decode(b64File.readText().trim()))
    }
}

val surumKodu = (System.getenv("VERSION_CODE") ?: "1").toInt()

android {
    namespace = "com.monur.aileebeveyn"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.monur.aileebeveyn"
        minSdk = 24
        targetSdk = 35
        versionCode = surumKodu
        versionName = "1.0.$surumKodu"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("aile") {
            storeFile = keystoreFile
            storePassword = "aile2026"
            keyAlias = "aile"
            keyPassword = "aile2026"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("aile")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("aile")
        }
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
}
