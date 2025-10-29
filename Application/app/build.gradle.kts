plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") // Apply Google Services plugin if you use Firebase in this module

}

android {
    namespace = "com.example.splashscreen"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.splashscreen"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

dependencies {
    // Don't know what these do

    implementation("androidx.annotation:annotation:1.6.0")

    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.core.splashscreen)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    //Location
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
// Use the latest stable version
    implementation(libs.play.services.location.v2101)
// Match this version if needed

    //Networking
    implementation(libs.retrofit)

    //Converter for parsing JSON responses into Java objects
    implementation(libs.converter.gson)


    // --- Coroutines (Recommended for modern Android concurrency) ---
// Since we are in a Fragment, using Coroutines is cleaner than AsyncTask or pure Executors
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
// LifecycleScope for Fragments
    implementation(libs.lifecycle.runtime.ktx)

    // Firebase dependencies
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

}
