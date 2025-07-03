import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktechMavenPublish)
    signing
}

group = "io.github.hyochan"
version = "1.0.0-alpha01"

kotlin {
    jvm()
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64()
    
    wasmJs {
        binaries.executable()
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "io.github.hyochan.audio"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    
    // Disable signing for testing (Maven Central requires signing for production)
    // signAllPublications()
    
    coordinates("io.github.hyochan", "kmp-audio-recorder-player", "1.0.0-alpha01")
    
    pom {
        name.set("KMP Audio Recorder Player")
        description.set("A Kotlin Multiplatform library for audio recording and playback on Android, iOS, Desktop, and Web platforms")
        inceptionYear.set("2025")
        url.set("https://github.com/hyochan/kmp-audio-recorder-player")
        
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("https://opensource.org/licenses/MIT")
            }
        }
        
        developers {
            developer {
                id.set("hyochan")
                name.set("Hyo Chan Jang")
                url.set("https://github.com/hyochan/")
            }
        }
        
        scm {
            url.set("https://github.com/hyochan/kmp-audio-recorder-player")
            connection.set("scm:git:git://github.com/hyochan/kmp-audio-recorder-player.git")
            developerConnection.set("scm:git:ssh://git@github.com/hyochan/kmp-audio-recorder-player.git")
        }
    }
}
