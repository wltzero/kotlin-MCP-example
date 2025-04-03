plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.fushi"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

val mcpVersion = "0.4.0"
val ktorVersion = "3.1.1"
val koinVersion = "4.0.2"

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.modelcontextprotocol:kotlin-sdk:$mcpVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    implementation(project.dependencies.platform("io.insert-koin:koin-bom:$koinVersion"))
    implementation("io.insert-koin:koin-core")
    implementation("io.insert-koin:koin-ktor")
    implementation("io.insert-koin:koin-logger-slf4j")

    implementation("io.github.microutils:kotlin-logging-jvm:2.1.23")

    implementation("ch.qos.logback:logback-classic:1.4.14")


}


tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}