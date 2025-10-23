import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    id("jacoco")
}

allprojects {
    group = property("app.group").toString()
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.cloud.dependencies.get().toString())
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation(libs.spring.boot.starter.web)
    annotationProcessor(libs.spring.boot.configuration.processor)

    // Test Dependencies
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
}

// about source and compilation
java {
    sourceCompatibility = JavaVersion.VERSION_17
}

with(extensions.getByType(JacocoPluginExtension::class.java)) {
    toolVersion = "0.8.7"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        // support JSR 305 annotation ( spring null-safety )
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}
// bundling tasks
tasks.getByName("bootJar") {
    enabled = true
}
tasks.getByName("jar") {
    enabled = false
}
// test tasks
tasks.test {
    ignoreFailures = true
    useJUnitPlatform()

    // 테스트 결과 상세 출력
    testLogging {
        events(
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED,
            TestLogEvent.STANDARD_OUT,
            TestLogEvent.STANDARD_ERROR
        )
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = false
    }

    // 테스트 실행 후 결과 요약 출력
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null) {
                println("\n====================================")
                println("Test Results")
                println("====================================")
                println("Tests: ${result.testCount}")
                println("Passed: ${result.successfulTestCount}")
                println("Failed: ${result.failedTestCount}")
                println("Skipped: ${result.skippedTestCount}")
                println("====================================\n")
            }
        }
    })
}