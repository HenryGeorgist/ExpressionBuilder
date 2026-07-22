plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.0.13"
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
    
    // JavaFX UI Toolkit
    implementation("org.openjfx:javafx-controls:21")
    implementation("org.openjfx:javafx-fxml:21")
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("usace.hec.ui.ExpressionNodeExplorer")
}