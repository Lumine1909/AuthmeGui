plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.2"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
}

group = "io.github.lumine1909"
version = "1.1-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
    compileOnly("fr.xephi:authme:5.6.1-SNAPSHOT")
}


tasks {
    assemble {
        dependsOn(shadowJar)
    }
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}