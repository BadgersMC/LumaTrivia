package net.badgersmc.trivia.infrastructure.bukkit

import net.badgersmc.trivia.infrastructure.di.ServiceModule
import org.bukkit.plugin.java.JavaPlugin

class LumaTriviaPlugin : JavaPlugin() {

    lateinit var services: ServiceModule
        private set

    private var triviaCommand: TriviaBukkitCommand? = null
    private var scheduleTaskId: Int = -1

    override fun onEnable() {
        saveDefaultConfig()
        dataFolder.mkdirs()

        services = ServiceModule(this)

        // Wire broadcast callback for game announcements
        services.triviaService.broadcast = { component ->
            server.broadcast(component)
        }

        // Wire async fetch callback — fetch questions then try auto-start
        services.triviaService.fetchCallback = {
            server.scheduler.runTaskAsynchronously(this, Runnable {
                services.questionFetcher.fetchQuestions()
                server.scheduler.runTask(this, Runnable {
                    services.triviaService.onFetchDone()
                    if (!services.questionFetcher.isEmpty && !services.triviaService.isActive) {
                        services.triviaService.startGame()
                    }
                })
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
        recreateScheduleTask()

        logger.info("LumaTrivia enabled (v${description.version})")
    }

    /** Cancel and recreate the schedule repeating task from current config. */
    fun recreateScheduleTask() {
        if (scheduleTaskId != -1) {
            server.scheduler.cancelTask(scheduleTaskId)
            scheduleTaskId = -1
        }

        val schedule = services.config.game.schedule
        if (!schedule.enabled || schedule.times.isEmpty()) {
            logger.info("Scheduled games disabled")
            return
        }

        scheduleTaskId = server.scheduler.runTaskTimer(this, Runnable {
            val now = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            if (now in schedule.times) {
                services.triviaService.startGame()
            }
        }, 20 * 60, 20 * 60).taskId
        logger.info("Scheduled games enabled for times: ${schedule.times.joinToString()}")
    }

    override fun onDisable() {
        if (::services.isInitialized) {
            services.nexusScheduler.cancelAll()
            services.databaseFactory.close()
        }
        if (scheduleTaskId != -1) {
            server.scheduler.cancelTask(scheduleTaskId)
        }
        triviaCommand?.let { cmd ->
            server.commandMap.knownCommands.values.removeIf { it == cmd }
        }
        logger.info("LumaTrivia disabled")
    }
}
