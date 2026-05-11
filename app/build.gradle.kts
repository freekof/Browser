plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.freekof.tvbrowser"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.freekof.tvbrowser"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.zxing:core:3.5.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.4.4")
}
