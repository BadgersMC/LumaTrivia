package net.badgersmc.trivia.infrastructure.bukkit

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.trivia.application.ChatPlatform
import net.badgersmc.trivia.application.TriviaService
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

/**
 * Single listener for mute enforcement and answer parsing (REQ-019).
 * Uses Bukkit's AsyncPlayerChatEvent (not Paper's AsyncChatEvent) to avoid
 * Component-type mismatches and priority conflicts with RoseChat.
 *
 * On vanilla Paper: cancels AsyncPlayerChatEvent at LOWEST priority.
 * On RoseChat: mute only enforced for the trivia channel; other channels are untouched.
 */
class ChatListener(
    private val triviaService: TriviaService,
    private val lang: LangService,
    private val chatPlatform: ChatPlatform,
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onChat(event: AsyncPlayerChatEvent) {
        val player = event.player

        // Only active during a game
        if (!triviaService.isActive) return

        val content = event.message.trim()
        if (content.isEmpty()) return

        // Channel/platform check first — only trivia-channel messages proceed
        if (!chatPlatform.isAnswerChat(player, content)) return
        val answer = chatPlatform.extractAnswer(content) ?: return

        // Mute enforcement — only blocks trivia-channel answers
        if (chatPlatform.isMuted(player) && !player.hasPermission("lumatrivia.mute.bypass")) {
            event.isCancelled = true
            player.sendMessage(lang.msg("mute.muted"))
            return
        }

        // Valid answer formats: single letter or t/f/true/false
        val normalized = answer.trim().lowercase()
        val question = triviaService.currentQuestion ?: return
        val maxLetter = 'a' + (question.answerCount - 1)
        val isValidAnswer = when {
            normalized.length == 1 && normalized[0] in 'a'..maxLetter -> true
            normalized.matches(Regex("^(t(rue)?|f(alse)?)$")) -> true
            else -> false
        }

        if (isValidAnswer) {
            event.isCancelled = true

            // Early-out: block duplicate answers before scheduling
            if (triviaService.hasPlayerAnswered(player.uniqueId)) {
                player.sendMessage(lang.msg("game.already_answered"))
                return
            }

            val mapped = when {
                normalized.startsWith("t") -> "true"
                normalized.startsWith("f") -> "false"
                else -> normalized
            }
            player.server.scheduler.runTask(
                player.server.pluginManager.getPlugin("LumaTrivia")!!,
                Runnable {
                    if (!player.isOnline) return@Runnable
                    val result = triviaService.checkAnswer(player, mapped)
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
                            if (chatPlatform.isMuted(player)) {
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
        } else {
            // Invalid format — cancel the message and give a hint
            event.isCancelled = true
            val hint = if (question.answerCount == 2 && question.type == "boolean") {
                lang.msg("game.hint.truefalse")
            } else {
                val last = 'a' + (question.answerCount - 1)
                lang.msg("game.hint.letters", "first" to "a", "last" to last.toString())
            }
            player.sendMessage(hint)
        }
    }
}
