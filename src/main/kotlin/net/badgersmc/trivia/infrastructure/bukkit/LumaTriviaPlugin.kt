package net.badgersmc.trivia.infrastructure.bukkit

import net.badgersmc.trivia.infrastructure.di.ServiceModule
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class LumaTriviaPlugin : JavaPlugin() {

    lateinit var services: ServiceModule
        private set

    private var triviaCommand: TriviaBukkitCommand? = null
    private var scheduleTaskId: Int = -1

 override fun onEnable() {
    logger.info("RoseChat plugin = ${server.pluginManager.getPlugin("RoseChat")}")

    try {
        val clazz = Class.forName("dev.rosewood.rosechat.message.RosePlayer")
        logger.info("Successfully loaded ${clazz.name}")
    } catch (t: Throwable) {
        logger.log(Level.SEVERE, "Failed to load RosePlayer", t)
    }
        saveDefaultConfig()
        dataFolder.mkdirs()

        services = ServiceModule(this)

        // Wire broadcast callback for game announcements
        services.triviaService.broadcast = { component ->
            server.broadcast(component)
        }

        // Wire async fetch callback — fetch with bounded retry on empty cache
        services.triviaService.fetchCallback = {
            server.scheduler.runTaskAsynchronously(this, Runnable {
                fetchWithRetry(0)
            })
        }

        // Start initial cache fill through guarded path
        services.triviaService.startPrewarm()

        // Register command
        triviaCommand = TriviaBukkitCommand(services.triviaService, services.statsService, services.lang, services)
        server.commandMap.register("lumatrivia", triviaCommand!!)

        // Register listener
        server.pluginManager.registerEvents(services.chatListener, this)

        // Set up scheduled games if configured
        recreateScheduleTask()

        logger.info("LumaTrivia enabled (v${description.version})")
    }

    /** Fetch questions with up to [MAX_RETRIES] attempts and linear backoff on empty cache. */
    private fun fetchWithRetry(attempt: Int) {
        val maxRetries = 3
        val baseDelay = 5L // seconds

        services.questionFetcher.fetchQuestions()

        if (!services.questionFetcher.isEmpty) {
            // Got questions — schedule game start on main thread
            server.scheduler.runTask(this, Runnable {
                services.triviaService.onFetchDone()
                if (!services.triviaService.isActive) {
                    services.triviaService.startGame()
                }
            })
            return
        }

        // Cache still empty — retry with backoff if not exhausted
        if (attempt < maxRetries) {
            val delay = baseDelay * (attempt + 1) // 5, 10, 15 seconds
            logger.info("Fetch attempt ${attempt + 1} returned no usable questions. Retrying in ${delay}s...")
            server.scheduler.runTaskLaterAsynchronously(this, Runnable {
                fetchWithRetry(attempt + 1)
            }, delay * 20L)
        } else {
            // Exhausted retries
            server.scheduler.runTask(this, Runnable {
                services.triviaService.onFetchDone()
            })
            logger.warning("Failed to fetch questions after ${attempt + 1} attempts")
        }
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
