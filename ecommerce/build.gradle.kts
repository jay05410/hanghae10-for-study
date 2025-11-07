plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
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
    implementation(libs.jackson.kotlin)
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Spring Transaction (for @Transactional without JPA)
    implementation("org.springframework:spring-tx")

    // JANSI for console colors
    implementation("org.fusesource.jansi:jansi:2.4.0")

    // Snowflake ID Generator
    implementation("cn.ipokerface:snowflake-id-generator:2.5.0")

    // Test Dependencies
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
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