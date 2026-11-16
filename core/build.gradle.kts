plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.f11.core"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.05.00")
    api(composeBom)
    api("androidx.core:core-ktx:1.16.0")
    api("androidx.activity:activity-compose:1.10.1")
    api("androidx.compose.ui:ui")
    api("androidx.compose.material3:material3")

    testImplementation("junit:junit:4.13.2")
}
