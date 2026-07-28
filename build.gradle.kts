import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.1.0"
    id("com.gradleup.shadow") version "8.3.5"
}

group = "net.badgersmc.trivia"
version = findProperty("releaseVersion")?.toString() ?: "1.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")

    if (providers.gradleProperty("useMavenLocal").orNull == "true") {
        mavenLocal()
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // Nexus (shaded — tiny without transitive deps excluded below)
    implementation("com.github.BadgersMC.Nexus:nexus-core:v2.1.1")
    implementation("com.github.BadgersMC.Nexus:nexus-paper:v2.1.1")
    implementation("com.github.BadgersMC.Nexus:nexus-scheduler:v2.1.1")
    implementation("com.github.BadgersMC.Nexus:nexus-i18n:v2.1.1")
    implementation("com.github.BadgersMC.Nexus:nexus-paper-loader:v2.1.1")

    // Runtime-resolved by NexusPaperPluginLoader (declared in LumaTriviaLoader)
    compileOnly("com.squareup.okhttp3:okhttp:4.12.0")
    compileOnly("com.google.code.gson:gson:2.10.1")
    compileOnly("org.xerial:sqlite-jdbc:3.45.1.0")
    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.jetbrains.exposed:exposed-core:0.55.0")
    compileOnly("org.jetbrains.exposed:exposed-dao:0.55.0")
    compileOnly("org.jetbrains.exposed:exposed-jdbc:0.55.0")
    compileOnly("org.jetbrains.exposed:exposed-java-time:0.55.0")

    // Kotlin + coroutines (PaperLoader downloads at runtime)
    compileOnly(kotlin("stdlib"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Adventure (bundled with Paper)
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")

    // RoseChat (optional compileOnly — softdepend at runtime)
    compileOnly(files("libs/RoseChat-RC-2.jar"))

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.21:3.127.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("com.lemonappdev:konsist:0.17.3")
    testImplementation("org.xerial:sqlite-jdbc:3.45.1.0")
    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("com.google.code.gson:gson:2.10.1")
    testImplementation("com.zaxxer:HikariCP:5.1.0")
    testImplementation("org.jetbrains.exposed:exposed-core:0.55.0")
    testImplementation("org.jetbrains.exposed:exposed-dao:0.55.0")
    testImplementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")
    testImplementation("org.jetbrains.exposed:exposed-java-time:0.55.0")
}

kotlin { jvmToolchain(21) }
tasks.test { useJUnitPlatform() }

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    mergeServiceFiles()
    // Strip Nexus transitive deps — resolved at runtime by NexusPaperPluginLoader.
    // Keep Nexus JARs themselves (nexus-core, nexus-paper, etc.).
    dependencies {
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib:.*"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-reflect:.*"))
        exclude(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:.*"))
        exclude(dependency("com.charleskorn.kaml:kaml-jvm:.*"))
        exclude(dependency("io.github.classgraph:classgraph:.*"))
        exclude(dependency("org.slf4j:slf4j-api:.*"))
    }
    // Do NOT relocate nexus — Kotlin @Metadata breaks on relocation
}

tasks.processResources {
    filesMatching("paper-plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.build { dependsOn(tasks.shadowJar) }
