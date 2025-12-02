plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.snapitout"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.snapitout"
        minSdk = 24
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

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation ("com.google.mlkit:segmentation-selfie:16.0.0-beta3")

    implementation ("androidx.core:core-splashscreen:1.0.1")

    implementation ("androidx.camera:camera-core:1.2.3")
    implementation ("androidx.camera:camera-camera2:1.2.3")
    implementation ("androidx.camera:camera-lifecycle:1.2.3")
    implementation ("androidx.camera:camera-view:1.2.3")

    implementation ("androidx.recyclerview:recyclerview:1.3.0")
    implementation ("com.github.bumptech.glide:glide:4.15.1")

    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-firestore-ktx")

    kapt ("com.github.bumptech.glide:compiler:4.15.1")

    // CameraX (you probably already have these)
    implementation ("androidx.camera:camera-core:1.2.2")
    implementation ("androidx.camera:camera-camera2:1.2.2")
    implementation ("androidx.camera:camera-lifecycle:1.2.2")
    implementation ("androidx.camera:camera-view:1.2.2")


// (Optional) if you don't already have these core libs
    implementation ("com.google.android.material:material:1.9.0")
    implementation ("androidx.core:core-ktx:1.10.1")


    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.9.0")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.room:room-runtime:2.5.2")
    kapt("androidx.room:room-compiler:2.5.2")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.recyclerview:recyclerview:1.3.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    implementation(libs.play.services.auth)
    implementation(libs.firebase.auth)

    implementation(libs.androidx.cardview)
    implementation(libs.firebase.storage)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth.ktx)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.material3.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
}