package net.badgersmc.trivia.application

import org.bukkit.entity.Player

/**
 * Abstracts chat platform integration so the trivia service can work with
 * either vanilla Paper chat or RoseChat.
 *
 * Implementations control:
 * - Per-player muting (suppress chat during active trivia round)
 * - Channel validation (only accept answers from the configured channel)
 * - Message routing (platform may cancel or redirect the chat event)
 */
interface ChatPlatform {

    /** Whether [player] is currently muted. */
    fun isMuted(player: Player): Boolean

    /**
     * Mute [player] for [durationSeconds]. The platform decides how to enforce it
     * (raw event cancellation, channel-level mute, etc.).
     */
    fun mutePlayer(player: Player, durationSeconds: Int)

    /** Unmute [player] immediately. */
    fun unmutePlayer(player: Player)

    /** Whether the player's chat should be processed as a trivia answer. */
    fun isAnswerChat(player: Player, rawMessage: String): Boolean

    /** Extract the answer string from the raw chat message. */
    fun extractAnswer(rawMessage: String): String?
}
