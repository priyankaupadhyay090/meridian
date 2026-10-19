plugins {
    java
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.meridian"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.apache.pdfbox:pdfbox:3.0.5")
    implementation(platform("software.amazon.awssdk:bom:2.31.0"))
    implementation("software.amazon.awssdk:s3")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform {
        excludeTags("model")
    }
}

tasks.register<Test>("evalModel") {
    description = "Compares rules alone with rules and a local Ollama model on the labelled contracts."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("model")
    }
    systemProperty("ollama.url", System.getenv("OLLAMA_URL") ?: "http://localhost:11434")
    systemProperty("ollama.model", System.getenv("MERIDIAN_MODEL") ?: "qwen2.5:7b")
    testLogging {
        showStandardStreams = true
    }
    outputs.upToDateWhen { false }
}
