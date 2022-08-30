import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
}

application {
    mainClass.set("BotKt")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("net.dv8tion", "JDA", "5.0.0-alpha.18") {
        exclude("club.minnced", "opus-java")
    }
    implementation("com.github.minndevelopment", "jda-ktx", "081a177")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

