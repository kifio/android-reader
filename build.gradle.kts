plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.parcelize) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
