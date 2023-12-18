import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform") version "1.9.21"
    kotlin("native.cocoapods") version "1.9.21"
}

group = "work.socialhub"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)

    jvm { withJava() }

    val xcf = XCFramework()
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
        macosX64(),
        macosArm64(),
    ).forEach {
        it.binaries.framework {
            xcf.add(this)
        }
    }

    cocoapods {
        version = "1.0"
        summary = "PlanetLink is social media client library for Kotlin Multiplatform."
        homepage = "https://www.uakihir0.com/socialhub/"

    }

    sourceSets {
        val ktorVersion = "2.3.7"
        val kotestVersion = "5.8.0"

        commonMain.dependencies {
            implementation("io.ktor:ktor-client-core:$ktorVersion")
            implementation("io.ktor:ktor-client-json:$ktorVersion")
        }

        // for Apple platform
        appleMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:$ktorVersion")
        }

        // for JVM
        jvmMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
        }

        // for test (kotlin/jvm)
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
            implementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
        }
    }
}

tasks.podPublishXCFramework {
    doLast {
        exec {
            executable = "sh"
            args = listOf("./tool/rename_podfile.sh")
        }
    }
}

tasks.wrapper {
    gradleVersion = "8.5"
    distributionType = Wrapper.DistributionType.ALL
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
