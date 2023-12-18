#!/usr/bin/env bash
cd "$(dirname "$0")"
BASE_PATH=$(pwd)

# Make Repository
cd $BASE_PATH
mkdir -p ../build/cocoapods/repository/debug
mkdir -p ../build/cocoapods/repository/release

# Copy Podspec
cd $BASE_PATH
cd ../build/cocoapods/publish/debug
cp planetlink.podspec ../../repository/planetlink-debug.podspec
sed -i -e "s|'planetlink'|'planetlink-debug'|g" planetlink-debug.podspec
sed -i -e "s|'planetlink.xcframework'|'debug/planetlink.xcframework'|g" planetlink-debug.podspec
rm *.podspec-e
cd $BASE_PATH
cd ../build/cocoapods/publish/release
cp planetlink.podspec ../../repository/planetlink-release.podspec
sed -i -e "s|'planetlink'|'planetlink-release'|g" planetlink-release.podspec
sed -i -e "s|'planetlink.xcframework'|'release/planetlink.xcframework'|g" planetlink-release.podspec
rm *.podspec-e

# Copy Framework
cd $BASE_PATH
cd ../build/cocoapods/publish/debug
cp -r planetlink.xcframework ../../repository/debug/planetlink.xcframework
cd $BASE_PATH
cd ../build/cocoapods/publish/release
cp -r planetlink.xcframework ../../repository/release/planetlink.xcframework

