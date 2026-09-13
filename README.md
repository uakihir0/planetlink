> [日本語](./docs/README_ja.md)

# PlanetLink

![badge][badge-js]
![badge][badge-jvm]
![badge][badge-ios]
![badge][badge-mac]

![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.repsy.io%2Fmvn%2Fuakihir0%2Fpublic%2Fwork%2Fsocialhub%2Fplanetlink%2Fall%2Fmaven-metadata.xml)

**This library is a multi Social Media (SNS) client library compatible with [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html).**
By using this library, you can transparently handle various Social Media platforms.

This is a Kotlin Multiplatform port of [SocialHub](https://github.com/uakihir0/SocialHub), which was originally implemented in Java.

## Supported Platforms

- Bluesky (library: [kbsky](https://github.com/uakihir0/kbsky))
- Misskey (library: [kmisskey](https://github.com/uakihir0/kmisskey))
- Mastodon (library: [kmastodon](https://github.com/uakihir0/kmastodon))
- Nostr (library: [knostr](https://github.com/uakihir0/knostr))
- Slack (library: [kslack](https://github.com/uakihir0/kslack))
- Matrix (library: [kmatrix](https://github.com/uakihir0/kmatrix))
- Tumblr (library: [ktumblr](https://github.com/uakihir0/ktumblr))
- Saypip (library: [ksaypip](https://github.com/uakihir0/ksaypip))
- X / Twitter (read-only, library: [kxweb](https://github.com/uakihir0/kxweb))

## Usage

The following is how to use it with Gradle in Kotlin for the respective platforms. **If you are using it on the Apple platform, please refer to [planetlink-cocoapods](https://github.com/uakihir0/planetlink-cocoapods).** Also, make sure to check the test code.

```kotlin:build.gradle.kts
repositories {
    mavenCentral()
+   maven { url = uri("https://repo.repsy.io/mvn/uakihir0/public") }
}

dependencies {
+   implementation("work.socialhub.planetlink:all:0.0.1-SNAPSHOT")
}
```

### Saypip

Saypip is a semi-anonymous SNS: everyone browses and posts anonymously, and real profiles are
revealed only between mutual friends. The adapter models what the product actually has, and the
things it deliberately does not: a person is a viewer-scoped identity token with no stable ID, a
friendship is mutual (there is no one-directional follow), there is no user search or social
graph, no re-sharing, bookmarks, polls or editing.

The API is reached through Saypip's OAuth 2.1 authorization server with PKCE:

```kotlin
import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.saypip.expand.PlanetLinkEx.saypip

val auth = PlanetLink.saypip("https://saypip.app")
    .setConsumerInfo(clientId = "saypip_app_...")

// Open the URL in a browser; the callback carries `code` and `state`.
val url = auth.authorizationURL(redirectUri = "myapp://callback")
val account = auth.accountWithVerifier("myapp://callback", code)
```

An application that already holds a token comes in through `accountWithAccessToken`.

The Global Room is available as a stream: a frame carries a post ID, and the adapter reads the
post back before calling the listener, so a `UpdateCommentCallback` sees a whole comment.

```kotlin
import net.socialhub.planetlink.model.event.CommentEvent
import work.socialhub.planetlink.action.callback.comment.UpdateCommentCallback

val stream = account.action.setHomeTimeLineStream(object : UpdateCommentCallback {
    override fun onUpdate(event: CommentEvent?) {
        println(event?.comment?.text?.displayText)
    }
})
stream.open()   // suspend; receives frames until close()
```

### X / Twitter

The X adapter intentionally exposes read-only operations only. Posting, replying,
deleting, liking, reposting, bookmarking, and following are not supported. This
follows QuaX's approach of keeping user actions outside X while allowing public
content, timelines, search, bookmarks, articles, and trends to be read.

```kotlin
import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.x.expand.PlanetLinkEx.x
import work.socialhub.planetlink.x.model.XPaging

val account = PlanetLink.x().accountWithCookies(
    authToken = "X_AUTH_TOKEN_COOKIE",
    csrfToken = "X_CT0_COOKIE",
)

// The common home timeline maps to X's Following timeline.
val following = account.action.homeTimeLine(XPaging(20))

// Guest mode supports a limited set of public reads.
val guest = PlanetLink.x().guestAccount()
```

## License

MIT License

## Author

[Akihiro Urushihara](https://github.com/uakihir0)

[badge-android]: http://img.shields.io/badge/-android-6EDB8D.svg
[badge-android-native]: http://img.shields.io/badge/support-[AndroidNative]-6EDB8D.svg
[badge-wearos]: http://img.shields.io/badge/-wearos-8ECDA0.svg
[badge-jvm]: http://img.shields.io/badge/-jvm-DB413D.svg
[badge-js]: http://img.shields.io/badge/-js-F8DB5D.svg
[badge-js-ir]: https://img.shields.io/badge/support-[IR]-AAC4E0.svg
[badge-nodejs]: https://img.shields.io/badge/-nodejs-68a063.svg
[badge-linux]: http://img.shields.io/badge/-linux-2D3F6C.svg
[badge-windows]: http://img.shields.io/badge/-windows-4D76CD.svg
[badge-wasm]: https://img.shields.io/badge/-wasm-624FE8.svg
[badge-apple-silicon]: http://img.shields.io/badge/support-[AppleSilicon]-43BBFF.svg
[badge-ios]: http://img.shields.io/badge/-ios-CDCDCD.svg
[badge-mac]: http://img.shields.io/badge/-macos-111111.svg
[badge-watchos]: http://img.shields.io/badge/-watchos-C0C0C0.svg

