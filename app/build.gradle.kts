plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.comunamadsistemacitas"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.comunamadsistemacitas"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "1.3"

        // Configurable sin modificar el código fuente:
        // ./gradlew assembleDebug -PmarketplaceApiBaseUrl=http://10.0.2.2:8080
        val marketplaceApiBaseUrl = providers.gradleProperty("marketplaceApiBaseUrl")
            .orElse("http://10.0.2.2:8080")
            .get()

        buildConfigField("String", "MARKETPLACE_API_BASE_URL", "\"$marketplaceApiBaseUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["cleartextAllowed"] = "false"
    }

    buildTypes {
        debug {
            manifestPlaceholders["cleartextAllowed"] = "true"
        }
        release {
            manifestPlaceholders["cleartextAllowed"] = "false"
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.recyclerview)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
