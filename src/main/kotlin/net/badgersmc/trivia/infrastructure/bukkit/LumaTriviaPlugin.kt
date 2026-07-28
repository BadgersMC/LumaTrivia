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

        // Wire broadcast callback for game announcements (start, question, options, time_up)
        services.triviaService.broadcast = { component ->
            server.broadcast(component)
        }

        // Wire async fetch callback for cache recovery
        services.triviaService.fetchCallback = {
            server.scheduler.runTaskAsynchronously(this, Runnable {
                services.questionFetcher.fetchQuestions()
            })
        }

        // Start async question cache fill
        server.scheduler.runTaskAsynchronously(this, Runnable {
            services.questionFetcher.fetchQuestions()
        })

        // Register command
        triviaCommand = TriviaBukkitCommand(services.triviaService, services.statsService, services.lang, services)
        server.commandMap.register("lumatrivia", triviaCommand!!)

        // Register listener
        server.pluginManager.registerEvents(services.chatListener, this)

        // Set up scheduled games if configured
        setupScheduledGames()

        logger.info("LumaTrivia enabled (v${description.version})")
    }

    private fun setupScheduledGames() {
        val schedule = services.config.game.schedule
        if (!schedule.enabled || schedule.times.isEmpty()) return

        // Check every minute if it's time for a scheduled game
        server.scheduler.runTaskTimer(this, Runnable {
            val now = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            if (now in schedule.times) {
                services.triviaService.startGame()
            }
        }, 20 * 60, 20 * 60) // 1 minute initial delay, 1 minute repeat
        logger.info("Scheduled games enabled for times: ${schedule.times.joinToString()}")
    }

    override fun onDisable() {
        if (::services.isInitialized) {
            services.nexusScheduler.cancelAll()
            services.databaseFactory.close()
        }
        triviaCommand?.let { cmd ->
            server.commandMap.knownCommands.values.removeIf { it == cmd }
        }
        logger.info("LumaTrivia disabled")
    }
}
