plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
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
        viewBinding = true
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
    implementation("androidx.navigation:navigation-runtime-ktx:2.4.1")
    val compose_version = "1.2.1"
    val readium_version = "2.2.1"
    val room_version = "2.4.1"
    val lifecycle_version = "2.5.1"
    val paging_version = "3.1.1"
    val coil_version = "2.2.2"
    val nav_version = "2.5.3"

//    implementation(project(":shared"))
    implementation("androidx.compose.ui:ui:$compose_version")
    implementation("androidx.compose.ui:ui-tooling:$compose_version")
    implementation("androidx.compose.ui:ui-tooling-preview:$compose_version")
    implementation("androidx.compose.foundation:foundation:$compose_version")
    implementation("androidx.compose.material:material:$compose_version")
    implementation("androidx.activity:activity-compose:1.6.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.paging:paging-runtime:$paging_version")
    implementation("androidx.fragment:fragment-ktx:1.5.4")
    implementation("io.coil-kt:coil-compose:$coil_version")
    implementation("io.coil-kt:coil:$coil_version")
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle_version")
    implementation("com.github.readium.kotlin-toolkit:readium-shared:$readium_version")
    implementation("com.github.readium.kotlin-toolkit:readium-streamer:$readium_version")
    implementation("com.github.readium.kotlin-toolkit:readium-navigator:$readium_version")
    implementation("com.github.readium.kotlin-toolkit:readium-navigator-media2:$readium_version")
    implementation("com.github.readium.kotlin-toolkit:readium-opds:$readium_version")
    implementation("com.github.readium.kotlin-toolkit:readium-lcp:$readium_version")

    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    kapt("androidx.room:room-compiler:$room_version")
}