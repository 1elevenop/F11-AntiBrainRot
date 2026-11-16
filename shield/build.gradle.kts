plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.f11.shield"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.f11.shield"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // Must match F11's key: F11 only talks to a plugin signed by the same developer.
    signingConfigs {
        f11ReleaseSigning(rootProject)?.let { cfg ->
            create("release") {
                storeFile = cfg.storeFile
                storePassword = cfg.storePassword
                keyAlias = cfg.keyAlias
                keyPassword = cfg.keyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
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
    implementation(project(":core"))
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
}
