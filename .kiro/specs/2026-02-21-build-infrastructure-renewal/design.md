# Design: Build Infrastructure Renewal

## Overview

This design modernizes PlanetLink's build infrastructure to align with the patterns established by sibling projects (kmisskey, kbsky, kmastodon, kslack). The changes are primarily in build configuration files, with no modifications to Kotlin source code beyond test dependency changes.

---

## File Change Map

| File | Operation | Implements |
|------|-----------|------------|
| `gradle/wrapper/gradle-wrapper.properties` | Edit | REQ-1 |
| `gradle/libs.versions.toml` | Rewrite | REQ-2 |
| `build.gradle.kts` | Rewrite | REQ-4, REQ-7, REQ-12 |
| `settings.gradle.kts` | Rewrite | REQ-6, REQ-12 |
| `gradle.properties` | Rewrite | REQ-8 |
| `plugins/build.gradle.kts` | Create | REQ-3 |
| `plugins/settings.gradle.kts` | Create | REQ-3 |
| `plugins/src/main/kotlin/module.publications.gradle.kts` | Create | REQ-3 |
| `plugins/src/main/kotlin/root.publications.gradle.kts` | Create | REQ-3 |
| `core/build.gradle.kts` | Rewrite | REQ-4, REQ-5, REQ-6 |
| `bluesky/build.gradle.kts` | Rewrite | REQ-4, REQ-5, REQ-6 |
| `misskey/build.gradle.kts` | Rewrite | REQ-4, REQ-5, REQ-6 |
| `mastodon/build.gradle.kts` | Rewrite | REQ-4, REQ-5, REQ-6 |
| `tumblr/build.gradle.kts` | Rewrite | REQ-4, REQ-5, REQ-6 |
| `all/build.gradle.kts` | Rewrite | REQ-5, REQ-9 |
| `.github/workflows/publish.yml` | Delete | REQ-10 |
| `.github/workflows/snapshot-publish.yml` | Create | REQ-10 |
| `.github/workflows/pods.yml` | Edit | REQ-10 |
| `Makefile` | Edit | REQ-10 |
| `AGENTS.md` | Create | REQ-11 |

---

## Detailed Design

### 1. Version Catalog (`gradle/libs.versions.toml`)

Replaces the current flat dependency listing with a structured version catalog using `[versions]` references, matching kmisskey/kbsky patterns.

```toml
[versions]
kotlin = "2.3.10"
dokka = "2.1.0"
maven-publish = "0.36.0"
serialization = "1.10.0"
coroutines = "1.10.2"

[libraries]
# Upstream platform libraries
kbsky-core = "work.socialhub.kbsky:core:0.1.0-SNAPSHOT"
kbsky-stream = "work.socialhub.kbsky:stream:0.1.0-SNAPSHOT"
kmisskey-core = "work.socialhub.kmisskey:core:0.1.0-SNAPSHOT"
kmisskey-stream = "work.socialhub.kmisskey:stream:0.1.0-SNAPSHOT"
kmastodon-core = "work.socialhub.kmastodon:core:0.1.0-SNAPSHOT"
kmastodon-stream = "work.socialhub.kmastodon:stream:0.1.0-SNAPSHOT"
ktumblr-core = "work.socialhub.ktumblr:core:0.0.1-SNAPSHOT"

# Shared utilities
khttpclient = "work.socialhub:khttpclient:0.0.8"
kmpcommon = "work.socialhub:kmpcommon:0.0.1-SNAPSHOT"

# Ktor
ktor-core = "io.ktor:ktor-client-core:3.4.0"

# KotlinX
datetime = "org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-0.6.x-compat"
coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
serialization-cbor = { module = "org.jetbrains.kotlinx:kotlinx-serialization-cbor", version.ref = "serialization" }

# HTML parsing
ksoup = "com.mohamedrejeb.ksoup:ksoup-html:0.4.0"

# Build plugins (used by plugins/ module)
nexus-publish = "io.github.gradle-nexus.publish-plugin:io.github.gradle-nexus.publish-plugin.gradle.plugin:2.0.0"
maven-publish = { module = "com.vanniktech:gradle-maven-publish-plugin", version.ref = "maven-publish" }
dokka = { module = "org.jetbrains.dokka:dokka-gradle-plugin", version.ref = "dokka" }

# Test
slf4j-simple = "org.slf4j:slf4j-simple:2.0.17"

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
kotlin-cocoapods = { id = "org.jetbrains.kotlin.native.cocoapods", version.ref = "kotlin" }
maven-publish = { id = "com.vanniktech.maven.publish", version.ref = "maven-publish" }
dokka = { id = "org.jetbrains.dokka", version.ref = "dokka" }
swiftpackage = "io.github.luca992.multiplatform-swiftpackage:2.3.0"
git-versioning = "me.qoomon.git-versioning:6.4.4"
```

**Design decisions:**
- `ktumblr-core` remains at `0.0.1-SNAPSHOT` since there may not be a newer version published
- `ksoup` version kept at `0.4.0` (already current)
- `kmpcommon` kept at `0.0.1-SNAPSHOT` (shared utility, check if newer exists)
- kotest libraries removed (replaced by `kotlin("test")`)

---

### 2. Plugin System Architecture

```
plugins/
├── build.gradle.kts           # kotlin-dsl plugin with build tool dependencies
├── settings.gradle.kts        # Version catalog bridge from parent project
└── src/main/kotlin/
    ├── module.publications.gradle.kts   # Per-module: Dokka + Maven + Repsy + POM
    └── root.publications.gradle.kts     # Root-only: Nexus/Sonatype publishing
```

#### `plugins/build.gradle.kts`

```kotlin
plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.nexus.publish)
    implementation(libs.maven.publish)
    implementation(libs.dokka)
}
```

#### `plugins/settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
```

#### `plugins/src/main/kotlin/module.publications.gradle.kts`

Based on kmisskey's pattern (`/Users/n3275/Documents/projects/planetlink/kmisskey/plugins/src/main/kotlin/module.publications.gradle.kts`):

```kotlin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform

plugins {
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
    id("com.vanniktech.maven.publish")
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

    publications.withType<MavenPublication> {
        pom {
            name.set("planetlink")
            description.set("Multi social media client library for Kotlin Multiplatform.")
            url.set("https://github.com/uakihir0/planetlink")

            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }

            developers {
                developer {
                    id.set("uakihir0")
                    name.set("URUSHIHARA Akihiro")
                    email.set("a.urusihara@gmail.com")
                }
            }

            scm {
                url.set("https://github.com/uakihir0/planetlink")
            }
        }
    }
}

mavenPublishing {
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml")
        )
    )

    if (project.hasProperty("mavenCentralUsername") ||
        System.getenv("ORG_GRADLE_PROJECT_mavenCentralUsername") != null
    ) signAllPublications()
}

signing {
    if (project.hasProperty("mavenCentralUsername") ||
        System.getenv("ORG_GRADLE_PROJECT_mavenCentralUsername") != null
    ) useGpgCmd()
}
```

#### `plugins/src/main/kotlin/root.publications.gradle.kts`

```kotlin
plugins {
    id("io.github.gradle-nexus.publish-plugin")
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}
```

---

### 3. Root `build.gradle.kts`

```kotlin
plugins {
    id("root.publications")

    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.kotlin.serialization).apply(false)
    alias(libs.plugins.kotlin.cocoapods).apply(false)

    alias(libs.plugins.dokka).apply(false)
    alias(libs.plugins.maven.publish).apply(false)

    alias(libs.plugins.git.versioning)
}

allprojects {
    group = "work.socialhub.planetlink"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.repsy.io/mvn/uakihir0/public") }
    }
}

gitVersioning.apply {
    refs {
        considerTagsOnBranches = true
        tag("v(?<version>.*)") {
            version = "\${ref.version}"
        }
    }
}

tasks.wrapper {
    gradleVersion = "9.3.1"
    distributionType = Wrapper.DistributionType.ALL
}
```

**Changes from current:**
- Added `root.publications` plugin
- Added `dokka`, `maven-publish` plugin declarations (apply false)
- Added `git-versioning` plugin with tag-based version extraction
- Removed `tasks.create("version")` (replaced by git-versioning)
- Updated Gradle version to 9.3.1

---

### 4. `settings.gradle.kts`

```kotlin
import java.util.Locale

pluginManagement {
    includeBuild("plugins")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "planetlink"

include("core")

// Each social media modules
include("bluesky")
include("misskey")
include("mastodon")
include("tumblr")

// exclude "all" on Windows OS
val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
if (!osName.contains("windows")) {
    include("all")
}

plugins {
    // To obtain an appropriate JVM environment in CI environments, etc.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
```

**Changes from current:**
- Added `pluginManagement { includeBuild("plugins") }` block
- Added repository configurations for plugin resolution
- Added OS-conditional `all` module inclusion (Windows exclusion)
- Added foojay resolver convention plugin

---

### 5. Module Build Files Pattern

All library modules (core, bluesky, misskey, mastodon, tumblr) follow this template:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("module.publications")
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    js(IR) {
        nodejs()
        browser()
    }

    if (HostManager.hostIsMac) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
        macosX64()
        macosArm64()
    }

    compilerOptions {
        freeCompilerArgs.add("-Xenable-suspend-function-exporting")
    }

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.js.ExperimentalJsExport")
            }
        }

        commonMain.dependencies {
            // Module-specific dependencies here
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
        }

        jvmTest.dependencies {
            implementation(libs.slf4j.simple)
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
```

**Module-specific `commonMain.dependencies`:**

| Module | Dependencies |
|--------|-------------|
| core | `ktor-core`, `khttpclient`, `datetime`, `coroutines-core`, `serialization-json` |
| bluesky | `project(":core")`, `ktor-core`, `kbsky-core`, `kbsky-stream`, `datetime` |
| misskey | `project(":core")`, `ktor-core`, `kmisskey-core`, `kmisskey-stream`, `kmpcommon`, `datetime` |
| mastodon | `project(":core")`, `ktor-core`, `kmastodon-core`, `kmastodon-stream`, `kmpcommon`, `datetime`, `ksoup` |
| tumblr | `project(":core")`, `ktor-core`, `ktumblr-core`, `kmpcommon`, `datetime`, `ksoup` |

---

### 6. `all/` Module Build File

```kotlin
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

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
```

**Changes from current:**
- Added `swiftpackage` plugin
- Added JS target with TypeScript definitions
- Replaced `jvmToolchain(17)` with `jvmToolchain(11)`
- Removed `jvm { withJava() }`
- Added `multiplatformSwiftPackage` configuration
- Added XCFramework/FatFramework task ordering fix
- Changed `exec` to `providers.exec` (Gradle 9.x compatible)
- Removed inline `publishing` block (handled by `module.publications`)
- Removed `kbsky-core`/`kbsky-stream` from `commonMain` (only needed in `bluesky` module)
- Test dependencies moved to `jvmTest` only (integration tests are JVM-only)

---

### 7. CI/CD Workflows

#### `.github/workflows/snapshot-publish.yml` (replaces `publish.yml`)

```yaml
name: Publish SNAPSHOT Package
on:
  push:
    branches:
      - main

jobs:
  build:
    runs-on: macos-latest

    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build with Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          arguments: build -x check

      - name: Publish to Repsy
        uses: gradle/actions/setup-gradle@v3
        with:
          arguments: publishAllPublicationsToMavenRepository -x check
        env:
          USERNAME: ${{ secrets.REPSY_USERNAME }}
          PASSWORD: ${{ secrets.REPSY_PASSWORD }}
```

#### `.github/workflows/pods.yml` (edit)

Change `-x test` to `-x check` in the build arguments.

---

### 8. Makefile

```makefile
build:
	./gradlew \
	core:clean \
	bluesky:clean \
	misskey:clean \
	mastodon:clean \
	tumblr:clean \
	all:clean all:build \
	-x check --refresh-dependencies

pods:
	./gradlew \
	all:assemblePlanetlinkXCFramework \
	all:podPublishXCFramework \
	-x check --refresh-dependencies

version:
	 ./gradlew version --no-daemon --console=plain -q

.PHONY: build pods version
```

---

### 9. AGENTS.md

Create at project root. Content based on the established patterns from kmisskey/kbsky/kmastodon/kslack, adapted for PlanetLink's multi-SNS abstraction architecture. Will include:

- Project overview and purpose
- Architecture (core abstraction + platform adapter pattern)
- Directory structure with module descriptions
- Testing instructions (`./gradlew :all:jvmTest`, `./gradlew jvmJar`, specific module tests)
- Implementation guidelines for adding new SNS platform support
- Naming conventions (Action, Mapper, Auth patterns per platform)
- Key file references table
- Authentication setup (secrets.json)
- Build and publish commands

---

## Implementation Order

1. Create `plugins/` directory and all plugin files
2. Update `gradle/libs.versions.toml`
3. Update `gradle.properties`
4. Update `settings.gradle.kts`
5. Update root `build.gradle.kts`
6. Update `gradle/wrapper/gradle-wrapper.properties`
7. Run `./gradlew wrapper` to download new Gradle
8. Update `core/build.gradle.kts`
9. Update `bluesky/build.gradle.kts`
10. Update `misskey/build.gradle.kts`
11. Update `mastodon/build.gradle.kts`
12. Update `tumblr/build.gradle.kts`
13. Update `all/build.gradle.kts`
14. Update `.github/workflows/` (delete publish.yml, create snapshot-publish.yml, edit pods.yml)
15. Update `Makefile`
16. Create `AGENTS.md`
17. Verify: `./gradlew build -x check`
18. Verify: `./gradlew jvmJar`

---

## Verification Plan

1. **Gradle wrapper**: `./gradlew --version` reports 9.3.1
2. **Full build**: `./gradlew build -x check` succeeds for all modules
3. **JVM JAR**: `./gradlew jvmJar` generates JARs
4. **JVM tests**: `./gradlew jvmTest` passes (non-authenticated tests only)
5. **Makefile**: `make build` succeeds
6. **Plugin resolution**: No duplicate publishing blocks, `module.publications` resolves correctly
