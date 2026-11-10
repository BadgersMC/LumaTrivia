package net.badgersmc.trivia.infrastructure.persistence

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParticipationPreferencesTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `players are opted in by default and opt outs persist across reloads`() {
        val file = File(tempDir.toFile(), "preferences.yml")
        val player = UUID.randomUUID()
        val anotherPlayer = UUID.randomUUID()
        val preferences = ParticipationPreferences(file)

        assertTrue(preferences.isOptedIn(player))
        assertTrue(preferences.isOptedIn(anotherPlayer))

        preferences.setOptedIn(player, false)
        assertFalse(preferences.isOptedIn(player))
        assertTrue(preferences.isOptedIn(anotherPlayer))
        assertFalse(ParticipationPreferences(file).isOptedIn(player))

        preferences.setOptedIn(player, true)
        assertTrue(ParticipationPreferences(file).isOptedIn(player))
    }
}
