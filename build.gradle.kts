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

tasks.register<Jar>("uberJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier.set("uber")

    manifest {
        attributes["Main-Class"] = "BotKt"
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)

    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}
