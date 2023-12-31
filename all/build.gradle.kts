import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform") version "1.9.22"
    kotlin("native.cocoapods") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
}

kotlin {
    jvmToolchain(17)

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
        }
    }
}

tasks.podPublishXCFramework {
    doLast {
        exec {
            executable = "sh"
            args = listOf("../tool/rename_podfile.sh")
        }
    }
}
