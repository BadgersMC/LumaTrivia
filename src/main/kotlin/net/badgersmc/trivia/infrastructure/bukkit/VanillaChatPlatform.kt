package net.badgersmc.trivia.infrastructure.bukkit

import net.badgersmc.trivia.application.ChatPlatform
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Vanilla Paper chat integration: uses a local mute map and cancels AsyncChatEvent.
 * Works on servers without RoseChat. Muting is global (all channels).
 */
class VanillaChatPlatform : ChatPlatform {

    private val mutedUntil: MutableMap<UUID, Long> = ConcurrentHashMap()

    override fun isMuted(player: Player): Boolean {
        val expiry = mutedUntil[player.uniqueId] ?: return false
        if (System.currentTimeMillis() >= expiry) {
            mutedUntil.remove(player.uniqueId)
            return false
        }
        return true
    }

    override fun mutePlayer(player: Player, durationSeconds: Int) {
        mutedUntil[player.uniqueId] = System.currentTimeMillis() + (durationSeconds * 1000L)
    }

    override fun unmutePlayer(player: Player) {
        mutedUntil.remove(player.uniqueId)
    }

    override fun isAnswerChat(player: Player, rawMessage: String): Boolean {
        // Vanilla: accept any chat as answer — no channel concept
        return true
    }

    override fun extractAnswer(rawMessage: String): String? {
        return rawMessage.takeIf { it.isNotBlank() }
    }
}
