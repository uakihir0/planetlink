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
| Tumblr | [ktumblr](https://github.com/uakihir0/ktumblr) | `tumblr/` |

### Kotlin Multiplatform Targets

- JVM (Java 11+)
- JavaScript (IR, Node.js + Browser)
- iOS (x64, arm64, simulatorArm64)
- macOS (x64, arm64)

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

### Platform-Specific Limitations

- **`all` module**: Can only be built on macOS (CocoaPods/XCFramework)
- **JavaScript**: Requires `@JsExport` annotations and Promise extensions for suspend functions
- **Apple platforms**: Refer to [planetlink-cocoapods](https://github.com/uakihir0/planetlink-cocoapods)

## Key File References

| Purpose | File Path |
|---------|-----------|
| Main entry point | `core/src/commonMain/kotlin/work/socialhub/planetlink/PlanetLink.kt` |
| Account action interface | `core/src/commonMain/kotlin/work/socialhub/planetlink/action/AccountAction.kt` |
| User model | `core/src/commonMain/kotlin/work/socialhub/planetlink/model/User.kt` |
| Comment model | `core/src/commonMain/kotlin/work/socialhub/planetlink/model/Comment.kt` |
| Account model | `core/src/commonMain/kotlin/work/socialhub/planetlink/model/Account.kt` |
| Bluesky action | `bluesky/src/commonMain/kotlin/work/socialhub/planetlink/bluesky/action/BlueskyAction.kt` |
| Misskey action | `misskey/src/commonMain/kotlin/work/socialhub/planetlink/misskey/action/MisskeyAction.kt` |
| Mastodon action | `mastodon/src/commonMain/kotlin/work/socialhub/planetlink/mastodon/action/MastodonAction.kt` |
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
