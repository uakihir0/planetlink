# Requirements: Build Infrastructure Renewal

## Background

PlanetLink is a multi-platform SNS client library for Kotlin Multiplatform that provides a unified abstraction layer over multiple social media platforms (Bluesky, Misskey, Mastodon, Tumblr). The project's build infrastructure has fallen significantly behind its sibling projects (kmisskey, kbsky, kmastodon, kslack), which serve as the reference implementations for modern Kotlin Multiplatform build patterns.

### Current State

- **Gradle**: 8.5 (reference projects use 8.14.3 or 9.3.1)
- **Kotlin**: 2.0.10 (reference projects use 2.3.0 - 2.3.10)
- **No plugin system**: Publishing configuration is duplicated across every module
- **No JS target**: Only JVM + iOS + macOS are supported
- **No Dokka**: No API documentation generation
- **No git-versioning**: Versions are managed manually
- **No Swift Package support**: Only CocoaPods for Apple platforms
- **Missing build optimizations**: No parallel builds, no incremental compilation settings
- **No AGENTS.md**: No agent documentation for AI-assisted development

### Reference Projects

| Project | Gradle | Kotlin | Plugin System | JS | Dokka | git-versioning |
|---------|--------|--------|---------------|-----|-------|----------------|
| kmisskey | 8.14.3 | 2.3.10 | Yes | Yes | Yes | Yes |
| kbsky | 9.3.1 | 2.3.10 | Yes | Yes | Yes | Yes |
| kmastodon | 8.14.3 | 2.3.0 | Yes | Yes | Yes | Yes |
| kslack | 9.3.1 | 2.3.10 | Yes | Yes | Yes | Yes |
| **planetlink** | **8.5** | **2.0.10** | **No** | **No** | **No** | **No** |

---

## Requirements

### REQ-1: Gradle Wrapper Upgrade

**Priority**: Critical

Upgrade the Gradle wrapper from 8.5 to 9.3.1 to match the latest reference projects (kbsky, kslack).

**Acceptance Criteria**:
- [ ] `gradle-wrapper.properties` references `gradle-9.3.1-all.zip`
- [ ] `build.gradle.kts` wrapper task specifies `gradleVersion = "9.3.1"`
- [ ] `./gradlew --version` reports Gradle 9.3.1

---

### REQ-2: Kotlin and Dependency Version Upgrade

**Priority**: Critical

Upgrade Kotlin from 2.0.10 to 2.3.10 and update all dependencies to their latest compatible versions aligned with reference projects.

**Acceptance Criteria**:
- [ ] Kotlin version is 2.3.10
- [ ] Ktor upgraded from 2.3.12 to 3.4.0
- [ ] kotlinx-datetime upgraded to 0.7.1-0.6.x-compat
- [ ] kotlinx-coroutines upgraded to 1.10.2
- [ ] kotlinx-serialization upgraded to 1.10.0
- [ ] khttpclient upgraded from 0.0.1-SNAPSHOT to 0.0.8
- [ ] Upstream library references (kbsky, kmisskey, kmastodon) updated to 0.1.0-SNAPSHOT
- [ ] All dependencies declared using version catalog references where applicable
- [ ] New dependencies added: dokka, maven-publish, nexus-publish, slf4j-simple, coroutines-test, swiftpackage plugin, git-versioning plugin
- [ ] kotest replaced with `kotlin("test")` + `coroutines-test` (matching reference projects)

---

### REQ-3: Plugin System Introduction

**Priority**: Critical

Introduce a `plugins/` module with precompiled Gradle script plugins (`module.publications` and `root.publications`) to eliminate duplicated publishing configuration across all modules.

**Acceptance Criteria**:
- [ ] `plugins/` directory exists with `build.gradle.kts`, `settings.gradle.kts`, and two precompiled plugins
- [ ] `module.publications.gradle.kts` handles Dokka, Maven Publish (vanniktech), Repsy repository, POM metadata, and conditional signing
- [ ] `root.publications.gradle.kts` handles Nexus Publishing (Sonatype) configuration
- [ ] All modules use `id("module.publications")` instead of inline `publishing {}` blocks
- [ ] Root `build.gradle.kts` uses `id("root.publications")`
- [ ] No publishing configuration is duplicated in individual module build files

---

### REQ-4: JVM Target Standardization

**Priority**: High

Standardize JVM target from 17 to 11, matching reference projects. Use explicit compiler options instead of `jvmToolchain`.

**Acceptance Criteria**:
- [ ] All modules use `jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }`
- [ ] `jvm { withJava() }` replaced with explicit JVM compiler options
- [ ] `jvmToolchain(17)` removed from all modules (except `all/` module which may use `jvmToolchain(11)`)

---

### REQ-5: JavaScript Target Support

**Priority**: High

Add JS (IR) target to all modules, matching reference project patterns.

**Acceptance Criteria**:
- [ ] All modules (core, bluesky, misskey, mastodon, tumblr) include `js(IR) { nodejs(); browser() }`
- [ ] `all/` module includes JS with library binaries and TypeScript definition generation
- [ ] `-Xenable-suspend-function-exporting` compiler flag is set
- [ ] `kotlin.js.ExperimentalJsExport` opt-in is configured

---

### REQ-6: Conditional Native Target Compilation

**Priority**: Medium

Make iOS/macOS native target compilation conditional based on host OS, preventing build failures on non-Mac platforms.

**Acceptance Criteria**:
- [ ] Native targets (iOS, macOS) are wrapped in `if (HostManager.hostIsMac) { ... }`
- [ ] `all/` module is excluded on Windows via `settings.gradle.kts`
- [ ] Builds succeed on non-Mac platforms (JVM + JS targets only)

---

### REQ-7: Git Versioning

**Priority**: Medium

Introduce automatic version extraction from git tags using the `git-versioning` plugin.

**Acceptance Criteria**:
- [ ] `git-versioning` plugin is applied in root `build.gradle.kts`
- [ ] Tag pattern `v(?<version>.*)` extracts version from git tags
- [ ] Manual `tasks.create("version")` block is removed

---

### REQ-8: Build Performance Configuration

**Priority**: Medium

Add Gradle performance settings and toolchain auto-provisioning.

**Acceptance Criteria**:
- [ ] `gradle.properties` includes: `org.gradle.parallel=true`, `org.gradle.daemon=true`, `kotlin.incremental=true`
- [ ] Dokka V2 mode enabled: `org.jetbrains.dokka.experimental.gradle.pluginMode=V2Enabled`
- [ ] Java toolchain auto-provisioning: `org.gradle.java.installations.auto-detect=true`, `org.gradle.java.installations.auto-download=true`
- [ ] Maven Central publishing flags set

---

### REQ-9: Swift Package Support

**Priority**: Medium

Add multiplatformSwiftPackage plugin support in the `all/` module.

**Acceptance Criteria**:
- [ ] `swiftpackage` plugin applied in `all/build.gradle.kts`
- [ ] Swift tools version 5.7 configured
- [ ] iOS baseline v15, macOS baseline v12.0

---

### REQ-10: CI/CD Workflow Modernization

**Priority**: High

Update GitHub Actions workflows to match reference project patterns.

**Acceptance Criteria**:
- [ ] `publish.yml` renamed to `snapshot-publish.yml` with unified build/publish approach (no matrix)
- [ ] tumblr included in publishing targets
- [ ] `pods.yml` uses `-x check` instead of `-x test`
- [ ] Makefile uses `-x check` instead of `-x test`

---

### REQ-11: AGENTS.md Documentation

**Priority**: Medium

Create comprehensive AGENTS.md for AI-assisted development, following the pattern established by reference projects.

**Acceptance Criteria**:
- [ ] AGENTS.md exists at project root
- [ ] Contains: project overview, architecture description, directory structure, testing instructions, implementation guidelines, key file references, authentication setup
- [ ] Follows the format and depth of kmisskey's AGENTS.md

---

### REQ-12: Settings and Root Configuration Modernization

**Priority**: Critical

Modernize `settings.gradle.kts` with `pluginManagement` block and foojay toolchain resolver.

**Acceptance Criteria**:
- [ ] `pluginManagement { includeBuild("plugins") }` is present
- [ ] Google, Maven Central, and Gradle Plugin Portal repositories are configured
- [ ] Foojay resolver convention plugin is applied
- [ ] OS-conditional module inclusion for `all/` module
