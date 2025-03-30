plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Plugin de Google Services para Firebase
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.jdlw1"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.jdlw1"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Firebase: Usa el BOM para gestionar las versiones de Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    // Firebase Auth
    implementation("com.google.firebase:firebase-auth")

    // [ANTIGUA] Google Play Services Auth (comentar o eliminar si usas One Tap):
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // [NUEVA] One Tap Sign-In
    //implementation("com.google.android.gms:play-services-auth-api-identity:21.2.0")

    // Compose
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.foundation:foundation:1.4.3")
    implementation("androidx.compose.material3:material3:1.1.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")

    // Coil para imágenes
    implementation("io.coil-kt:coil-compose:2.2.2")

    // Firebase Storage (si lo necesitas)
    implementation("com.google.firebase:firebase-storage-ktx")

    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")



    // [Opcional] Quita o comenta si no lo necesitas:
    // implementation("com.google.android.gms:play-services-auth:21.2.0")
    // implementation("com.google.android.gms:play-services-auth-api-identity:21.2.0")

    // ---------------------
    // Dependencias de tu archivo libs.versions.toml
    // ---------------------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.firestore.ktx)
    // ...

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Para usar APIs de Java 8+ en minSdk < 26
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
}
