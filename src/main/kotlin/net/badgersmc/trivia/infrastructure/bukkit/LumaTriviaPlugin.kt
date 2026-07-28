package net.badgersmc.trivia.infrastructure.bukkit

import net.badgersmc.trivia.infrastructure.di.ServiceModule
import org.bukkit.plugin.java.JavaPlugin

class LumaTriviaPlugin : JavaPlugin() {

    lateinit var services: ServiceModule
        private set

    override fun onEnable() {
        saveDefaultConfig()
        dataFolder.mkdirs()

        services = ServiceModule(this)

        // Register command
        server.commandMap.register(
            "lumatrivia",
            TriviaBukkitCommand(services.triviaService, services.statsService, services.lang),
        )

        // Register listener
        server.pluginManager.registerEvents(services.chatListener, this)

        // Prefetch questions on startup
        services.questionFetcher.fetchQuestions()

        logger.info("LumaTrivia enabled (v${description.version})")
    }

    override fun onDisable() {
        if (::services.isInitialized) {
            services.nexusScheduler.cancelAll()
            services.databaseFactory.close()
        }
        logger.info("LumaTrivia disabled")
    }
}
