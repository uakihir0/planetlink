import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    js {
        nodejs()
        browser()
        binaries.library()
        compilerOptions {
            target.set("es2015")
            generateTypeScriptDefinitions()
        }
    }

    // XCFramework（Swift Package / CocoaPods）に含めるアダプタの一覧。
    // ここを増減させたら、sourceSets の commonMain.dependencies も揃える。
    val exportedModules = listOf(
        "core",
        "bluesky",
        "misskey",
        "mastodon",
        "tumblr",
        "slack",
        "nostr",
        "matrix",
        "discord",
        "x",
        "mixi2",
        "saypip",
    )

    val xcf = XCFramework("planetlink")
    // x86_64 シミュレータは含めない（D-039）。全アダプタを入れると
    // fat バイナリが 145 MB になり、GitHub の 1 ファイル 100 MB 制限を超えるため。
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        macosArm64(),
    ).forEach {
        it.binaries.framework {
            exportedModules.forEach { module -> export(project(":$module")) }
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
        framework {
            // CocoaPods 側の framework にも export を付けないと、
            // multiplatform-swiftpackage が生成する XCFramework にアダプタが入らない。
            exportedModules.forEach { module -> export(project(":$module")) }
            baseName = "planetlink"
        }
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
            api(project(":slack"))
            api(project(":nostr"))
            api(project(":matrix"))
            api(project(":discord"))
            api(project(":x"))
            api(project(":mixi2"))
            api(project(":saypip"))
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
        macOS { v("12") }
    }
}

tasks.configureEach {
    // Fix implicit dependency between XCFramework and FatFramework tasks
    if (name.contains("assemblePlanetlink") && name.contains("XCFramework")) {
        mustRunAfter(tasks.matching { it.name.contains("FatFramework") })
    }
    // GitHub の 1 ファイル 100 MB 制限を避けるため、配布用 zip は作らない（D-039）。
    // Swift Package は xcframework を直接参照するので、zip は不要。
    if (name == "createZipFile") {
        enabled = false
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

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

afterEvaluate {
    tasks.withType<Kotlin2JsCompile>().configureEach {
        compilerOptions {
            target.set("es2015")
            freeCompilerArgs.add("-Xes-long-as-bigint")
            freeCompilerArgs.add("-Xenable-implementing-interfaces-from-typescript")
        }
    }
}
