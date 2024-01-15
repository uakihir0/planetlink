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
            implementation("io.ktor:ktor-client-core:2.3.7")
            implementation("work.socialhub.kbsky:core:0.0.1-SNAPSHOT")
            implementation("work.socialhub.kbsky:stream:0.0.1-SNAPSHOT")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
        }

        // for test (kotlin/jvm)
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
            implementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
        }
    }
}


tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
