> [日本語](./docs/README_ja.md)

# PlanetLink

![badge][badge-js]
![badge][badge-jvm]
![badge][badge-ios]
![badge][badge-mac]

**This library is a multi Social Media (SNS) client library compatible with [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html).**
By using this library, you can transparently handle various Social Media platforms.

## Supported Platforms

- Bluesky (library: [kbksy](https://github.com/uakihir0/kbsky))
- Misskey (library: [kmisskey](https://github.com/uakihir0/kmisskey))
- Mastodon (library: [kmastodon](https://github.com/uakihir0/kmastodon))

## Planned Supported Platforms

- Mastodon
- Slack
- Tumblr

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
[badge-tvos]: http://img.shields.io/badge/-tvos-808080.svg