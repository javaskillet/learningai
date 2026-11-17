plugins {
    id("io.micronaut.application") version "5.0.2"
    id("com.gradleup.shadow") version "9.0.0-beta12"
}

version = "1.0.0"
group = "com.chat5000"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    annotationProcessor("io.micronaut:micronaut-http-validation")
    annotationProcessor("io.micronaut.serde:micronaut-serde-processor")
    annotationProcessor("io.micronaut.validation:micronaut-validation-processor")

    implementation("io.micronaut:micronaut-http-server-netty")
    implementation("io.micronaut:micronaut-jackson-databind")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("io.micronaut.micrometer:micronaut-micrometer-core")
    implementation("io.micronaut.micrometer:micronaut-micrometer-registry-prometheus")
    implementation("io.micronaut:micronaut-management")

    // Valkey (Redis-protocol) client - Apache-2.0
    implementation("io.lettuce:lettuce-core:7.9.0.RELEASE")

    // OAuth2 / JWT Bearer token handling - Apache-2.0
    implementation("com.nimbusds:nimbus-jose-jwt:10.10")

    // Logging - MIT / EPL-2.0
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("org.yaml:snakeyaml")

    // Testing with JUnit 5 & Ephemeral Containers (No mock libraries)
    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.testcontainers:testcontainers:1.21.4")
}

application {
    mainClass.set("com.chat5000.Application")
    applicationDefaultJvmArgs = listOf(
        "-Djdk.tls.namedGroups=X25519MLKEM768,x25519,secp384r1"
    )
}

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.chat5000.*")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-Djdk.tls.namedGroups=X25519MLKEM768,x25519,secp384r1")
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

tasks.register("verifyNoSpring") {
    group = "verification"
    description = "Fails the build if any org.springframework dependency is present."
    doLast {
        val springDeps = configurations.runtimeClasspath.get()
            .resolvedConfiguration.resolvedArtifacts
            .filter { it.moduleVersion.id.group.startsWith("org.springframework") }
        if (springDeps.isNotEmpty()) {
            throw GradleException("Forbidden Spring dependencies detected: ${springDeps.map { it.moduleVersion.id }}")
        }
        println("Verified: zero Spring dependencies in runtimeClasspath.")
    }
}

tasks.named("check") {
    dependsOn("verifyNoSpring")
}
