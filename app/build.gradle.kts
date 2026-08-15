plugins {
    id("com.android.application") version "8.2.0"
    id("org.jetbrains.kotlin.android") version "1.9.20"
}

android {
    namespace = "com.keepit"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.keepit"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
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
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}

configurations.all {
    resolutionStrategy {
        force(
            "androidx.core:core:1.12.0",
            "androidx.core:core-ktx:1.12.0",
            "androidx.appcompat:appcompat:1.6.1",
            "androidx.appcompat:appcompat-resources:1.6.1",
            "com.google.android.material:material:1.11.0",
            "androidx.constraintlayout:constraintlayout:2.1.4",
            "androidx.constraintlayout:constraintlayout-core:1.0.4",
            "androidx.transition:transition:1.4.1",
            "androidx.viewpager2:viewpager2:1.1.0",
            "androidx.fragment:fragment:1.6.2",
            "androidx.activity:activity:1.8.2",
            "androidx.lifecycle:lifecycle-viewmodel-savedstate:2.6.2",
            "androidx.savedstate:savedstate:1.2.1",
            "androidx.vectordrawable:vectordrawable:1.1.0",
            "androidx.vectordrawable:vectordrawable-animated:1.1.0",
            "androidx.drawerlayout:drawerlayout:1.1.1",
            "androidx.coordinatorlayout:coordinatorlayout:1.1.0",
            "androidx.recyclerview:recyclerview:1.3.2",
            "androidx.cardview:cardview:1.0.0"
        )
        preferProjectModules()
    }
}
