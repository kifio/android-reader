plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin)
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    namespace = "me.kifio.kreader.android"
    compileSdk = 34
    defaultConfig {
        applicationId = "me.kifio.kreader.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.2"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.9"
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildToolsVersion = "34.0.0"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val readium_version = "2.2.1"
    val room_version = "2.4.1"
    val lifecycle_version = "2.5.1"
    val paging_version = "3.1.1"
    val coil_version = "2.2.2"
    val insetter_version = "0.6.1"
    val activity_compose_version = "1.6.0"
    val fragment_ktx_version = "1.5.4"
    val material_version = "1.7.0"
    val coroutines_core_version = "1.6.0"
    val navigation_compose_version = "2.5.3"
    val splash_screen_version = "1.0.0"

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material)
    implementation("androidx.navigation:navigation-compose:$navigation_compose_version")
    implementation("androidx.activity:activity-compose:$activity_compose_version")
    implementation("androidx.paging:paging-runtime:$paging_version")
    implementation("androidx.fragment:fragment-ktx:$fragment_ktx_version")
    implementation("io.coil-kt:coil-compose:$coil_version")
    implementation("io.coil-kt:coil:$coil_version")
    implementation("com.google.android.material:material:$material_version")
    implementation("dev.chrisbanes.insetter:insetter:$insetter_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle_version")
    implementation("com.github.readium.kotlin-toolkit:readium-shared:$readium_version")
    implementation("com.github.readium.kotlin-toolkit:readium-streamer:$readium_version")
    implementation("com.github.readium.kotlin-toolkit:readium-navigator:$readium_version")
    implementation("com.github.readium.kotlin-toolkit:readium-lcp:$readium_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_core_version")
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    implementation("androidx.core:core-splashscreen:$splash_screen_version")
    kapt("androidx.room:room-compiler:$room_version")
}