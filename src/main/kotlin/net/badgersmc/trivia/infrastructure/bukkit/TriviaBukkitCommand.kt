package net.badgersmc.trivia.infrastructure.bukkit

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.trivia.application.StatsService
import net.badgersmc.trivia.application.TriviaService
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
            "start" -> {
                if (!sender.hasPermission("lumatrivia.start")) {
                    sender.sendMessage(lang.msg("error.no_permission"))
                    return true
                }
                val started = triviaService.startGame()
                if (!started) {
                    val remaining = triviaService.cooldownRemaining()
                    if (remaining > 0) {
                        sender.sendMessage(lang.msg("game.cooldown", "time" to remaining.toString()))
                    }
                }
            }
            "stats" -> {
                if (!sender.hasPermission("lumatrivia.use")) {
                    sender.sendMessage(lang.msg("error.no_permission"))
                    return true
                }
                if (sender !is Player) {
                    sender.sendMessage(lang.msg("error.player_only"))
                    return true
                }
                val stats = statsService.getStats(sender.uniqueId)
                if (stats != null) {
                    sender.sendMessage(
                        lang.msg(
                            "commands.stats",
                            "player" to sender.name,
                            "total" to stats.totalCorrect.toString(),
                            "easy" to stats.easyCorrect.toString(),
                            "medium" to stats.mediumCorrect.toString(),
                            "hard" to stats.hardCorrect.toString(),
                            "points" to stats.points.toString(),
                        )
                    )
                } else {
                    sender.sendMessage(
                        lang.msg(
                            "commands.stats",
                            "player" to sender.name,
                            "total" to "0", "easy" to "0", "medium" to "0", "hard" to "0", "points" to "0",
                        )
                    )
                }
            }
            "top" -> {
                if (!sender.hasPermission("lumatrivia.use")) {
                    sender.sendMessage(lang.msg("error.no_permission"))
                    return true
                }
                val top = statsService.getTopPlayers(10)
                val entries = top.mapIndexed { i, ps ->
                    "&e${i + 1}. &f${ps.playerName} &7- &6${ps.points} points &7(&f${ps.totalCorrect} correct)"
                }.joinToString("\n")
                sender.sendMessage(lang.msg("commands.leaderboard", "leaderboard" to entries))
            }
            "reload" -> {
                if (!sender.hasPermission("lumatrivia.admin")) {
                    sender.sendMessage(lang.msg("error.no_permission"))
                    return true
                }
                sender.sendMessage(lang.msg("commands.reload"))
            }
            else -> sendUsage(sender)
        }
        return true
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
