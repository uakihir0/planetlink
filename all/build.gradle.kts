import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform") version "1.9.22"
    kotlin("native.cocoapods") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("maven-publish")
}

kotlin {
    jvmToolchain(17)
    jvm { withJava() }

    val xcf = XCFramework("planetlink")
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
        macosX64(),
        macosArm64(),
    ).forEach {
        it.binaries.framework {
            export(project(":core"))
            export(project(":bluesky"))
            baseName = "planetlink"
            xcf.add(this)
        }
    }

    cocoapods {
        name = "planetlink"
        version = "0.0.1"
        summary = "Planetlink is multi social media client for Kotlin Multiplatform."
        homepage = "https://github.com/uakihir0/planetlink"
        authors = "Akihiro Urushihara"
        license = "MIT"
        framework { baseName = "planetlink" }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core"))
            api(project(":bluesky"))
            implementation(libs.kbsky.core)
            implementation(libs.kbsky.stream)
        }

        // for test (kotlin/jvm)
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.junit5)
            implementation(libs.kotest.assertions)
            implementation(libs.serialization.json)
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

tasks.podPublishXCFramework {
    doLast {
        exec {
            executable = "sh"
            args = listOf("../tool/rename_podfile.sh")
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri("https://repo.repsy.io/mvn/uakihir0/public")
            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("PASSWORD")
            }
        }
    }
}

