plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.nubtam.badmintonscore"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.nubtam.badmintonscore"
        minSdk = 23
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0"

        val peerPackage = providers.gradleProperty("WEAR_PEER_PACKAGE").orElse("")
        val peerFingerprint = providers.gradleProperty("WEAR_PEER_FINGERPRINT").orElse("")
        buildConfigField("String", "WEAR_PEER_PACKAGE", "\"${peerPackage.get()}\"")
        buildConfigField("String", "WEAR_PEER_FINGERPRINT", "\"${peerFingerprint.get()}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("com.huawei.hms:wearengine:5.0.2.306")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

