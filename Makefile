build:
	./gradlew \
	core:clean core:build \
	bluesky:clean bluesky:build \
	misskey:clean misskey:build \
	-x test --refresh-dependencies

pods:
	./gradlew \
	all:assemblePlanetlinkXCFramework \
	all:podPublishXCFramework \
	-x test --refresh-dependencies

version:
	 ./gradlew version --no-daemon --console=plain -q

.PHONY: build pods version