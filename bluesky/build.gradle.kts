plugins {
    kotlin("multiplatform") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    id("maven-publish")
}

kotlin {
    jvmToolchain(17)

    jvm { withJava() }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(libs.ktor.core)
            implementation(libs.kbsky.core)
            implementation(libs.kbsky.stream)
            implementation(libs.datetime)
        }

        // for test (kotlin/jvm)
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.junit5)
            implementation(libs.kotest.assertions)
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
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
