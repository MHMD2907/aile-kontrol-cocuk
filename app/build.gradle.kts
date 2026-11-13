// sürüm: 0.3.0
import java.util.Base64

plugins {
id("com.android.application")
id("org.jetbrains.kotlin.android")
}

val keystoreFile = file("aile.jks")
if (!keystoreFile.exists()) {
val b64File = file("aile.jks.b64")
if (b64File.exists()) {
keystoreFile.writeBytes(Base64.getDecoder().decode(b64File.readText().trim()))
}
}

android {
namespace = "com.monur.ailekontrol"
compileSdk = 35

defaultConfig {
applicationId = "com.monur.ailekontrol"
minSdk = 24
targetSdk = 35
versionCode = 1
versionName = "0.1.0"
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
proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}
