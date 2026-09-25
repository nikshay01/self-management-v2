import java.util.Properties
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
}

// Load local.properties for API configuration
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
 
android {
    namespace = "com.selfmanagement.samsungbridge"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.selfmanagement.samsungbridge"
        minSdk = 29 // Android 10 — required by Samsung Health Data SDK
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Default API base URL (development)
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${localProperties.getProperty("API_BASE_URL_DEV", "http://10.0.2.2:3000")}\""
        )
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${localProperties.getProperty("API_BASE_URL_DEV", "http://10.0.2.2:3000")}\""
            )
        }

        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${localProperties.getProperty("API_BASE_URL", "https://api.selfmanagement.app")}\""
            )
        }
    }

    // Product flavors for environment targeting
    flavorDimensions += "environment"
    productFlavors {
        create("development") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${localProperties.getProperty("API_BASE_URL_DEV", "http://10.0.2.2:3000")}\""
            )
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${localProperties.getProperty("API_BASE_URL_STAGING", "https://staging-api.selfmanagement.app")}\""
            )
        }
        create("production") {
            dimension = "environment"
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${localProperties.getProperty("API_BASE_URL", "https://api.selfmanagement.app")}\""
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Samsung Health Data SDK — place the .aar in app/libs/
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))

    // AndroidX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // JSON serialization
    implementation("com.google.code.gson:gson:2.11.0")

    // HTTP client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Android Security (EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

    // Android Instrumented Testing
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
