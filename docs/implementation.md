# Implementation — LumaTrivia

**Date:** 2026-07-28
**Status:** Bootstrap (SPEAR init for Java→Kotlin+Nexus rewrite)
**Owner:** BadgersMC

## 1. Repo layout (canonical)

```
LumaTrivia/
├── src/main/kotlin/net/badgersmc/trivia/
│   ├── domain/             # rules of the game — zero framework imports
│   │   ├── Question.kt     # Question data class with answer validation
│   │   └── PlayerStats.kt  # PlayerStats, Comparable by points desc
│   ├── application/        # use cases — imports domain only
│   │   ├── TriviaService.kt     # Game lifecycle: start, checkAnswer, timeUp, cooldown, mute
│   │   ├── QuestionFetcher.kt   # OpenTriviaDB HTTP client + base64 decode + cache
│   │   ├── ContentFilter.kt     # Regex-based question content filtering
│   │   └── StatsService.kt      # Thin wrapper around StatsRepository
│   ├── infrastructure/     # adapters — imports anything
│   │   ├── bukkit/
│   │   │   ├── LumaTriviaPlugin.kt    # JavaPlugin entry point
│   │   │   ├── ChatListener.kt        # AsyncChatEvent handler
│   │   │   └── TriviaBukkitCommand.kt # Command("trivia") with subcommands
│   │   ├── config/
│   │   │   └── TriviaConfig.kt        # Data classes + SnakeYAML loader
│   │   ├── di/
│   │   │   └── ServiceModule.kt       # Manual DI via lazy delegates
│   │   ├── i18n/
│   │   │   └── LumaTriviaLang.kt      # @LangFile marker
│   │   └── persistence/
│   │       ├── DatabaseFactory.kt     # SQLite connection + WAL mode
│   │       ├── StatsTable.kt          # Exposed table definition
│   │       ├── StatsRepository.kt     # Interface
│   │       └── SqliteStatsRepository.kt
│   └── loader/
│       └── LumaTriviaLoader.kt    # NexusPaperPluginLoader subclass
├── src/main/resources/
│   ├── paper-plugin.yml
│   ├── config.yml
│   └── lang/en_US.yml
├── src/test/kotlin/net/badgersmc/trivia/
│   ├── domain/   (QuestionTest, PlayerStatsTest)
│   ├── application/ (TriviaServiceTest, ContentFilterTest)
│   ├── infrastructure/
│   │   ├── persistence/ (StatsRepositoryTest)
│   │   └── bukkit/ (ChatListenerTest)
│   └── architecture/ (LayerRulesTest)
├── docs/ (tech-stack, requirements, implementation, tasks)
├── build.gradle.kts
└── settings.gradle.kts
```

## 2. Layer Dependency Rules

The three-layer discipline SPEAR enforces. `/spear:arch` reads this exact section and blocks on violations.

| Layer | Concrete files | May depend on |
|---|---|---|
| `domain/` (rules-of-the-game) | `src/main/kotlin/net/badgersmc/trivia/domain/**` | nothing outside `domain/` + Kotlin stdlib |
| `application/` (use cases / workflow) | `src/main/kotlin/net/badgersmc/trivia/application/**` | `domain/` only |
| `infrastructure/` (adapters, frameworks, I/O) | `src/main/kotlin/net/badgersmc/trivia/infrastructure/**` + `loader/**` | anything |

## Forbidden Domain Annotations

```yaml
forbidden:
  - org.bukkit.**
  - net.badgersmc.nexus.**
  - com.squareup.okhttp3.**
  - com.google.gson.**
  - org.jetbrains.exposed.**
  - net.kyori.adventure.**
  - org.xerial.sqlite.**
  - com.zaxxer.hikari.**
  - java.sql.**
```

## 3. DI pattern

Manual `ServiceModule` with `lazy` delegates (EnthusiaVotes pattern). No `@Service`/`@Repository` annotations — all wiring is explicit.

## 4. i18n pattern

Nexus `LangService` with `@LangFile` marker. All messages in `lang/en_US.yml` use MiniMessage:
- `<shadow:#000000:1>` on every line
- `<gradient:#FF7272:#FFC976>` prefix: `[Trivia]`
- `<name>` angle-bracket param placeholders
- `lang.msg("key", "param" to value)` call pattern

## 5. Game state machine

```
IDLE --[startGame]--> ACTIVE(question, answeredPlayers, taskId) --[correct/timeout]--> IDLE(+cooldown)
```

## 6. Mute system

- `ConcurrentHashMap<UUID, Long>` — player UUID → mute expiry epoch millis
- Mute duration = answer-time seconds (REQ-004)
- Single repeating 1s task clears expired entries
- `lumatrivia.mute.bypass` exempts

## 7. Scheduled games

- `runTaskTimer` on MAIN thread (not async — fixes audit H1)
- 24h period, delay computed from current time to next HH:mm
- Cancel + recreate on reload
