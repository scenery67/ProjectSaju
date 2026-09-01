plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "io.sj"
version = "0.0.1-SNAPSHOT"
description = "Saju (Korean fortune-telling) service backend"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	// 스키마는 Flyway가 소유한다 — ddl-auto는 validate로만 쓴다.
	// Spring Boot 4는 flyway-core만으로는 오토컨피그가 안 되고
	// spring-boot-starter-flyway가 따로 있어야 스캔된다 (spring-boot-flyway
	// 모듈로 오토컨피그가 분리됨 — 없으면 예외 없이 조용히 무시된다).
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.flywaydb:flyway-database-postgresql")
	// 만세력/팔자(사주) 계산 — 절기 기준 정밀 계산 포함. MIT license.
	// https://github.com/6tail/lunar-java
	implementation("cn.6tail:lunar:1.7.7")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
