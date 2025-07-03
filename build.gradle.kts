plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.vanniktechMavenPublish) apply false
    id("org.jetbrains.dokka") version "1.9.20"
}
