plugins {
    java
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.freefair.lombok") version "8.14"
    id("org.springframework.boot.experimental.thin-launcher") version "1.0.31.RELEASE"
    `maven-publish`
}

group = "com.thatgamerblue.subauth"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("io.jsonwebtoken:jjwt-gson:0.12.6")

    implementation("commons-codec:commons-codec:1.18.0")
    implementation("org.apache.commons:commons-collections4:4.5.0")

    implementation("com.google.guava:guava:33.4.8-jre")
    implementation("com.google.code.gson:gson:2.11.0")

    implementation(platform("io.projectreactor:reactor-bom:2024.0.7"))
    implementation("io.projectreactor:reactor-core")

    implementation("com.github.twitch4j:twitch4j:1.25.0")
    runtimeOnly("org.postgresql:postgresql")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}