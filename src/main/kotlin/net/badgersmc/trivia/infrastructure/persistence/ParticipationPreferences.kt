package net.badgersmc.trivia.infrastructure.persistence

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID

/** Saved opt-outs; players participate by default for backwards compatibility. */
class ParticipationPreferences(private val file: File) {

    // Replaced atomically so async chat events never observe a partially updated set.
    @Volatile
    private var optedOut: Set<UUID> = YamlConfiguration.loadConfiguration(file)
        .getStringList("opted-out")
        .mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
        .toSet()

    fun isOptedIn(playerId: UUID): Boolean = playerId !in optedOut

    /** Called from the main thread. A failed save leaves the current preference unchanged. */
    @Synchronized
    fun setOptedIn(playerId: UUID, enabled: Boolean) {
        val updated = optedOut.toMutableSet()
        if (enabled) updated.remove(playerId) else updated.add(playerId)

        val yaml = YamlConfiguration()
        yaml.set("opted-out", updated.map(UUID::toString).sorted())
        file.parentFile?.mkdirs()
        yaml.save(file)
        optedOut = updated.toSet()
    }
}
