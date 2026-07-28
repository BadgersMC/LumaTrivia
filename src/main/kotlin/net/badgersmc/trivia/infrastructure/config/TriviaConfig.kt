package net.badgersmc.trivia.infrastructure.config

/** Config data classes — loaded from config.yml via SnakeYAML (INFRA-08). */
data class ApiConfig(val url: String, val batchSize: Int, val timeoutMs: Long)
data class GameConfig(
    val answerTime: Int, val cooldown: Int,
    val muteIncorrect: MuteIncorrectConfig,
    val schedule: ScheduleConfig,
    val categories: List<Int>, val difficulties: List<String>,
)
data class MuteIncorrectConfig(val enabled: Boolean)
data class ScheduleConfig(val enabled: Boolean, val times: List<String>)
data class RewardConfig(val commands: List<String>, val points: Int)
data class ContentFilterConfig(
    val enabled: Boolean, val logFiltered: Boolean,
    val blockedPatterns: List<String>, val requiredPattern: String,
)
data class StorageConfig(val backend: String, val file: String)
data class TriviaConfig(
    val api: ApiConfig, val game: GameConfig,
    val rewards: Map<String, RewardConfig>,
    val contentFilter: ContentFilterConfig,
    val storage: StorageConfig,
)
