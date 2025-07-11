import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Load local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

// Load environment variables first (for CI)
System.getenv().forEach { (key, value) ->
    if (key.startsWith("ORG_GRADLE_PROJECT_")) {
        val propertyKey = key.removePrefix("ORG_GRADLE_PROJECT_")
        localProperties.setProperty(propertyKey, value)
        project.extensions.extraProperties.set(propertyKey, value)
    }
}

// Handle GPG key from environment variable or file
val envGpgKey = System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey")
if (envGpgKey != null && envGpgKey.isNotBlank()) {
    // CI/CD: Use environment variable directly
    val cleanedKey = envGpgKey.trim()
    localProperties.setProperty("signingInMemoryKey", cleanedKey)
    project.extensions.extraProperties.set("signingInMemoryKey", cleanedKey)
    println("DEBUG: GPG key loaded from environment variable (length: ${cleanedKey.length})")
} else {
    // Local development: Read GPG key from file if specified
    val keyFile = localProperties.getProperty("signingInMemoryKeyFile")
    if (keyFile != null) {
        val keyFileHandle = rootProject.file(keyFile)
        if (keyFileHandle.exists()) {
            val keyContent = keyFileHandle.readText().trim()
            localProperties.setProperty("signingInMemoryKey", keyContent)
            project.extensions.extraProperties.set("signingInMemoryKey", keyContent)
            println("DEBUG: GPG key loaded from file: $keyFile (length: ${keyContent.length})")
        } else {
            println("DEBUG: GPG key file not found: $keyFile")
        }
    } else {
        println("DEBUG: No GPG key configured (for CI, set ORG_GRADLE_PROJECT_signingInMemoryKey)")
    }
}

// Add local properties to project properties
localProperties.forEach { key, value ->
    // Set as project extra properties
    project.extra.set(key.toString(), value)
    // Also set via extensions for vanniktech plugin
    project.extensions.extraProperties.set(key.toString(), value)
    
    // For vanniktech plugin, also set as system properties
    if (key.toString().startsWith("maven") || key.toString().startsWith("central") || key.toString().startsWith("signing")) {
        System.setProperty(key.toString(), value.toString())
    }
}

// Ensure critical properties are available as both project and system properties
val criticalProperties = listOf(
    "mavenCentralUsername",
    "mavenCentralPassword", 
    "sonatypeRepositoryId",
    "sonatypeAutomaticRelease",
    "signingInMemoryKeyId",
    "signingInMemoryKey",
    "signingInMemoryKeyPassword",
    "libraryVersion"
)

criticalProperties.forEach { propName ->
    val value = localProperties.getProperty(propName) ?: project.findProperty(propName) as String?
    if (value != null) {
        project.extra.set(propName, value)
        System.setProperty(propName, value)
        // Also set as gradle property for vanniktech plugin
        project.extensions.extraProperties.set(propName, value)
        if (propName == "signingInMemoryKey") {
            println("DEBUG: signingInMemoryKey is set (length: ${value.length})")
            println("DEBUG: First 50 chars: ${value.take(50)}")
        }
    } else if (propName == "signingInMemoryKey") {
        println("DEBUG: signingInMemoryKey is NOT set!")
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktechMavenPublish)
    alias(libs.plugins.dokka)
    signing
}

group = "io.github.hyochan"
version = localProperties.getProperty("libraryVersion") ?: "1.0.0-alpha02"

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

// Task to update README version
val updateReadmeVersion = tasks.register("updateReadmeVersion") {
    doLast {
        val version = localProperties.getProperty("libraryVersion") ?: "1.0.0-alpha04"
        val readmeFile = rootProject.file("README.md")
        val content = readmeFile.readText()
        val updatedContent = content.replace(
            Regex("implementation\\(\"io\\.github\\.hyochan:kmp-audio-recorder-player:.*\"\\)"),
            "implementation(\"io.github.hyochan:kmp-audio-recorder-player:$version\")"
        )
        readmeFile.writeText(updatedContent)
        println("Updated README.md with version: $version")
    }
}

// Automatically update README when publishing
tasks.withType<PublishToMavenRepository> {
    dependsOn(updateReadmeVersion)
}

mavenPublishing {
    // Explicitly use Central Portal instead of legacy Sonatype
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    
    // Re-enable vanniktech signing to use its built-in signing mechanism
    signAllPublications()
    
    coordinates("io.github.hyochan", "kmp-audio-recorder-player", localProperties.getProperty("libraryVersion") ?: "1.0.0-alpha02")
    
    // Configure publications with empty Javadoc JAR (Maven Central compatible)
    configure(
        com.vanniktech.maven.publish.KotlinMultiplatform(
            javadocJar = com.vanniktech.maven.publish.JavadocJar.Empty(),
            sourcesJar = true,
        )
    )
    
    pom {
        name.set("KMP Audio Recorder Player")
        description.set("A Kotlin Multiplatform library for audio recording and playback on Android, iOS, Desktop, and Web platforms")
        inceptionYear.set("2025")
        url.set("https://github.com/hyochan/kmp-audio-recorder-player")
        
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        
        developers {
            developer {
                id.set("hyochan")
                name.set("Hyo Chan Jang")
                email.set("hyo@hyo.dev")
                url.set("https://github.com/hyochan/")
                organization.set("hyochan")
                organizationUrl.set("https://github.com/hyochan")
            }
        }
        
        scm {
            connection.set("scm:git:git://github.com/hyochan/kmp-audio-recorder-player.git")
            developerConnection.set("scm:git:ssh://git@github.com/hyochan/kmp-audio-recorder-player.git")
            url.set("https://github.com/hyochan/kmp-audio-recorder-player")
            tag.set("v${localProperties.getProperty("libraryVersion") ?: "1.0.0-alpha02"}")
        }
        
        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/hyochan/kmp-audio-recorder-player/issues")
        }
    }
}

// Manual signing configuration (disabled in favor of vanniktech signing)
/*
signing {
    val signingKeyId: String? by project
    val signingPassword: String? by project
    
    // Try to read signing key from file if not available in properties
    val signingKey: String? = project.findProperty("signingKey") as String? ?: 
        rootProject.file("temp_private_key.asc").takeIf { it.exists() }?.readText()
    
    println("DEBUG: signingKeyId = ${signingKeyId?.substring(0, 8)}...")
    println("DEBUG: signingKey present = ${signingKey != null}")
    println("DEBUG: signingPassword present = ${signingPassword != null}")
    
    if (signingKeyId != null && signingKey != null) {
        println("DEBUG: Configuring in-memory PGP keys for signing")
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(publishing.publications)
    } else {
        println("DEBUG: Signing disabled - missing required properties")
    }
}
*/
