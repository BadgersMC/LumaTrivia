# Requirements — LumaTrivia

**Date:** 2026-07-28
**Status:** Bootstrap (SPEAR init for Java→Kotlin+Nexus rewrite)
**EARS subset enforced:** Ubiquitous, Event-driven, State-driven, Unwanted.

Each requirement carries a stable ID. Tasks reference requirements by ID. New requirements append at the next free integer ID (three-digit padded); IDs are never re-used or renumbered.

---

## Product

### REQ-001 — Trivia game start

**Event-driven.** WHEN an operator or scheduled timer triggers a game start THE SYSTEM SHALL fetch a cached question, announce it in chat with shuffled multiple-choice options (A/B/C/D or True/False), and start a countdown timer for the configured answer duration.

### REQ-002 — Answer validation

**Event-driven.** WHEN a player sends a chat message during an active game THE SYSTEM SHALL interpret single-letter answers (a/b/c/d) or true/false answers (t/f/true/false) as trivia answers, cancel the chat message if it matches the answer format, and evaluate the answer against the current question.

### REQ-003 — Correct answer handling

**Event-driven.** WHEN a player answers correctly THE SYSTEM SHALL end the current question, announce the winner and the correct answer, award points based on difficulty, execute configured reward commands, update persistent player stats, and unmute all players.

### REQ-004 — Wrong answer muting

**Event-driven.** WHEN a player answers incorrectly and mute-incorrect is enabled THE SYSTEM SHALL mute the player's chat for the remaining question duration and broadcast the wrong-answer announcement. THE SYSTEM SHALL respect the `lumatrivia.mute.bypass` permission.

### REQ-005 — Time-up handling

**Event-driven.** WHEN the answer timer expires without a correct answer THE SYSTEM SHALL announce the correct answer, end the question, and unmute all players.

### REQ-006 — One answer per player

**State-driven.** WHILE a question is active THE SYSTEM SHALL track which players have already answered and reject subsequent answers from the same player.

### REQ-007 — Game cooldown

**State-driven.** WHILE the global cooldown timer is active THE SYSTEM SHALL reject manual game-start commands and inform the player of the remaining cooldown duration.

### REQ-008 — Question fetching and caching

**Event-driven.** WHEN the question cache is empty at game start THE SYSTEM SHALL asynchronously fetch a batch of questions from the OpenTriviaDB API, decode base64-encoded fields, apply content filtering, cache the results, and automatically start the game when questions arrive. THE SYSTEM SHALL retry with a delay if all questions are filtered.

### REQ-009 — Content filtering

**Ubiquitous.** THE SYSTEM SHALL filter fetched questions against configurable regex blocked-patterns (case-insensitive) and an optional required-pattern. Filtered questions SHALL be excluded from the cache and logged when log-filtered is enabled.

### REQ-010 — Player statistics

**Ubiquitous.** THE SYSTEM SHALL persist per-player trivia statistics (total correct, easy/medium/hard breakdown, total points) to SQLite across server restarts and expose them via the `/trivia stats` command.

### REQ-011 — Leaderboard

**Ubiquitous.** THE SYSTEM SHALL compute and display a top-N leaderboard of players ranked by total points descending via the `/trivia top` command.

### REQ-012 — Scheduled games

**Event-driven.** WHEN the schedule is enabled THE SYSTEM SHALL automatically start trivia games at each configured time-of-day (HH:mm) on the main server thread.

### REQ-013 — Permission model

**Ubiquitous.** THE SYSTEM SHALL enforce:
- `lumatrivia.use` (default: true) — access to `/trivia stats` and `/trivia top`
- `lumatrivia.start` (default: op) — access to `/trivia start`
- `lumatrivia.admin` (default: op) — access to `/trivia reload`; inherits start + use
- `lumatrivia.mute.bypass` (no default) — exempt from wrong-answer muting

### REQ-014 — Configuration reload

**Event-driven.** WHEN an operator runs `/trivia reload` THE SYSTEM SHALL reload the config.yml from disk and apply new values to the running plugin without a server restart.

### REQ-015 — Reward commands

**Event-driven.** WHEN a player answers correctly THE SYSTEM SHALL dispatch configured reward commands via the console sender on the main thread, substituting `%player%` with the winner's name.

### REQ-016 — i18n via MiniMessage

**Ubiquitous.** THE SYSTEM SHALL render all player-facing messages using the Nexus LangService with MiniMessage-formatted locale strings from `lang/en_US.yml`. All messages SHALL include `<shadow:#000000:1>` for readability. THE SYSTEM SHALL return `Component.empty()` for missing locale keys rather than throwing.

### REQ-017 — Nexus framework integration

**Ubiquitous.** THE SYSTEM SHALL load via `NexusPaperPluginLoader`, use `NexusScheduler` for all scheduled tasks (auto-cancel on disable), and register commands via `server.commandMap.register()` (NOT `getCommand()`).

### REQ-018 — Stats persistence

**Ubiquitous.** THE SYSTEM SHALL persist player statistics to a SQLite database at `plugins/LumaTrivia/stats.db` using Exposed ORM with WAL mode and busy_timeout for concurrent access safety.

### REQ-019 — Chat event handling

**Event-driven.** WHEN a player sends chat during an active game THE SYSTEM SHALL process the message through a single `AsyncChatEvent` listener at `LOWEST` priority with `ignoreCancelled = true`, handling both mute enforcement and answer parsing in one handler.
