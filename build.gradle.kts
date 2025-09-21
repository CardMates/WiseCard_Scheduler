import com.google.protobuf.gradle.*

plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.4"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "1.9.25"
	id("com.google.protobuf") version "0.9.4"
}

group = "com.wisecard"
version = "0.0.1-SNAPSHOT"
description = "wise card scheduler"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// spring boot
	implementation("org.springframework.boot:spring-boot-starter-web")

	// kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	// test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// csv
	implementation("com.opencsv:opencsv:5.12.0")

	// protobuf
	implementation("io.grpc:grpc-kotlin-stub:1.4.3")
	implementation("io.grpc:grpc-netty-shaded:1.63.0")
	implementation("io.grpc:grpc-protobuf:1.63.0")
	implementation("io.grpc:grpc-stub:1.63.0")

	implementation("net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE")

	// gemini
	implementation("com.google.genai:google-genai:1.15.0")

	//jsoup
	implementation("org.jsoup:jsoup:1.21.2")

	//selenium
	implementation("org.seleniumhq.selenium:selenium-java:4.35.0")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

sourceSets{
	getByName("main"){
		java {
			srcDirs(
				"build/generated/source/proto/main/java",
				"build/generated/source/proto/main/kotlin"
			)
		}
	}
}

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:3.25.3"
	}
	plugins {
		id("grpc") {
			artifact = "io.grpc:protoc-gen-grpc-java:1.60.0"
		}
		id("grpckt") {
			artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.3:jdk8@jar"
		}
	}
	generateProtoTasks {
		all().forEach {
			it.plugins {
				id("grpc")
				id("grpckt")
			}
		}
	}
}