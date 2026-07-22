plugins {
    java
    application
}

group = "usace.hec"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://www.hec.usace.army.mil/nexus/repository/maven-releases/")
    }
}

dependencies {
    implementation("mil.army.usace.hec:expressions:1.0.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("expression.builder.App")
}

tasks.test {
    useJUnitPlatform()
}