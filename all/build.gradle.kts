import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform") version "1.9.22"
    kotlin("native.cocoapods") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
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
        val kotestVersion = "5.8.0"

        commonMain.dependencies {
            api(project(":core"))
            api(project(":bluesky"))
            implementation("work.socialhub.kbsky:core:0.0.1-SNAPSHOT")
            implementation("work.socialhub.kbsky:stream:0.0.1-SNAPSHOT")
        }

        // for test (kotlin/jvm)
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
            implementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
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
