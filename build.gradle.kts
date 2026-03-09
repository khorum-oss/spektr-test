import org.khorum.oss.plugins.open.publishing.digitalocean.domain.uploadToDigitalOceanSpaces
import org.khorum.oss.plugins.open.publishing.mavengenerated.domain.mavenGeneratedArtifacts
import org.khorum.oss.plugins.open.secrets.getPropertyOrEnv
import kotlin.apply

plugins {
	kotlin("jvm") version "2.3.0"
	id("dev.detekt") version "2.0.0-alpha.2"
	id("org.jetbrains.dokka") version "2.1.0"
	id("org.jetbrains.dokka-javadoc") version "2.1.0"
	id("org.jetbrains.kotlinx.kover") version "0.9.4"
	id("org.sonarqube") version "7.0.0.6105"
	id("org.khorum.oss.plugins.open.publishing.maven-generated-artifacts") version "1.0.3"
	id("org.khorum.oss.plugins.open.publishing.digital-ocean-spaces") version "1.0.3"
	id("org.khorum.oss.plugins.open.secrets") version "1.0.0"
	id("org.khorum.oss.plugins.open.pipeline") version "1.0.0"
}

group = "org.khorum.oss.spektr"
version = file("VERSION").readText().trim()

digitalOceanSpacesPublishing {
	bucket = "open-reliquary"
	accessKey = project.getPropertyOrEnv("spaces.key", "DO_SPACES_API_KEY")
	secretKey = project.getPropertyOrEnv("spaces.secret", "DO_SPACES_SECRET")
	publishedVersion = version.toString()
	isPlugin = false
	dryRun = false
}

tasks.uploadToDigitalOceanSpaces?.apply {
	val task: Task = tasks.mavenGeneratedArtifacts ?: throw Exception("mavenGeneratedArtifacts task not found")
	dependsOn(task)
}

mavenGeneratedArtifacts {
	publicationName = "digitalOceanSpaces"
	name = "Spektr Test"
	description = """
		A testing utilities library for Spektr APIs.
	"""
	websiteUrl = "https://github.com/khorum-oss/spektr-test/tree/main/src"

	licenses {
		license {
			name = "MIT License"
			url = "https://opensource.org/license/mit"
		}
	}

	developers {
		developer {
			id = "khorum-oss"
			name = "Khorum OSS Team"
			email = "khorum.oss@gmail.com"
			organization = "Khorum OSS"
		}
	}

	scm {
		connection.set("https://github.com/khorum-oss/spektr-test.git")
	}
}

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

	// Spring dependencies — compileOnly so consumers provide their own version
	compileOnly("org.springframework:spring-test:7.0.5")
	compileOnly("org.springframework:spring-web:7.0.5")
	compileOnly("org.springframework:spring-webflux:7.0.5")

	// JUnit 5
	api("org.jetbrains.kotlin:kotlin-test-junit5")
	api("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

	api("io.mockk:mockk:1.13.8")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testImplementation("org.springframework:spring-test:7.0.5")
	testImplementation("org.springframework:spring-web:7.0.5")
	testImplementation("org.springframework:spring-webflux:7.0.5")
	testImplementation("io.projectreactor:reactor-test:3.7.6")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
	useJUnitPlatform()
}

sonar {
	properties {
		property("sonar.projectKey", "khorum-oss_spektr-test")
		property("sonar.organization", "khorum-oss")
		property("sonar.host.url", "https://sonarcloud.io")
		property("sonar.coverage.jacoco.xmlReportPaths",
			"${layout.buildDirectory.get()}/reports/kover/report.xml")
	}
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

detekt {
	buildUponDefaultConfig = true
	allRules = false
	config.setFrom(files("$rootDir/detekt.yml"))
	baseline = file("$rootDir/detekt-baseline.xml")
	parallel = true
}
