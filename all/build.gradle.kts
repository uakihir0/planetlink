import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.cocoapods)
    alias(libs.plugins.swiftpackage)
    id("module.publications")
}

kotlin {
    jvmToolchain(11)
    jvm()

    js(IR) {
        nodejs()
        browser()
        binaries.library()
        compilerOptions {
            target.set("es2015")
            generateTypeScriptDefinitions()
        }
    }

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
            export(project(":misskey"))
            export(project(":mastodon"))
            export(project(":tumblr"))
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
        all {
            languageSettings.apply {
                optIn("kotlin.js.ExperimentalJsExport")
            }
        }

        commonMain.dependencies {
            api(project(":core"))
            api(project(":bluesky"))
            api(project(":misskey"))
            api(project(":mastodon"))
            api(project(":tumblr"))
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
            implementation(libs.slf4j.simple)
            implementation(libs.datetime)
            implementation(libs.serialization.json)
        }
    }
}

multiplatformSwiftPackage {
    swiftToolsVersion("5.7")
    targetPlatforms {
        iOS { v("15") }
        macOS { v("12.0") }
    }
}

tasks.configureEach {
    // Fix implicit dependency between XCFramework and FatFramework tasks
    if (name.contains("assemblePlanetlink") && name.contains("XCFramework")) {
        mustRunAfter(tasks.matching { it.name.contains("FatFramework") })
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

tasks.podPublishXCFramework {
    doLast {
        providers.exec {
            executable = "sh"
            args = listOf(project.projectDir.path + "/../tool/rename_podfile.sh")
        }.standardOutput.asText.get()
    }
}

afterEvaluate {
    tasks.withType<Kotlin2JsCompile>().configureEach {
        compilerOptions {
            target.set("es2015")
            freeCompilerArgs.add("-Xes-long-as-bigint")
        }
    }
}
