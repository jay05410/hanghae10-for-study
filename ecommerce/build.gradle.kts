plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    id("jacoco")
}

group = "com.hanghae"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.cloud.dependencies.get().toString())
    }
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.jackson.kotlin)
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Spring Data JPA
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // MySQL Database
    runtimeOnly(libs.mysql.connector)

    // H2 Database (for testing only)
    testRuntimeOnly("com.h2database:h2")


    // P6Spy Spring Boot Starter for SQL logging
    implementation("com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.12.0")

    // Snowflake ID Generator
    implementation("cn.ipokerface:snowflake-id-generator:2.5.0")

    // API 문서화
    implementation(libs.springdoc.openapi.webmvc.ui)
    implementation(libs.springdoc.openapi.common)

    // Spring Actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Micrometer Prometheus for metrics export
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Test Dependencies
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.3")

    // TestContainers
    testImplementation(libs.bundles.testcontainers.mysql)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

// bundling tasks
tasks.getByName("bootJar") {
    enabled = true
}
tasks.getByName("jar") {
    enabled = false
}

// jacoco configuration
jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}