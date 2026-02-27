plugins {
	kotlin("jvm") version "2.3.0"
	kotlin("plugin.spring") version "2.3.0"
	id("org.springframework.boot") version "4.1.0-M1"
	id("io.spring.dependency-management") version "1.1.7"
	id("dev.detekt") version "2.0.0-alpha.2"
	id("org.jetbrains.dokka") version "2.1.0"
	id("org.jetbrains.dokka-javadoc") version "2.1.0"
	id("org.jetbrains.kotlinx.kover") version "0.7.6"
	id("org.khorum.oss.plugins.open.publishing.maven-generated-artifacts") version "1.0.0"
	id("org.khorum.oss.plugins.open.publishing.digital-ocean-spaces") version "1.0.0"
	id("org.khorum.oss.plugins.open.secrets") version "1.0.0"
	id("org.khorum.oss.plugins.open.pipeline") version "1.0.0"
}

group = "org.khorum.oss.spektr"
version = file("VERSION").readText().trim()

// Root project is not a Spring Boot application
tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

repositories {
	mavenCentral()
	maven {
		url = uri("https://open-reliquary.nyc3.cdn.digitaloceanspaces.com")
	}
}

dependencies {
	implementation("io.github.microutils:kotlin-logging:4.0.0-beta-2")
	// DSL needed so users can reference EndpointModule in their test code
	implementation("org.khorum.oss.spektr:spektr-dsl:1.0.4")

	// Testcontainers
	api("org.testcontainers:testcontainers:2.0.3")
	api("org.testcontainers:junit-jupiter:1.21.4")

	// Spring Boot test support (WebFlux + Testcontainers integration)
	api("org.springframework.boot:spring-boot-testcontainers")
	api("org.springframework.boot:spring-boot-starter-webflux-test")
	api("org.springframework.boot:spring-boot-starter-actuator-test")

	// JUnit 5
	api("org.jetbrains.kotlin:kotlin-test-junit5")
	api("org.jetbrains.kotlinx:kotlinx-coroutines-test")
	api("io.projectreactor:reactor-test")

	api("io.mockk:mockk:1.13.8")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
	useJUnitPlatform()
}

// Disable Kover instrumentation globally to avoid race condition
// with kover-agent.args file during parallel builds (Kover 0.7.x bug)
extensions.configure<kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension> {
	disable()
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

detekt {
	buildUponDefaultConfig = true
	allRules = false
	config.setFrom(files("$rootDir/detekt.yml"))
	baseline = file("$rootDir/detekt-baseline.xml")
	parallel = true
}
