import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("net.dv8tion", "JDA", "5.0.0-alpha.13") {
        exclude("club.minnced", "opus-java")
    }
    implementation("com.github.minndevelopment", "jda-ktx", "d5c5d9d")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}