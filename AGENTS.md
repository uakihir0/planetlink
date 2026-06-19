# Agent Documentation

## Overview

This repository is a **multi Social Media (SNS) client abstraction library** compatible with [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html). PlanetLink provides a unified interface (`AccountAction`) to interact with multiple social media platforms transparently.

This is a Kotlin Multiplatform port of [SocialHub](https://github.com/uakihir0/SocialHub), which was originally implemented in Java. When implementing new features or modifying existing functionality, refer to the original SocialHub repository for design patterns, architecture decisions, and implementation references.

### Supported Platforms

| Platform | Library | Module |
|----------|---------|--------|
| Bluesky | [kbsky](https://github.com/uakihir0/kbsky) | `bluesky/` |
| Misskey | [kmisskey](https://github.com/uakihir0/kmisskey) | `misskey/` |
| Mastodon | [kmastodon](https://github.com/uakihir0/kmastodon) | `mastodon/` |
| Nostr | [knostr](https://github.com/uakihir0/knostr) | `nostr/` |
| Slack | [kslack](https://github.com/uakihir0/kslack) | `slack/` |
| Matrix | [kmatrix](https://github.com/uakihir0/kmatrix) | `matrix/` |
| Tumblr | [ktumblr](https://github.com/uakihir0/ktumblr) | `tumblr/` |

### Kotlin Multiplatform Targets

- JVM (Java 11+)
- JavaScript (Node.js + Browser)
- iOS (x64, arm64, simulatorArm64)
- macOS (arm64)

## Architecture

PlanetLink uses a **Service Adapter pattern** where each SNS platform implements the same core interfaces:

```
Application Code
    │
    ▼
PlanetLink Core (core/)
  │  AccountAction interface (45+ methods)
  │  Universal Models: User, Comment, Account, Service, Pageable<T>
  │
  ├── Bluesky Adapter (bluesky/) → kbsky SDK
  ├── Misskey Adapter (misskey/) → kmisskey SDK
  ├── Mastodon Adapter (mastodon/) → kmastodon SDK
  ├── Nostr Adapter (nostr/) → knostr SDK
  ├── Slack Adapter (slack/) → kslack SDK
  ├── Matrix Adapter (matrix/) → kmatrix SDK
  └── Tumblr Adapter (tumblr/) → ktumblr SDK
```

Each adapter module provides three key classes:

- **Auth** (`*Auth.kt`) - Authentication and account creation via `ServiceAuth<T>`
- **Action** (`*Action.kt`) - Implements `AccountAction` interface using platform SDK
- **Mapper** (`*Mapper.kt`) - Converts platform-specific models to PlanetLink universal models

## Directory Structure

- **`core/`**: Core abstraction library
  - `action/` - Action interfaces (`AccountAction`, `UserAction`, `CommentAction`, `RequestAction`)
    - `callback/` - Event callbacks for streams (comment, lifecycle, user)
    - `group/` - Batch operation interfaces
    - `request/` - Request builder interfaces (`CommentsRequest`, `UsersRequest`)
  - `define/` - Enumerations and type definitions
    - `action/` - Action type enums (`TimeLineActionType`, `SocialActionType`)
    - `emoji/` - Emoji type definitions
  - `model/` - Universal data models
    - `common/` - `AttributedString` and text attribution
    - `error/` - `SocialHubException`, `SocialHubError`
    - `event/` - Stream event models
    - `group/` - Group response models
    - `paging/` - Pagination strategies (`BorderPaging`, `CursorPaging`, `DatePaging`, etc.)
    - `request/` - Form models (`CommentForm`, `MediaForm`, `PollForm`)
    - `support/` - Supporting models
  - `micro/` - Micro-blogging specific models
  - `utils/` - Utility functions
- **`bluesky/`**: Bluesky adapter (kbsky)
  - `action/` - `BlueskyAuth`, `BlueskyAction`, `BlueskyMapper`, `BlueskyRequest`
  - `define/` - `BlueskyNotificationType`, `BlueskyReactionType`
  - `model/` - `BlueskyUser`, `BlueskyComment`, `BlueskyPaging`, `BlueskyChannel`
  - `expand/` - `PlanetLinkEx` (factory extension), `ServiceEx`
- **`misskey/`**: Misskey adapter (kmisskey)
  - `action/` - `MisskeyAuth`, `MisskeyAction`, `MisskeyMapper`
  - `define/` - `MisskeyVisibility`, `MisskeyScope`, `MisskeyFormKey`, etc.
  - `model/` - `MisskeyUser`, `MisskeyComment`, `MisskeyPaging`, `MisskeyPoll`, `MisskeyStream`, `MisskeyThread`
  - `expand/` - `PlanetLinkEx`, `ServiceEx`
- **`mastodon/`**: Mastodon adapter (kmastodon)
  - `action/` - `MastodonAuth`, `MastodonAction`, `MastodonMapper`
  - `define/` - `MastodonVisibility`, `MastodonScope`, `MastodonImageSize`, etc.
  - `model/` - `MastodonUser`, `MastodonComment`, `MastodonPaging`, `MastodonPoll`, `MastodonStream`, `MastodonThread`
  - `expand/` - `PlanetLinkEx`, `ServiceEx`, `AttributedStringEx`
- **`tumblr/`**: Tumblr adapter (ktumblr)
  - `action/` - `TumblrAuth`, `TumblrAction`, `TumblrMapper`
  - `define/` - `TumblrReactionType`, `TumblrIconSize`
  - `model/` - `TumblrUser`, `TumblrComment`, `TumblrPaging`
  - `expand/` - `PlanetLinkEx`, `ServiceEx`, `AttributedStringEx`
- **`all/`**: Aggregation module (CocoaPods, XCFramework, JS, Swift Package)
- **`plugins/`**: Gradle build plugins (module.publications, root.publications)
- **`docs/`**: Documentation
- **`tool/`**: Build scripts for CocoaPods

## Testing

Run the following tests after making changes:

```shell
# Run all JVM tests
./gradlew :all:jvmTest

# Run specific platform tests
./gradlew :all:jvmTest --tests "work.socialhub.planetlink.GetUserTest"
./gradlew :all:jvmTest --tests "work.socialhub.planetlink.HomeTimelineTest"

# Run core module tests
./gradlew :core:jvmTest

# Run platform adapter tests
./gradlew :bluesky:jvmTest
./gradlew :mastodon:jvmTest
```

If network access is not available, verify that the build succeeds:

```shell
./gradlew jvmJar
```

If authentication credentials are required for testing, create `secrets.json` (refer to `secrets.json.default`):

```json
{
  "bluesky": [
    {
      "apiHost": "https://bsky.social",
      "streamHost": "wss://bsky.network",
      "identify": "your-handle",
      "password": "your-app-password"
    }
  ],
  "misskey": [
    {
      "host": "https://misskey.io/api/",
      "userToken": "YOUR_USER_TOKEN"
    }
  ],
  "mastodon": [
    {
      "host": "https://mastodon.social",
      "clientId": "YOUR_CLIENT_ID",
      "clientSecret": "YOUR_CLIENT_SECRET",
      "userToken": "YOUR_USER_TOKEN"
    }
  ],
  "tumblr": [
    {
      "clientId": "YOUR_CLIENT_ID",
      "clientSecret": "YOUR_CLIENT_SECRET",
      "accessToken": "YOUR_ACCESS_TOKEN",
      "refreshToken": "YOUR_REFRESH_TOKEN"
    }
  ]
}
```

## Implementation Guidelines

### Adding a New SNS Platform

1. Create a new module directory (e.g., `slack/`)
2. Create `build.gradle.kts` following the existing module pattern
3. Add the module to `settings.gradle.kts`
   - Dependencies on external libraries (kbsky, knostr, etc.) must use **published Maven artifacts** via the version catalog (`gradle/libs.versions.toml`). Do NOT use `includeBuild()` for local composite builds.
4. Implement the following classes:

| Class | Purpose | Base |
|-------|---------|------|
| `{Platform}Auth.kt` | Authentication and account creation | Extends `ServiceAuth<T>` |
| `{Platform}Action.kt` | Core API implementation | Extends `AccountActionImpl` |
| `{Platform}Mapper.kt` | Model conversion | Object with mapping functions |
| `{Platform}User.kt` | Platform-specific user model | Extends `User` |
| `{Platform}Comment.kt` | Platform-specific post model | Extends `Comment` |
| `{Platform}Paging.kt` | Platform-specific pagination | Extends `Paging` |
| `PlanetLinkEx.kt` | Factory extension on PlanetLink | Extension function |
| `ServiceEx.kt` | Service type extension | Extension function |

5. Add the module export in `all/build.gradle.kts`
6. Add test configurations in `all/src/jvmTest/`

### Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Auth class | `{Platform}Auth` | `BlueskyAuth` |
| Action class | `{Platform}Action` | `BlueskyAction` |
| Mapper object | `{Platform}Mapper` | `BlueskyMapper` |
| Model classes | `{Platform}{Model}` | `BlueskyUser`, `BlueskyComment` |
| Paging class | `{Platform}Paging` | `BlueskyPaging` |
| Factory extension | `PlanetLinkEx` | `PlanetLink.bluesky()` |
| Service extension | `ServiceEx` | Service property extensions |

### Key Patterns

- All platform actions wrap API calls in `proceed { }` / `proceedUnit { }` for error handling
- Models carry action methods: `user.action.follow()`, `comment.action.like()`
- Pagination uses strategy pattern: `BorderPaging`, `CursorPaging`, `DatePaging`, `OffsetPaging`
- Rich text is handled via `AttributedString` across all platforms
- Stream callbacks are registered via `setHomeTimeLineStream()`, `setNotificationStream()`
- **Capabilities discovery**: Each adapter declares supported actions via `capabilities()` — see below

### Kotlin/JS yield* Bug — MUST READ

**Known Kotlin/JS code-generation defect** (Kotlin 2.4.0, `es2015` generator target + `-XXLanguage:+JsAllowExportingSuspendFunctions`). Manifests at runtime on the JS target only:

```
TypeError: yield* (intermediate value)(...) is not iterable
```

#### Root cause

With the `es2015` target each suspend function is lowered to an ES generator and every suspension point becomes `yield* callee()`. For **`@JsExport` classes**, calls to a suspend member that is dispatched **virtually** (abstract/`open`, overridden in a subclass) are routed through a generated `name$suspendBridge` method. In some shapes the compiler emits the bridge under a mangled name that is **never wired onto the prototype**, so `yield*` delegates to `undefined` / a non-generator and the coroutine driver throws "is not iterable". It is NOT a compile-time error and only fires on the path that actually reaches the call (e.g. a cache *miss*). Related upstream: KT-84710, KT-86934.

The general trigger is: **a suspend method calls another suspend method of the same class that is itself an `@JsExport` override (or an overload that shares the exported name) — the call routes through that method's `name$suspendBridge`, which is unwired.** This is NOT limited to `userMe`. Empirically reproduced (Node, driving the generated `.mjs`) across many methods:

| caller | callee (overridden) | modules |
|---|---|---|
| `userMeWithCache()` | `userMe()` | all (base class) |
| `comment(url)` | `comment(id)` | misskey, mastodon |
| `user(url)` | `user(id)` / `searchUsers()` | misskey, mastodon, tumblr |
| `reactionComment()` | `likeComment()` / `shareComment()` | bluesky, misskey, mastodon, tumblr |
| `unreactionComment()` | `unlikeComment()` / `unshareComment()` | bluesky, misskey, mastodon, tumblr |
| `postMessage()` | `postComment()` | mastodon, matrix |
| `postComment()` | `postMessage()` | misskey |
| `relationship()` | `user()` | tumblr |
| `messageTimeLine()` | `channelTimeLine()` | matrix |
| `likeComment()`/`unlikeComment()` | `reactionComment()` | matrix |
| `setNotificationStream()` | `setHomeTimeLineStream()` | matrix |

Three presentations of the same defect:

1. **Base→override virtual suspend delegation.** A base-class `open suspend fun` calls an abstract/overridden suspend fun — `AccountActionImpl.userMeWithCache()` (open) → `userMe()`.

2. **Same-class override→override call** (direct or inside `proceed { }` / `coroutineScope { }`). One overridden suspend method calls another on the same class — e.g. `reactionComment()` → `likeComment()`, `comment(url)` → `comment(id)`.

3. **Same-class suspend call from inside a suspend lambda.** Same as #2 but the call sits inside `proceed { }` / `proceedUnit { }`.

Cross-**class** suspend calls (calling a method on a *different* object, e.g. `auth.accessor.slack.chat().chatPostMessage(...)`, or a separate helper class) are codegen'd correctly and do NOT hit the broken bridge. A call to a `private` (non-exported, non-virtual) suspend function is also safe — it compiles to a direct generator call.

**Why some same-class calls are safe and others crash (the key distinction).** It comes down to whether the *callee* is an interface/base **`override`** or a **class-owned** member:

- **Callee is `override suspend fun`** (implements an interface/base method, e.g. `comment`, `user`, `likeComment`, `reactionComment`, `postComment`, `channelTimeLine`) → its `name$suspendBridge` is declared on the **interface side** (core) and **never wired onto the subclass prototype**. A same-class `yield* this.callee$suspendBridge(...)` hits `undefined` → **crash**.
- **Callee is a plain class-owned `suspend fun`** (no `override`; declared only on the concrete adapter, e.g. `commentContext`, `getEmojis`, `homeTimeLineStream`, `notificationStream`, `getBots`, and every `private fetchX`/`doX`) → the compiler emits a **wired dispatcher method** `*name$suspendBridge(){ this.name === protoOf(C).name ? yield* internal : await_0(this.name()) }` on that class → **safe**.

This is a 100% predictor across this repo: every crash had an `override` callee; every method that "worked before the fix" called a non-`override` class-owned method. That's exactly why the `private fetchX`/`doX` fix works — a private function is class-owned and gets a direct call, never the unwired interface bridge. (Verify a callee's status: `grep "suspend fun <name>(" Adapter.kt` — leading `override` = risky as a same-class callee.)

#### The fix: route same-class calls through a private free-standing function

The universal rule: **a public/overridden suspend method must never call another public/overridden suspend method of the same class.** Extract the callee's body into a `private suspend fun` and have BOTH the public override and any same-class caller invoke the private one. A `private` function compiles to a direct generator call, bypassing the virtual bridge.

```kotlin
// userMe / userMeWithCache (NostrAction, SlackAction, + all adapters)
override suspend fun userMe(): User = fetchUserMe()
override suspend fun userMeWithCache(): User = me ?: fetchUserMe()
private suspend fun fetchUserMe(): User { /* real API + cache into me */ }

// reactionComment -> likeComment/shareComment (all adapters)
override suspend fun likeComment(id: Identify) = doLikeComment(id)
override suspend fun reactionComment(id: Identify, r: String) {
    if (isLike(r)) { doLikeComment(id); return }   // NOT likeComment(id)
    ...
}
private suspend fun doLikeComment(id: Identify) { /* real impl */ }

// comment(url) -> comment(id), user(url) -> user(id), relationship -> user, etc.
override suspend fun comment(id: Identify): Comment = fetchComment(id)
override suspend fun comment(url: String): Comment = fetchComment(parse(url))   // NOT comment(id)
private suspend fun fetchComment(id: Identify): Comment { /* real impl */ }
```

Naming convention used in-repo: `fetchX` for getters (`fetchUserMe`, `fetchUser`, `fetchComment`), `doX` for actions (`doLikeComment`, `doPostComment`, `doChannelTimeLine`).

Other valid patterns:
- **Extract shared suspend logic into a separate (non-exported) helper class** — cross-class calls are safe (`SlackActionHelper`). Note: a helper calling back into `action.userMe()` still hits the bridge; combine with the private-function rule.
- **Inline the callee** when it's trivial (e.g. Misskey `postComment` for a message just throws `NotImplementedException` directly instead of calling `postMessage()`).
- When hoisting out of `proceed { }`, keep error handling: embed `proceed { }` in the private callee or integrate `ExceptionHandler.classify` (as in `TumblrAction.validateToken`).

#### How to diagnose / verify precisely

Compile to JS (`./gradlew :all:jsNodeDevelopmentLibraryDistribution`, output under `all/build/dist/js/developmentLibrary/`), then scan for **reachable unwired bridges** — a `X$suspendBridge_...` referenced via an unguarded `yield* this.X$suspendBridge(...)` but never defined as a `*X$suspendBridge(` method anywhere:

```sh
cd all/build/dist/js/developmentLibrary
grep -rhoE '\*[A-Za-z0-9_]+\$suspendBridge_[a-z0-9]+_k\$\(' *.mjs | sed 's/^\*//;s/($//' | sort -u > /tmp/defs.txt
for f in planetlink-*.mjs; do
  grep -hoE 'yield\* this\.[A-Za-z0-9_]+\$suspendBridge_[a-z0-9]+_k\$' "$f" | sed 's/yield\* this\.//' | sort -u > /tmp/r.txt
  bad=$(comm -23 /tmp/r.txt /tmp/defs.txt); [ -n "$bad" ] && echo "$f: $bad"
done
# (a guarded `jsIsFunction(...) ? ... : await_0(...)` reference is SAFE; only unguarded `yield* this.X` is the bug)
```
Zero output = no reachable unwired bridges. Then build an action with a fake token and drive the suspect methods via `promisify`; a fixed method reaches a network/logic error, a broken one throws `yield* ... is not iterable`.

#### Scope

- JS target only (JVM / Native use a different coroutine implementation)
- No compile-time error — runtime only, and often only on a specific path (cache-miss, a particular URL overload, a reaction code branch)
- All known call sites fixed in: core (`userMeWithCache`/`userMe` via per-adapter overrides), bluesky, misskey, mastodon, tumblr, matrix, slack, nostr. Verified: 0 reachable unwired bridges remain across all adapter `.mjs`.

#### Why it surfaced only in Slack first

The defect is latent in many call sites but only fires when the broken path is actually executed. `userMeWithCache → userMe` only hits the bridge on a cache *miss* (`me == null`); most adapters call `userMe()` at login (warming `me`) before any timeline, so they took the cache-hit branch. Slack's `homeTimeLine` (polling/stream orchestrator) reached `userMeWithCache()` while `me` was still null → crash. Likewise `comment(url)`/`reactionComment` etc. only crash when those specific entry points run. "Module X works" only meant "X's broken paths weren't exercised yet" — not that X was safe.

### Capabilities Discovery

Each adapter statically declares which `ActionType` entries it supports via a `CAPABILITIES` companion-object field returned by `capabilities()`. This allows callers to check feature support **without making API calls**.

**Key files:**
- `core/src/commonMain/kotlin/work/socialhub/planetlink/action/Capabilities.kt`
- `core/src/commonMain/kotlin/work/socialhub/planetlink/define/action/StreamActionType.kt`
- `core/src/commonMain/kotlin/work/socialhub/planetlink/define/action/MessageActionType.kt`

**When adding or removing a method in an adapter's `*Action.kt`:**
1. Update the adapter's `CAPABILITIES` set in its companion object to reflect the change
2. If the method corresponds to a new action category, add a new enum value to the appropriate `ActionType` enum (or create a new enum implementing `ActionType`)
3. Run `./gradlew :all:compileKotlinJvm` to verify all adapters compile

### Platform-Specific Limitations

- **`all` module**: Can only be built on macOS (CocoaPods/XCFramework)
- **JavaScript**: Requires `@JsExport` annotations and Promise extensions for suspend functions
- **Apple platforms**: Refer to [planetlink-cocoapods](https://github.com/uakihir0/planetlink-cocoapods)

## Key File References

| Purpose | File Path |
|---------|-----------|
| Main entry point | `core/src/commonMain/kotlin/work/socialhub/planetlink/PlanetLink.kt` |
| Account action interface | `core/src/commonMain/kotlin/work/socialhub/planetlink/action/AccountAction.kt` |
| Capabilities class | `core/src/commonMain/kotlin/work/socialhub/planetlink/action/Capabilities.kt` |
| User model | `core/src/commonMain/kotlin/work/socialhub/planetlink/model/User.kt` |
| Comment model | `core/src/commonMain/kotlin/work/socialhub/planetlink/model/Comment.kt` |
| Account model | `core/src/commonMain/kotlin/work/socialhub/planetlink/model/Account.kt` |
| Bluesky action | `bluesky/src/commonMain/kotlin/work/socialhub/planetlink/bluesky/action/BlueskyAction.kt` |
| Misskey action | `misskey/src/commonMain/kotlin/work/socialhub/planetlink/misskey/action/MisskeyAction.kt` |
| Mastodon action | `mastodon/src/commonMain/kotlin/work/socialhub/planetlink/mastodon/action/MastodonAction.kt` |
| Nostr action | `nostr/src/commonMain/kotlin/work/socialhub/planetlink/nostr/action/NostrAction.kt` |
| Slack action | `slack/src/commonMain/kotlin/work/socialhub/planetlink/slack/action/SlackAction.kt` |
| Matrix action | `matrix/src/commonMain/kotlin/work/socialhub/planetlink/matrix/action/MatrixAction.kt` |
| Tumblr action | `tumblr/src/commonMain/kotlin/work/socialhub/planetlink/tumblr/action/TumblrAction.kt` |
| Integration tests | `all/src/jvmTest/kotlin/work/socialhub/planetlink/` |
| Test configuration | `all/src/jvmTest/kotlin/work/socialhub/planetlink/AbstractTest.kt` |

## Build & Publish

```shell
# Full build
make build

# Build CocoaPods XCFramework
make pods

# Print project version
make version
```
