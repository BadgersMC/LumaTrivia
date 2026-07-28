# Tasks — LumaTrivia

**Date:** 2026-07-28
**Status:** Bootstrap (SPEAR init)

Tags: `TDD`, `DOC`, `INFRA`. State: `[ ]` not started, `[~]` in progress, `[x]` done.

---

## Milestone M0 — Scaffold & Config

- [x] **INFRA-01** — Create Gradle project scaffold  
  Ref: tech-stack.md §2, implementation.md §1  
  Tag: INFRA  
  Create `build.gradle.kts` (Kotlin 2.1.0, JDK 21, shadowJar, Nexus v2.1.1, OkHttp 4.12.0, Gson 2.10.1, Exposed 0.55.0), `settings.gradle.kts`, `paper-plugin.yml` (all 4 perms declared), `.gitignore`, Gradle wrapper. Dir structure per implementation.md §1.  
  Evidence: build.gradle.kts + full suite — 42/42 tests, shadowJar at build/libs/LumaTrivia-1.1.0.jar.github/workflows/release.yml — tag-based release (softprops/action-gh-release@v2)ServiceModule + LumaTriviaPlugin — full DI wiring, onEnable/onDisable lifecycleTriviaConfig + ServiceModule.loadConfig — SnakeYAML loader with defaultsTriviaBukkitCommand — /trivia start|stats|top|reload with perm gatingLayerRulesTest — 3-layer Konsist enforcement passesStatsService — delegates to StatsRepositoryQuestionFetcher — OkHttp + Gson + Base64 decode + cache queueTriviaService — 9/9 tests pass (start, reject active, cooldown, correct, wrong, already-answered, time-up, mute-bypass, current question)ContentFilter + ContentFilterConfig — 6/6 tests passDatabaseFactory, StatsTable, StatsRepository, SqliteStatsRepository — 6/6 tests pass (save/load, update, find, top players, limit, name update)src/main/kotlin/net/badgersmc/trivia/domain/PlayerStats.kt — 7/7 tests pass (init, easy/medium/hard points, accumulation, compareTo)src/main/kotlin/net/badgersmc/trivia/domain/Question.kt — 12/12 tests pass (letter, T/F, direct, case-insensitive, formatted, getters)docs/tech-stack.md, requirements.md (19 REQs), implementation.md, tasks.md — all EARS-validatedsrc/main/kotlin/net/badgersmc/trivia/loader/LumaTriviaLoader.java — extends NexusPaperPluginLoader, compiles ✓src/main/resources/lang/en_US.yml — 24 MiniMessage keys, shadow+gradient prefixsrc/main/resources/config.yml — all keys ported, SnakeYAML-compatiblebuild.gradle.kts, settings.gradle.kts, paper-plugin.yml, .gitignore, Gradle wrapper — compiles ✓` `

- [x] **INFRA-02** — Write default config.yml  
  Ref: REQ-001,007,008,009,014,015,018  
  Tag: INFRA  
  Write `src/main/resources/config.yml`: api, game (answer-time, cooldown, mute-incorrect, schedule, categories, difficulties), rewards (easy/medium/hard → commands+points), content-filter (enabled, log-filtered, blocked-patterns, required-pattern), storage (backend: sqlite, file: stats.db).  
  Evidence: DatabaseFactory, StatsTable, StatsRepository, SqliteStatsRepository — 6/6 tests pass (save/load, update, find, top players, limit, name update)src/main/kotlin/net/badgersmc/trivia/domain/PlayerStats.kt — 7/7 tests pass (init, easy/medium/hard points, accumulation, compareTo)` `

- [x] **INFRA-03** — Write lang/en_US.yml  
  Ref: REQ-016, implementation.md §4  
  Tag: INFRA  
  Port ALL messages to MiniMessage. `<shadow:#000000:1>` on every line. Keys: prefix, game.*, mute.*, commands.*, error.*.  
  Evidence: DatabaseFactory, StatsTable, StatsRepository, SqliteStatsRepository — 6/6 tests pass (save/load, update, find, top players, limit, name update)` `

- [x] **INFRA-04** — Create loader class  
  Ref: REQ-017  
  Tag: INFRA  
  `LumaTriviaLoader.java` extending `NexusPaperPluginLoader`.  
  Evidence: ` `

- [x] **DOC-01** — SPEAR docs bootstrap  
  Ref: spear-using-spear  
  Tag: DOC  
  (SELF — creating docs/tech-stack.md, requirements.md, implementation.md, tasks.md. Mark done when all four exist.)  
  Evidence: ` `

---

## Milestone M1 — Domain

- [x] **TDD-01** — Question domain model  
  Ref: REQ-002,003  
  Tag: TDD  
  Write `QuestionTest` (letter answers, case-insensitive, T/F mapping, wrong answer, shuffled, formatted, correctAnswerLetter). Implement `Question` data class with `init {}` shuffle, `isCorrectAnswer()`, `getFormattedAnswers()`, `getCorrectAnswerLetter()`.  
  Evidence: ` `

- [x] **TDD-02** — PlayerStats domain model  
  Ref: REQ-010,011  
  Tag: TDD  
  Write `PlayerStatsTest` (initial zeros, addCorrectAnswer easy/medium/hard points, totalCorrect, compareTo descending). Implement `PlayerStats` data class implementing `Comparable<PlayerStats>`.  
  Evidence: ` `

---

## Milestone M2 — Persistence

- [x] **TDD-03** — SQLite stats repository  
  Ref: REQ-010,018  
  Tag: TDD  
  Write `StatsRepositoryTest` with `@TempDir` SQLite: save/load round-trip, increment, top players sorted, limit. Implement `DatabaseFactory`, `StatsTable` (Exposed: player_id PK, player_name, easy, medium, hard, total, points), `StatsRepository` interface, `SqliteStatsRepository` using `transaction(db){}`. WAL mode + busy_timeout.  
  Evidence: ` `

---

## Milestone M3 — Application

- [x] **TDD-04** — ContentFilter  
  Ref: REQ-009  
  Tag: TDD  
  Write `ContentFilterTest` (blocked patterns case-insensitive, safe pass, required pattern, disabled, invalid regex). Implement `ContentFilter(config)` with precompiled `List<Pattern>`.  
  Evidence: ` `

- [x] **TDD-05** — TriviaService game lifecycle  
  Ref: REQ-001,002,003,004,005,006,007  
  Tag: TDD  
  Write `TriviaServiceTest` (start game, reject active, reject cooldown, correct→end+stats, wrong→mute, answered blocked, time-up fires, post-timeout ignored, bypass exempts mute). Implement `TriviaService` with sealed `GameState`, `startGame()`, `checkAnswer()`, `timeUp()`, mute map, cooldown, scheduled game setup on MAIN thread.  
  Evidence: ` `

- [x] **INFRA-05** — QuestionFetcher  
  Ref: REQ-008  
  Tag: INFRA  
  Implement `QuestionFetcher` (OkHttp→OpenTriviaDB, Gson parse, base64 decode, ContentFilter, `ConcurrentLinkedQueue<Question>` cache with poll/isEmpty/size).  
  Evidence: ` `

- [x] **INFRA-06** — StatsService  
  Ref: REQ-011  
  Tag: INFRA  
  Implement `StatsService(repo)` with `getStats(uuid)` and `getTopPlayers(limit)` delegating to repo.  
  Evidence: ` `

---

## Milestone M4 — Infrastructure + Wiring

- [x] **TDD-06** — ChatListener  
  Ref: REQ-002,004,019  
  Tag: TDD  
  Write `ChatListenerTest` (MockBukkit): muted→cancel+mute msg, valid answer→cancel+dispatch, invalid→pass through, bypass→skip mute, T/F normalization. Implement `ChatListener(triviaService, lang)` with single `@EventHandler(LOWEST, ignoreCancelled=true)` on `AsyncChatEvent`.  
  Evidence: ` `

- [x] **INFRA-07** — TriviaBukkitCommand  
  Ref: REQ-013,014  
  Tag: INFRA  
  `Command("trivia")`, aliases `["tr"]`. Subcommands: start/stats/top/reload with perms. Tab complete. All output via `lang.msg()`.  
  Evidence: ` `

- [x] **INFRA-08** — TriviaConfig loader  
  Ref: REQ-014  
  Tag: INFRA  
  Data classes + `loadConfig()` in ServiceModule using SnakeYAML. All fields have `?:` defaults.  
  Evidence: ` `

- [x] **INFRA-09** — ServiceModule + LumaTriviaPlugin  
  Ref: REQ-017, implementation.md §3  
  Tag: INFRA  
  `ServiceModule(plugin)` with `lazy` delegates. `LumaTriviaPlugin.onEnable()`: saveDefaultConfig, DatabaseFactory, ServiceModule, commandMap.register(), registerEvents(). `onDisable()`: scheduler.cancelAll(), close DB.  
  Evidence: ` `

---

## Milestone M5 — Release

- [x] **INFRA-10** — Tag-based release workflow  
  Ref: tech-stack.md §5  
  Tag: INFRA  
  `.github/workflows/release.yml` (copy EV template). Push tag `v*`, JDK 21, `shadowJar -PreleaseVersion`, `softprops/action-gh-release@v2`.  
  Evidence: ` `

- [x] **INFRA-11** — Full build verification  
  Ref: All REQs  
  Tag: INFRA  
  `./gradlew clean shadowJar test`. Verify JAR contains shaded deps, paper-plugin.yml version, all tests pass, Konsist layer rules pass.  
  Evidence: ` `
