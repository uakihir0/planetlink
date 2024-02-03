plugins {
    kotlin("multiplatform") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
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
        val kotestVersion = "5.8.0"

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
