package net.badgersmc.trivia.infrastructure.bukkit

import dev.rosewood.rosechat.message.RosePlayer
import net.badgersmc.trivia.application.ChatPlatform
import org.bukkit.entity.Player

/**
 * RoseChat integration: uses RoseChat's mute and channel APIs for proper
 * per-channel behaviour.
 *
 * Compiled against RoseChat API (compileOnly), but safe to load without it —
 * ServiceModule only instantiates this when RoseChat is detected at runtime.
 */
class RoseChatPlatform(private val channelName: String) : ChatPlatform {

    override fun isMuted(player: Player): Boolean {
        val data = RosePlayer(player).playerData ?: return false
        if (data.isMuteExpired) data.unmute()
        return data.isMuted
    }

    override fun mutePlayer(player: Player, durationSeconds: Int) {
        val data = RosePlayer(player).playerData ?: return
        val expiry = System.currentTimeMillis() + (durationSeconds * 1000L)
        data.mute(expiry)
    }

    override fun unmutePlayer(player: Player) {
        RosePlayer(player).playerData?.unmute()
    }

    override fun isAnswerChat(player: Player, rawMessage: String): Boolean {
        val currentChannel = RosePlayer(player).playerData?.currentChannel ?: return false
        return currentChannel.id.equals(channelName, ignoreCase = true)
    }

    override fun extractAnswer(rawMessage: String): String? {
        return rawMessage.takeIf { it.isNotBlank() }
    }

    override fun clearMutes() {
        // RoseChat mutes are per-player with expiry — they expire naturally.
        // No bulk clear needed; individual unmutes happen on demand.
    }
}
