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