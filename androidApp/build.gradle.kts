plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.parcelize)
}

android {
    val target_sdk = 34
    val compile_sdk = 34
    val min_sdk = 26
    val version_code = 4
    val version_name = "1.0.4"
    val kotlin_compiler_extension_version = "1.5.9"
    val jvm_target = "11"
    val build_tools_version = "34.0.0"

    namespace = "me.kifio.kreader.android"
    compileSdk = compile_sdk
    defaultConfig {
        applicationId = "me.kifio.kreader.android"
        minSdk = min_sdk
        targetSdk = target_sdk
        versionCode = version_code
        versionName = version_name
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = kotlin_compiler_extension_version
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
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = jvm_target
    }
    buildToolsVersion = build_tools_version
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.coil.compose)
    implementation(libs.coil)
    implementation(libs.material)
    implementation(libs.insetter)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.readium.shared)
    implementation(libs.readium.streamer)
    implementation(libs.readium.navigator)
    implementation(libs.readium.lcp)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.core.splashscreen)
    ksp(libs.androidx.room.compiler)
}