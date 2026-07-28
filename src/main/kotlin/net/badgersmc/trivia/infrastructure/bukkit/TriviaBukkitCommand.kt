package net.badgersmc.trivia.infrastructure.bukkit

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.trivia.application.StatsService
import net.badgersmc.trivia.application.TriviaService
import net.badgersmc.trivia.infrastructure.di.ServiceModule
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Bukkit command adapter for /trivia (REQ-013, REQ-014).
 */
class TriviaBukkitCommand(
    private val triviaService: TriviaService,
    private val statsService: StatsService,
    private val lang: LangService,
    private val services: ServiceModule,
) : Command("trivia") {

    init {
        description = "Main command for trivia games"
        usage = "/<command> [start|stats|top|reload]"
        setAliases(listOf("tr"))
    }

    override fun execute(sender: CommandSender, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sendUsage(sender)
            return true
        }

        when (args[0].lowercase()) {
            "start" -> handleStart(sender)
            "stats" -> handleStats(sender)
            "top" -> handleTop(sender)
            "reload" -> handleReload(sender)
            else -> sendUsage(sender)
        }
        return true
    }

    private fun handleStart(sender: CommandSender) {
        if (!sender.hasPermission("lumatrivia.start")) {
            sender.sendMessage(lang.msg("error.no_permission"))
            return
        }
        if (triviaService.isActive) {
            sender.sendMessage(lang.msg("game.already_active"))
            return
        }
        val remaining = triviaService.cooldownRemaining()
        if (remaining > 0) {
            sender.sendMessage(lang.msg("game.cooldown", "time" to remaining.toString()))
            return
        }
        val started = triviaService.startGame()
        if (!started) {
            sender.sendMessage(lang.msg("game.no_questions"))
        }
        // startGame() broadcasts game.start/question/options via the broadcast callback
    }

    private fun handleStats(sender: CommandSender) {
        if (!sender.hasPermission("lumatrivia.use")) {
            sender.sendMessage(lang.msg("error.no_permission"))
            return
        }
        if (sender !is Player) {
            sender.sendMessage(lang.msg("error.player_only"))
            return
        }
        val stats = statsService.getStats(sender.uniqueId)
        sender.sendMessage(
            lang.msg(
                "commands.stats",
                "player" to sender.name,
                "total" to (stats?.totalCorrect?.toString() ?: "0"),
                "easy" to (stats?.easyCorrect?.toString() ?: "0"),
                "medium" to (stats?.mediumCorrect?.toString() ?: "0"),
                "hard" to (stats?.hardCorrect?.toString() ?: "0"),
                "points" to (stats?.points?.toString() ?: "0"),
            )
        )
    }

    private fun handleTop(sender: CommandSender) {
        if (!sender.hasPermission("lumatrivia.use")) {
            sender.sendMessage(lang.msg("error.no_permission"))
            return
        }
        val top = statsService.getTopPlayers(10)
        val entries = if (top.isEmpty()) {
            "<gray>(no players yet)"
        } else {
            top.mapIndexed { i, ps ->
                "<yellow>${i + 1}. <white>${ps.playerName} <gray>- <gold>${ps.points} points <gray>(<white>${ps.totalCorrect} correct<gray>)"
            }.joinToString("\n")
        }
        sender.sendMessage(lang.msg("commands.leaderboard", "leaderboard" to entries))
    }

    private fun handleReload(sender: CommandSender) {
        if (!sender.hasPermission("lumatrivia.admin")) {
            sender.sendMessage(lang.msg("error.no_permission"))
            return
        }
        val newConfig = services.reloadConfig()
        triviaService.updateConfig(newConfig)
        // Rebuild fetcher reference in trivia service (fetcher was recreated in ServiceModule)
        triviaService.fetcher = services.questionFetcher
        // Rebuild schedule task from new config
        services.plugin.recreateScheduleTask()
        sender.sendMessage(lang.msg("commands.reload"))
    }

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        return listOfNotNull(
            "start".takeIf { sender.hasPermission("lumatrivia.start") },
            "stats".takeIf { sender.hasPermission("lumatrivia.use") },
            "top".takeIf { sender.hasPermission("lumatrivia.use") },
            "reload".takeIf { sender.hasPermission("lumatrivia.admin") },
        )
    }

    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage(lang.msg("commands.usage_header"))
        if (sender.hasPermission("lumatrivia.start"))
            sender.sendMessage(lang.msg("commands.usage_start"))
        if (sender.hasPermission("lumatrivia.use")) {
            sender.sendMessage(lang.msg("commands.usage_stats"))
            sender.sendMessage(lang.msg("commands.usage_top"))
        }
        if (sender.hasPermission("lumatrivia.admin"))
            sender.sendMessage(lang.msg("commands.usage_reload"))
    }
}
