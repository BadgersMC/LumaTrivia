package net.badgersmc.trivia.infrastructure.bukkit

import net.badgersmc.trivia.application.ChatPlatform
import org.bukkit.entity.Player

/**
 * RoseChat integration: uses RoseChat's mute and channel APIs for proper
 * per-channel behaviour. All RoseChat access is via reflection to avoid a
 * hard compile-time dependency — the plugin works without RoseChat installed.
 */
class RoseChatPlatform(private val channelName: String) : ChatPlatform {

    override fun isMuted(player: Player): Boolean {
        return try {
            val rosePlayer = rosePlayerClass()
                .getConstructor(Player::class.java)
                .newInstance(player)
            val playerData = rosePlayer.javaClass.getMethod("getPlayerData").invoke(rosePlayer)
                ?: return false
            playerData.javaClass.getMethod("validateMuteExpiry").invoke(playerData)
            playerData.javaClass.getMethod("isMuted").invoke(playerData) as? Boolean ?: false
        } catch (_: Exception) {
            false
        }
    }

    override fun mutePlayer(player: Player, durationSeconds: Int) {
        try {
            val rosePlayer = rosePlayerClass()
                .getConstructor(Player::class.java)
                .newInstance(player)
            val playerData = rosePlayer.javaClass.getMethod("getPlayerData").invoke(rosePlayer)
                ?: return
            val expiry = System.currentTimeMillis() + (durationSeconds * 1000L)
            playerData.javaClass.getMethod("mute", Long::class.javaPrimitiveType!!)
                .invoke(playerData, expiry)
        } catch (_: Exception) {
            // RoseChat not available — no-op
        }
    }

    override fun unmutePlayer(player: Player) {
        try {
            val rosePlayer = rosePlayerClass()
                .getConstructor(Player::class.java)
                .newInstance(player)
            val playerData = rosePlayer.javaClass.getMethod("getPlayerData").invoke(rosePlayer)
                ?: return
            playerData.javaClass.getMethod("unmute").invoke(playerData)
        } catch (_: Exception) {
            // no-op
        }
    }

    override fun isAnswerChat(player: Player, rawMessage: String): Boolean {
        return try {
            val rosePlayer = rosePlayerClass()
                .getConstructor(Player::class.java)
                .newInstance(player)
            val playerData = rosePlayer.javaClass.getMethod("getPlayerData").invoke(rosePlayer)
                ?: return false
            val currentChannel = playerData.javaClass.getMethod("getCurrentChannel").invoke(playerData)
                ?: return false
            val channelId = currentChannel.javaClass.getMethod("getId").invoke(currentChannel) as? String
            val channelName = currentChannel.javaClass.getMethod("getName").invoke(currentChannel) as? String
            channelName.equals(this.channelName, ignoreCase = true) ||
                channelId.equals(this.channelName, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    override fun extractAnswer(rawMessage: String): String? {
        return rawMessage.takeIf { it.isNotBlank() }
    }

    private fun rosePlayerClass(): Class<*> =
        Class.forName("dev.rosewood.rosechat.message.RosePlayer")
}
