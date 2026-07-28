package net.badgersmc.trivia.infrastructure.bukkit

import io.papermc.paper.event.player.AsyncChatEvent
import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.trivia.application.TriviaService
import net.kyori.adventure.text.TextComponent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

/**
 * Single listener for mute enforcement and answer parsing (REQ-019).
 * Handles everything at LOWEST priority to block muted chat before any other plugin sees it.
 */
class ChatListener(
    private val triviaService: TriviaService,
    private val lang: LangService,
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        val player = event.player

        // Mute enforcement — check bypass permission
        if (triviaService.isPlayerMuted(player.uniqueId) && !player.hasPermission("lumatrivia.mute.bypass")) {
            event.isCancelled = true
            player.sendMessage(lang.msg("mute.muted"))
            return
        }

        // Answer parsing — only when game is active
        if (!triviaService.isActive) return

        val content = (event.message() as? TextComponent)?.content() ?: return
        val normalized = content.trim().lowercase()

        // Valid answer formats: single letter bound to actual option count, or t/f/true/false
        val question = triviaService.currentQuestion ?: return
        val maxLetter = 'a' + (question.answerCount - 1)
        val isValidAnswer = when {
            normalized.length == 1 && normalized[0] in 'a'..maxLetter -> true
            normalized.matches(Regex("^(t(rue)?|f(alse)?)$")) -> true
            else -> false
        }

        if (isValidAnswer) {
            event.isCancelled = true
            val answer = when {
                normalized.startsWith("t") -> "true"
                normalized.startsWith("f") -> "false"
                else -> normalized
            }
            // Process on main thread
            player.server.scheduler.runTask(
                player.server.pluginManager.getPlugin("LumaTrivia")!!,
                Runnable {
                    if (!player.isOnline) return@Runnable
                    val result = triviaService.checkAnswer(player, answer)
                    when (result) {
                        TriviaService.AnswerResult.CORRECT -> {
                            val q = triviaService.currentQuestion ?: return@Runnable
                            player.server.broadcast(
                                lang.msg(
                                    "game.correct_answer",
                                    "player" to player.name,
                                    "answer" to q.correctAnswer,
                                    "letter" to q.correctAnswerLetter,
                                )
                            )
                        }
                        TriviaService.AnswerResult.WRONG -> {
                            player.server.broadcast(
                                lang.msg(
                                    "game.wrong_answer",
                                    "player" to player.name,
                                    "answer" to content,
                                )
                            )
                            if (triviaService.isPlayerMuted(player.uniqueId)) {
                                player.sendMessage(lang.msg("mute.muted"))
                            }
                        }
                        TriviaService.AnswerResult.ALREADY_ANSWERED -> {
                            player.sendMessage(lang.msg("game.already_answered"))
                        }
                        TriviaService.AnswerResult.NO_GAME -> {}
                    }
                }
            )
        }
    }
}
