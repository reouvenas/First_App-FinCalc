import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "reouven.first_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "reouven.first_app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // קריאת המפתח מקובץ local.properties לאבטחה
        val properties = Properties()
        val propertiesFile = project.rootProject.file("local.properties")
        if (propertiesFile.exists()) {
            properties.load(propertiesFile.inputStream())
        }
        val apiKey = properties.getProperty("GEMINI_API_KEY") ?: ""

        // יצירת השדה שישמש אותנו בקוד הג'אווה
        buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")
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

    // הפעלת תכונת BuildConfig (חובה כדי שהקוד למעלה יעבוד)
    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation("com.google.firebase:firebase-database:21.0.0")
    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation("com.google.firebase:firebase-firestore:25.1.1")
    implementation("com.google.android.gms:play-services-auth:20.4.1")
    implementation(libs.firebase.analytics)
    implementation(libs.activity)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
}