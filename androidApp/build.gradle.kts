plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "me.kifio.kreader.android"
    compileSdk = 33
    defaultConfig {
        applicationId = "me.kifio.kreader.android"
        minSdk = 23
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.0"
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.compose.ui:ui:1.2.1")
    implementation("androidx.compose.ui:ui-tooling:1.2.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.2.1")
    implementation("androidx.compose.foundation:foundation:1.2.1")
    implementation("androidx.compose.material:material:1.2.1")
    implementation("androidx.activity:activity-compose:1.6.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
//    implementation("com.github.readium.kotlin-toolkit:readium-shared:2.2.1")
//    implementation("com.github.readium.kotlin-toolkit:readium-streamer:2.2.1")
//    implementation("com.github.readium.kotlin-toolkit:readium-navigator:2.2.1")
//    implementation("com.github.readium.kotlin-toolkit:readium-opds:2.2.1")
//    implementation("com.github.readium.kotlin-toolkit:readium-lcp:2.2.1")
//    implementation(project(":folioreader-android"))
//    implementation("com.folioreader:folioreader:0.5.4")
}