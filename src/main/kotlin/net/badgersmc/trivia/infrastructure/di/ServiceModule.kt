package net.badgersmc.trivia.infrastructure.di

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.nexus.i18n.Locale
import net.badgersmc.nexus.scheduler.NexusScheduler
import net.badgersmc.trivia.application.*
import net.badgersmc.trivia.infrastructure.bukkit.ChatListener
import net.badgersmc.trivia.infrastructure.bukkit.LumaTriviaPlugin
import net.badgersmc.trivia.infrastructure.bukkit.RoseChatPlatform
import net.badgersmc.trivia.infrastructure.bukkit.VanillaChatPlatform
import net.badgersmc.trivia.infrastructure.config.*
import net.badgersmc.trivia.infrastructure.i18n.LumaTriviaLang
import net.badgersmc.trivia.infrastructure.persistence.DatabaseFactory
import net.badgersmc.trivia.infrastructure.persistence.SqliteStatsRepository
import net.badgersmc.trivia.infrastructure.persistence.StatsRepository
import okhttp3.OkHttpClient
import org.bukkit.Bukkit
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.util.concurrent.TimeUnit

/** Manual DI wiring via lazy delegates (EnthusiaVotes pattern). */
class ServiceModule(val plugin: LumaTriviaPlugin) {

    val nexusScheduler = NexusScheduler(plugin)

    val lang: LangService by lazy {
        LangService(plugin, Locale("en_US"), LumaTriviaLang::class.java)
    }

    // Mutable config — set by loadConfig() at init and reloadConfig() at runtime
    lateinit var config: TriviaConfig
        private set

    init {
        loadConfig()
    }

    /** Reload config from disk, rebuild dependent components. Returns new config. */
    fun reloadConfig(): TriviaConfig {
        loadConfig()
        // Rebuild all config-dependent components (lazy vals capture old config at first access)
        rebuildConfigDependentComponents()
        return config
    }

    // Config-dependent components — rebuilt on reload
    var contentFilter: ContentFilter = ContentFilter(config.contentFilter)
        private set
    var httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(config.api.timeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(config.api.timeoutMs, TimeUnit.MILLISECONDS)
        .build()
        private set
    var questionFetcher: QuestionFetcher = QuestionFetcher(httpClient, config.api.url, config.api.batchSize, contentFilter)
        private set

    val databaseFactory: DatabaseFactory by lazy {
        DatabaseFactory(plugin.dataFolder, config.storage.file)
    }

    val statsRepository: StatsRepository by lazy {
        SqliteStatsRepository(databaseFactory)
    }

    val statsService: StatsService by lazy {
        StatsService(statsRepository)
    }

    val triviaService: TriviaService by lazy {
        TriviaService(plugin, config, questionFetcher, statsRepository, nexusScheduler, chatPlatform)
    }

    val chatPlatform: ChatPlatform by lazy {
        val channel = config.game.channel
        if (Bukkit.getPluginManager().getPlugin("RoseChat") != null) {
            plugin.logger.info("RoseChat detected — using channel-scoped chat platform (channel: $channel)")
            RoseChatPlatform(channel)
        } else {
            plugin.logger.info("Using vanilla chat platform")
            VanillaChatPlatform(plugin)
        }
    }

    val chatListener: ChatListener by lazy {
        ChatListener(triviaService, lang, chatPlatform)
    }

    private fun rebuildConfigDependentComponents() {
        contentFilter = ContentFilter(config.contentFilter)
        httpClient = OkHttpClient.Builder()
            .connectTimeout(config.api.timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(config.api.timeoutMs, TimeUnit.MILLISECONDS)
            .build()
        questionFetcher = QuestionFetcher(httpClient, config.api.url, config.api.batchSize, contentFilter)
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadConfig(): TriviaConfig {
        val configFile = File(plugin.dataFolder, "config.yml")
        if (!configFile.exists()) {
            plugin.dataFolder.mkdirs()
            plugin.saveResource("config.yml", false)
        }

        config = try {
            val yaml = Yaml()
            val data: Map<String, Any> = yaml.load(configFile.reader()) as Map<String, Any>

            val api = (data["api"] as? Map<String, Any>)?.let {
                ApiConfig(
                    url = it["url"]?.toString() ?: "https://opentdb.com/api.php",
                    batchSize = (it["batch-size"] as? Number)?.toInt() ?: 24,
                    timeoutMs = (it["timeout-ms"] as? Number)?.toLong() ?: 10000,
                )
            } ?: ApiConfig("https://opentdb.com/api.php", 24, 10000)

            val game = (data["game"] as? Map<String, Any>)?.let { g ->
                val mute = (g["mute-incorrect"] as? Map<String, Any>)?.let {
                    MuteIncorrectConfig(enabled = it["enabled"] as? Boolean ?: true)
                } ?: MuteIncorrectConfig(true)
                val schedule = (g["schedule"] as? Map<String, Any>)?.let {
                    ScheduleConfig(
                        enabled = it["enabled"] as? Boolean ?: false,
                        times = (it["times"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                    )
                } ?: ScheduleConfig(false, emptyList())
                GameConfig(
                    answerTime = (g["answer-time"] as? Number)?.toInt() ?: 30,
                    cooldown = (g["cooldown"] as? Number)?.toInt() ?: 300,
                    muteIncorrect = mute,
                    schedule = schedule,
                    channel = g["channel"]?.toString() ?: "global",
                    categories = (g["categories"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList(),
                    difficulties = (g["difficulties"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                )
            } ?: GameConfig(30, 300, MuteIncorrectConfig(true), ScheduleConfig(false, emptyList()), "global", emptyList(), emptyList())

            val rewards: Map<String, RewardConfig> = (data["rewards"] as? Map<String, Any>)?.mapValues { (_, v) ->
                val r = v as? Map<String, Any> ?: return@mapValues RewardConfig(emptyList(), 0)
                RewardConfig(
                    commands = (r["commands"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                    points = (r["points"] as? Number)?.toInt() ?: 0,
                )
            } ?: emptyMap()

            val cf = (data["content-filter"] as? Map<String, Any>)?.let {
                ContentFilterConfig(
                    enabled = it["enabled"] as? Boolean ?: true,
                    logFiltered = it["log-filtered"] as? Boolean ?: true,
                    blockedPatterns = (it["blocked-patterns"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                    requiredPattern = it["required-pattern"]?.toString() ?: "",
                )
            } ?: ContentFilterConfig(true, true, emptyList(), "")

            val storage = (data["storage"] as? Map<String, Any>)?.let {
                StorageConfig(
                    backend = it["backend"]?.toString() ?: "sqlite",
                    file = it["file"]?.toString() ?: "stats.db",
                )
            } ?: StorageConfig("sqlite", "stats.db")

            TriviaConfig(api, game, rewards, cf, storage)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to load config.yml: ${e.message}, using defaults")
            TriviaConfig(
                api = ApiConfig("https://opentdb.com/api.php", 24, 10000),
                game = GameConfig(30, 300, MuteIncorrectConfig(true), ScheduleConfig(false, emptyList()), "global", emptyList(), emptyList()),
                rewards = emptyMap(),
                contentFilter = ContentFilterConfig(true, true, emptyList(), ""),
                storage = StorageConfig("sqlite", "stats.db"),
            )
        }
        return config
    }
}
