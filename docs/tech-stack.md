# Tech Stack — LumaTrivia

**Date:** 2026-07-28
**Status:** Bootstrap (SPEAR init for Java→Kotlin+Nexus rewrite)
**Owner:** BadgersMC

## 1. What this project is

LumaTrivia is a Paper plugin that runs trivia games in chat, powered by the OpenTriviaDB API. Players answer multiple-choice and true/false questions, earn points per difficulty tier, and compete on a leaderboard. Supports scheduled automatic games, content filtering, wrong-answer muting, and configurable reward commands. Consumed by survival-economy players on a Paper 1.21.x server. Deployment target: BadgersMC production network.

This is a **greenfield rewrite** of the existing Java LumaTrivia plugin (v1.0.1) into Kotlin using the BadgersMC Nexus framework.

## 2. Runtimes & languages

| Layer | Language / Tool | Min version | Reason |
|---|---|---|---|
| Plugin | Kotlin | 2.1.0 | Concise domain model, null safety |
| Build tool | Gradle (Kotlin DSL) + Shadow | 8.x / 8.3.5 | Standard for Paper plugins; Shadow relocates Nexus + OkHttp + Gson |
| Test framework | JUnit 5 + MockK + MockBukkit | 5.10.0 / 1.13.10 / 3.127.0 | Idiomatic Kotlin tests + Bukkit-API simulation |
| DI + Scheduler + i18n | **Nexus** (nexus-core + nexus-paper + nexus-scheduler + nexus-i18n + nexus-paper-loader) | v2.1.1 | Internal BadgersMC framework |
| Config loading | SnakeYAML (bundled with Bukkit) | — | EV-style manual config parsing |
| ORM | Exposed (0.55.0) | 0.55.0 | SQLite stats persistence |
| HTTP client | OkHttp | 4.12.0 | OpenTriviaDB API calls |
| JSON | Gson | 2.10.1 | OpenTriviaDB response parsing |
| JVM | JDK 21 | — | Paper 1.21.x minimum |

## 3. Runtime dependencies

| Package | Version | Why |
|---|---|---|
| io.papermc.paper:paper-api | 1.21.11-R0.1-SNAPSHOT | Server API (compileOnly) |
| com.github.BadgersMC.Nexus:nexus-core | v2.1.1 | DI container (shaded, relocated) |
| com.github.BadgersMC.Nexus:nexus-paper | v2.1.1 | Paper command registration utilities (shaded, relocated) |
| com.github.BadgersMC.Nexus:nexus-scheduler | v2.1.1 | NexusScheduler — auto-cancel on disable (shaded, relocated) |
| com.github.BadgersMC.Nexus:nexus-i18n | v2.1.1 | LangService + @LangFile marker (shaded, relocated) |
| com.github.BadgersMC.Nexus:nexus-paper-loader | v2.1.1 | NexusPaperPluginLoader base class (shaded, relocated) |
| com.squareup.okhttp3:okhttp | 4.12.0 | HTTP client (shaded) |
| com.google.code.gson:gson | 2.10.1 | JSON parsing (shaded) |
| org.xerial:sqlite-jdbc | 3.45.1.0 | SQLite driver (shaded) |
| com.zaxxer:HikariCP | 5.1.0 | Connection pool (shaded) |
| org.jetbrains.exposed:exposed-* | 0.55.0 | ORM (shaded) |
| net.kyori:adventure-* | 4.17.0 | Chat components + MiniMessage (compileOnly, Paper) |

## 4. Versioning

Semantic versioning. Starts at 1.1.0 (continuation from Java v1.0.1).

## 5. CI

GitHub Actions — tag-based release at `.github/workflows/release.yml`:
1. Push tag `v*` triggers build
2. `./gradlew shadowJar` with `-PreleaseVersion=${{ github.ref_name }}`
3. Upload JAR via `softprops/action-gh-release@v2`

## 6. Out of stack

- Spring / Jakarta / Micronaut
- Koin DI (use manual ServiceModule)
- Hibernate/JPA (use Exposed ORM)
- MariaDB / remote DB (SQLite only)
- Geyser/Floodgate/Cumulus (not needed for chat-based trivia)
- PlaceholderAPI / Vault (reward commands dispatch via console)
- Non-Paper server forks
