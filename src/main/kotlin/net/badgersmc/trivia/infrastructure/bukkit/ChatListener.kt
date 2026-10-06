package net.badgersmc.trivia.infrastructure.bukkit

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.trivia.application.ChatPlatform
import net.badgersmc.trivia.application.TriviaService
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

/**
 * Single listener for chat suppression and answer parsing (REQ-019).
 * Uses Bukkit's AsyncPlayerChatEvent (not Paper's AsyncChatEvent) to avoid
 * Component-type mismatches and priority conflicts with RoseChat.
 *
 * During an active round, messages in the trivia answer channel are suppressed.
 * Valid answers are locked in once per player and correctness stays secret until time expires.
 */
class ChatListener(
    private val triviaService: TriviaService,
    private val lang: LangService,
    private val chatPlatform: ChatPlatform,
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onChat(event: AsyncPlayerChatEvent) {
        val player = event.player

        if (!triviaService.isActive) return

        val content = event.message.trim()
        if (content.isEmpty()) return

        if (!chatPlatform.isAnswerChat(player, content)) return
        val answer = chatPlatform.extractAnswer(content) ?: return

        if (chatPlatform.isMuted(player) && !player.hasPermission("lumatrivia.mute.bypass")) {
            event.isCancelled = true
            player.sendMessage(lang.msg("mute.muted"))
            return
        }

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

            if (!triviaService.tryClaimAnswer(player.uniqueId)) {
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
                    when (triviaService.checkAnswer(player, mapped)) {
                        TriviaService.AnswerResult.CORRECT,
                        TriviaService.AnswerResult.WRONG -> player.sendMessage(lang.msg("game.answer_locked"))

                        TriviaService.AnswerResult.ALREADY_ANSWERED ->
                            player.sendMessage(lang.msg("game.already_answered"))

                        TriviaService.AnswerResult.NO_GAME -> {}
                    }
                }
            )
        } else {
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
