# PlanetLink

![badge][badge-js]
![badge][badge-jvm]
![badge][badge-ios]
![badge][badge-mac]

**このライブラリは [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) に対応したマルチ Social Media (SNS) クライアントライブラリです。**
このライブラリを用いることで、各 Social Media を透過的な操作で扱うことができます。

本ライブラリは、Java で実装された [SocialHub](https://github.com/uakihir0/SocialHub) の Kotlin Multiplatform 版です。

## サポートプラットフォーム

- Bluesky (library: [kbksy](https://github.com/uakihir0/kbsky))
- Misskey (library: [kmisskey](https://github.com/uakihir0/kmisskey))
- Mastodon (library: [kmastodon](https://github.com/uakihir0/kmastodon))
- Slack (library: [kslack](https://github.com/uakihir0/kslack))
- Tumblr (library: [ktumblr](https://github.com/uakihir0/ktumblr))

## 使い方

以下は対応するプラットフォームにおいて Gradle を用いて Kotlin で使用する際の使い方になります。
**Apple プラットフォームで使用する場合は、 [planetlink-cocoapods](https://github.com/uakihir0/planetlink-cocoapods) を参照してください。**
また、テストコードも合わせて確認してください。

```kotlin:build.gradle.kts
repositories {
    mavenCentral()
+   maven { url = uri("https://repo.repsy.io/mvn/uakihir0/public") }
}

dependencies {
+   implementation("work.socialhub.planetlink:all:0.0.1-SNAPSHOT")
}
```

## ライセンス

MIT License

## 作者

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
[badge-tvos]: http://img.shields.io/badge/-tvos-808080.svg