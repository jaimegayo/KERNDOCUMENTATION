import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "es.iesagora.proyectointermodular"
    compileSdk = 36

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
    }

    defaultConfig {
        applicationId = "es.iesagora.proyectointermodular"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Configuración de Cloudinary desde local.properties
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${localProperties.getProperty("CLOUDINARY_CLOUD_NAME")}\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"${localProperties.getProperty("CLOUDINARY_API_KEY")}\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"${localProperties.getProperty("CLOUDINARY_UPLOAD_PRESET")}\"")
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
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.7.0")
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.core:core:1.12.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.joanzapata.iconify:android-iconify-fontawesome:2.2.2") {
        exclude(group = "com.android.support")
    }
    implementation("com.joanzapata.iconify:android-iconify-material:2.2.2") {
        exclude(group = "com.android.support")
    }
    implementation("com.github.prolificinteractive:material-calendarview:2.0.1")
    // Sprint 1: Identidad Visual
    implementation("com.cloudinary:cloudinary-android:2.2.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    // Sprint 2: Gráfico de Volumen
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}