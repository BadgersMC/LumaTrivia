package net.badgersmc.trivia.loader;

import net.badgersmc.nexus.paper.loader.NexusPaperPluginLoader;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Declares LumaTrivia-specific runtime libraries resolved by Paper's
 * MavenLibraryResolver at server startup. The Nexus base class contributes:
 * kotlin-stdlib, kotlin-reflect, kotlinx-coroutines-core-jvm, kaml-jvm,
 * classgraph, slf4j-api.
 *
 * <p>Keep the {@code additionalLibraries()} coordinates in sync with the
 * {@code shadowJar} excludes in build.gradle.kts — anything declared here is
 * resolved at runtime, so it does NOT need to ship in the fat jar.
 *
 * <p>THIS MUST BE JAVA, not Kotlin. The loader runs before kotlin-stdlib
 * is on the classpath, so any Kotlin bytecode here causes
 * {@code NoClassDefFoundError: kotlin/collections/CollectionsKt}.
 */
@SuppressWarnings("UnstableApiUsage")
public class LumaTriviaLoader extends NexusPaperPluginLoader {

    @Override
    @NotNull
    protected List<String> additionalLibraries() {
        return List.of(
                "com.squareup.okhttp3:okhttp:4.12.0",
                "com.squareup.okio:okio:3.9.1",
                "com.squareup.okio:okio-jvm:3.9.1",
                "com.google.code.gson:gson:2.10.1",
                "org.xerial:sqlite-jdbc:3.45.1.0",
                "com.zaxxer:HikariCP:5.1.0",
                "org.jetbrains.exposed:exposed-core:0.55.0",
                "org.jetbrains.exposed:exposed-dao:0.55.0",
                "org.jetbrains.exposed:exposed-jdbc:0.55.0",
                "org.jetbrains.exposed:exposed-java-time:0.55.0"
        );
    }
}
