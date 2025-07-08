plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-android")
    id("kotlin-kapt")
}


android {
    namespace = "com.countingfees.andckaps"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.countingfees.andckaps"
        minSdk = 24
        targetSdk = 34
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
    kotlinOptions {
        jvmTarget = "11"
    }
    viewBinding {
        enable = true
    }
}


dependencies {
    implementation(libs.com.github.bumptech.glide.glide2)

    annotationProcessor(libs.compiler)


    implementation("com.airbnb.android:lottie:5.2.0")
//ssp = sp
    implementation("com.intuit.ssp:ssp-android:1.0.6")

    //sdp = dp
    implementation("com.intuit.sdp:sdp-android:1.0.6")


    implementation("com.github.chrisbanes:PhotoView:2.3.0")


    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
// Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

// Circular images
    implementation("de.hdodenhof:circleimageview:3.1.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}