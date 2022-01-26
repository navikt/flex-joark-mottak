import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    kotlin("jvm") version "1.6.10"
}

group = "no.nav.helse.flex"
version = "1.0.0"
description = "flex-joark-mottak"
java.sourceCompatibility = JavaVersion.VERSION_11

buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2/")
    }
}

val githubUser: String by project
val githubPassword: String by project

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")

    maven(url = "https://jitpack.io")
}

val kafkaVersion = "3.0.0"
val confluentVersion = "7.0.1"
val jettyVersion = "9.4.41.v20210516"
val prometheusVersion = "0.12.0"
val resilience4jVersion = "1.6.1"
val gsonVersion = "2.8.9"
val apacheCommonsVersion = "2.7"
val logbackVersion = "1.2.10"

val mockitoKotlinVersion = "2.2.0"
val mockkVersion = "1.12.2"
val junitVersion = "5.8.2"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    implementation("io.confluent:kafka-streams-avro-serde:$confluentVersion")
    implementation("org.eclipse.jetty:jetty-servlet:$jettyVersion")
    implementation("org.eclipse.jetty:jetty-server:$jettyVersion")
    implementation("io.prometheus:simpleclient_servlet:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_logback:$prometheusVersion")
    implementation("org.json:json:20180813")
    implementation("io.github.resilience4j:resilience4j-all:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-retry:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:$resilience4jVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("org.apache.commons:commons-configuration2:$apacheCommonsVersion")
    implementation("ch.qos.logback:logback-core:$logbackVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:7.0.1")

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
            jvmTarget = "11"
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
