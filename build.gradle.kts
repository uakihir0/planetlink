allprojects {
    group = "work.socialhub.planetlink"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.repsy.io/mvn/uakihir0/public") }
    }
}

tasks.wrapper {
    gradleVersion = "8.5"
    distributionType = Wrapper.DistributionType.ALL
}

tasks.create("version") {
    doLast { println(project.version) }
}
