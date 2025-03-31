plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.jdlw1"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.jdlw1"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3" // Usa la última versión disponible
    }
    buildFeatures {
        compose = true
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
    implementation(libs.play.services.wearable)

    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.tiles.tooling.preview)
    implementation(libs.androidx.ui.tooling.preview.android)
    implementation(libs.androidx.activity)

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
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

    implementation(libs.firebase.firestore.ktx)
    // ...

    // Testing

    // Para usar APIs de Java 8+ en minSdk < 26
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
    implementation("androidx.activity:activity-compose:1.8.0") // Asegura que esté presente
    implementation("androidx.compose.ui:ui:1.5.3")
    implementation("androidx.appcompat:appcompat:1.6.1")


}