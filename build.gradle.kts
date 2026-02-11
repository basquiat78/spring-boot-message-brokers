plugins {
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"

    kotlin("jvm") version "2.2.21"
    kotlin("kapt") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.jpa") version "2.2.21"
}

group = "io.basquiat"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")

    // RabbitMQ
    implementation("org.springframework.retry:spring-retry:2.0.12")
    implementation("org.springframework.boot:spring-boot-starter-amqp")

    // redisson
    implementation("org.redisson:redisson-spring-boot-starter:4.2.0")

    // Kafka
    implementation("org.springframework.boot:spring-boot-starter-kafka")
    implementation("org.apache.kafka:kafka-streams")

    // NATS
    implementation("io.nats:jnats:2.25.1")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    runtimeOnly("org.postgresql:postgresql")

    // 보안 문제로 해당 maven을 사용하도록 한다.
    implementation("io.github.openfeign.querydsl:querydsl-jpa:5.6.1")
    kapt("io.github.openfeign.querydsl:querydsl-apt:5.6.1:jakarta")
    kapt("jakarta.annotation:jakarta.annotation-api")
    kapt("jakarta.persistence:jakarta.persistence-api")
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.2.3")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "junit-vintage-engine")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}