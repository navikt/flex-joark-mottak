import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "11.3.1"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm") version "1.8.20"
}

group = "no.nav.helse.flex"
version = "1.0.0"
description = "flex-joark-mottak"
java.sourceCompatibility = JavaVersion.VERSION_17

buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2/")
    }
}

tasks.withType<ShadowJar> {
    archiveFileName.set("app.jar")
}

val githubUser: String by project
val githubPassword: String by project

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")

    maven(url = "https://jitpack.io")
}

val kafkaVersion = "3.2.3"
val confluentVersion = "7.3.2"

val jettyVersion = "11.0.14"
val prometheusVersion = "0.16.0"
val resilience4jVersion = "1.7.1"
val gsonVersion = "2.10.1"
val apacheCommonsVersion = "2.8.0"
val logbackVersion = "1.4.6"
val logstashVersion = "7.3"

val mockitoKotlinVersion = "2.2.0"
val mockkVersion = "1.13.4"
val junitVersion = "5.9.2"

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    implementation("io.confluent:kafka-streams-avro-serde:$confluentVersion")
    implementation("org.eclipse.jetty:jetty-servlet:$jettyVersion")
    implementation("org.eclipse.jetty:jetty-server:$jettyVersion")
    implementation("io.prometheus:simpleclient_servlet_jakarta:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_logback:$prometheusVersion")
    implementation("org.json:json:20220924")
    implementation("io.github.resilience4j:resilience4j-all:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-retry:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:$resilience4jVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("org.apache.commons:commons-configuration2:$apacheCommonsVersion")

    implementation("ch.qos.logback:logback-core:$logbackVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:$mockitoKotlinVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
            if (System.getenv("CI") == "true") {
                kotlinOptions.allWarningsAsErrors = true
            }
        }
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    withType<ShadowJar> {
        transform(ServiceFileTransformer::class.java) {
            setPath("META-INF/cxf")
            include("bus-extensions.txt")
        }
    }

    withType<Jar> {
        manifest.attributes["Main-Class"] = "no.nav.helse.flex.infrastructure.server.StartJetty"
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("PASSED", "FAILED", "SKIPPED")
        }
    }
}
