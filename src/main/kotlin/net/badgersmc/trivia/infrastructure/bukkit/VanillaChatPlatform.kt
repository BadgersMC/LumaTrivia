package net.badgersmc.trivia.infrastructure.bukkit

import net.badgersmc.trivia.application.ChatPlatform
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Vanilla Paper chat integration: uses a local mute map and cancels AsyncChatEvent.
 * Works on servers without RoseChat. Muting is global (all channels).
 *
 * A periodic cleanup task prunes stale entries left behind by disconnected players.
 */
class VanillaChatPlatform(private val plugin: JavaPlugin) : ChatPlatform {

    private val mutedUntil: MutableMap<UUID, Long> = ConcurrentHashMap()

    init {
        // Periodic cleanup of stale entries (runs every 30s)
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable { pruneStale() }, 20 * 30, 20 * 30)
    }

    /** No-arg constructor for tests that don't have a plugin. */
    constructor() : this(Bukkit.getPluginManager().getPlugin("LumaTrivia") as JavaPlugin)

    override fun isMuted(player: Player): Boolean {
        val expiry = mutedUntil[player.uniqueId] ?: return false
        if (System.currentTimeMillis() >= expiry) {
            // Race-safe: only remove if the value still matches the observed expiry
            mutedUntil.remove(player.uniqueId, expiry)
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

    override fun clearMutes() {
        mutedUntil.clear()
    }

    override fun isAnswerChat(player: Player, rawMessage: String): Boolean {
        return true
    }

    override fun extractAnswer(rawMessage: String): String? {
        return rawMessage.takeIf { it.isNotBlank() }
    }

    private fun pruneStale() {
        val now = System.currentTimeMillis()
        mutedUntil.entries.removeIf { it.value < now }
    }
}
