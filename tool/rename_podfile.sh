#!/usr/bin/env bash
cd "$(dirname "$0")" || exit
BASE_PATH=$(pwd)
BUILD_PATH=../all/build

# Make Repository
cd "$BASE_PATH" || exit
mkdir -p $BUILD_PATH/cocoapods/repository/debug
mkdir -p $BUILD_PATH/cocoapods/repository/release

# Copy Podspec
cd "$BASE_PATH" || exit
cd $BUILD_PATH/cocoapods/publish/debug || exit
cp planetlink.podspec ../../repository/planetlink-debug.podspec
cd ../../repository/ || exit
sed -i -e "s|'planetlink'|'planetlink-debug'|g" planetlink-debug.podspec
sed -i -e "s|'planetlink.xcframework'|'debug/planetlink.xcframework'|g" planetlink-debug.podspec
rm *.podspec-e
cd "$BASE_PATH" || exit
cd $BUILD_PATH/cocoapods/publish/release || exit
cp planetlink.podspec ../../repository/planetlink-release.podspec
cd ../../repository/ || exit
sed -i -e "s|'planetlink'|'planetlink-release'|g" planetlink-release.podspec
sed -i -e "s|'planetlink.xcframework'|'release/planetlink.xcframework'|g" planetlink-release.podspec
rm *.podspec-e

# Copy Framework
cd "$BASE_PATH" || exit
cd $BUILD_PATH/cocoapods/publish/debug || exit
cp -r planetlink.xcframework ../../repository/debug/planetlink.xcframework
cd "$BASE_PATH" || exit
cd $BUILD_PATH/cocoapods/publish/release || exit
cp -r planetlink.xcframework ../../repository/release/planetlink.xcframework

# Copy README
cd "$BASE_PATH" || exit
cd ../ || exit
cp ./LICENSE ./all/build/cocoapods/repository/LICENSE
cp ./docs/pods/README.md ./all/build/cocoapods/repository/README.md
cp ./docs/pods/README_ja.md ./all/build/cocoapods/repository/README_ja.md
