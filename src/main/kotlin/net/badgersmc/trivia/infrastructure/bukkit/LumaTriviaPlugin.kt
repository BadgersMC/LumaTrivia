package net.badgersmc.trivia.infrastructure.bukkit

import net.badgersmc.trivia.infrastructure.di.ServiceModule
import org.bukkit.plugin.java.JavaPlugin

class LumaTriviaPlugin : JavaPlugin() {

    lateinit var services: ServiceModule
        private set

    private var triviaCommand: TriviaBukkitCommand? = null

    override fun onEnable() {
        saveDefaultConfig()
        dataFolder.mkdirs()

        services = ServiceModule(this)

        // Register command (stored for unregister on disable)
        triviaCommand = TriviaBukkitCommand(services.triviaService, services.statsService, services.lang)
        server.commandMap.register("lumatrivia", triviaCommand!!)

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
        // Unregister command from command map
        triviaCommand?.let { cmd ->
            server.commandMap.knownCommands.values.removeIf { it == cmd }
        }
        logger.info("LumaTrivia disabled")
    }
}
