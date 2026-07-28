package net.badgersmc.trivia.infrastructure.bukkit

import dev.rosewood.rosechat.message.RosePlayer
import net.badgersmc.trivia.application.ChatPlatform
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * RoseChat integration: uses RoseChat's mute and channel APIs for proper
 * per-channel behaviour.
 *
 * Compiled against RoseChat API (compileOnly), but safe to load without it —
 * ServiceModule only instantiates this when RoseChat is detected at runtime.
 */
class RoseChatPlatform(private val channelName: String) : ChatPlatform {

    /** Players muted by trivia — only these are cleared on round end. */
    private val triviaMuted: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    override fun isMuted(player: Player): Boolean {
        val data = RosePlayer(player).playerData ?: return false
        if (data.isMuteExpired) data.unmute()
        return data.isMuted
    }

    override fun mutePlayer(player: Player, durationSeconds: Int) {
        val data = RosePlayer(player).playerData ?: return
        // Never override an existing admin mute
        if (data.isMuted && !triviaMuted.contains(player.uniqueId)) return
        val expiry = System.currentTimeMillis() + (durationSeconds * 1000L)
        data.mute(expiry)
        triviaMuted.add(player.uniqueId)
    }

    override fun unmutePlayer(player: Player) {
        // Only unmute if WE muted them — don't strip admin mutes
        if (!triviaMuted.remove(player.uniqueId)) return
        RosePlayer(player).playerData?.unmute()
    }

    override fun clearMutes() {
        for (uuid in triviaMuted) {
            val player = Bukkit.getPlayer(uuid)
            if (player != null && player.isOnline) {
                RosePlayer(player).playerData?.unmute()
            }
        }
        triviaMuted.clear()
    }

    override fun isAnswerChat(player: Player, rawMessage: String): Boolean {
        val currentChannel = RosePlayer(player).playerData?.currentChannel ?: return false
        return currentChannel.id.equals(channelName, ignoreCase = true)
    }

    override fun extractAnswer(rawMessage: String): String? {
        return rawMessage.takeIf { it.isNotBlank() }
    }
}
