val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "1.9.23"
    id("io.ktor.plugin") version "2.3.9"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
}

group = "com.anarchyghost"
version = "0.0.2"

application {
    mainClass.set("com.anarchyghost.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

val githubPackagesUsername: String? by project
val githubPackagesToken: String? by project
val dockerHubUsername: String? by project
val dockerHubPassword: String? by project

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/anarchyghost/gitlab-notification-core")
        credentials {
            username = githubPackagesUsername
            password = githubPackagesToken
        }
    }
}

ktor {
    docker {
        jreVersion.set(JavaVersion.VERSION_17)
        imageTag.set("0.0.2")
        externalRegistry.set(
            io.ktor.plugin.features.DockerImageRegistry.dockerHub(
                appName = provider { "gitlab-notification" },
                username = provider { dockerHubUsername },
                password = provider { dockerHubPassword },
            )
        )
    }
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")

    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-okhttp-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")

    implementation("io.ktor:ktor-serialization-jackson-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")

    implementation("ch.qos.logback:logback-classic:$logback_version")

    implementation("com.anarchyghost:gitlab-notification-core:0.0.2")

    runtimeOnly("org.jetbrains.kotlin:kotlin-scripting-jsr223:$kotlin_version")

    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
