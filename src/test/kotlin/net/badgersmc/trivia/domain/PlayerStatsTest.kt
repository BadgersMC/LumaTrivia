package net.badgersmc.trivia.domain

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerStatsTest {
    private val playerId = UUID.randomUUID()
    private val playerName = "TestPlayer"

    @Test
    fun `initial values are zero`() {
        val stats = PlayerStats(playerId, playerName)
        assertEquals(0, stats.totalCorrect)
        assertEquals(0, stats.easyCorrect)
        assertEquals(0, stats.mediumCorrect)
        assertEquals(0, stats.hardCorrect)
        assertEquals(0, stats.points)
        assertEquals(playerId, stats.playerId)
        assertEquals(playerName, stats.playerName)
    }

    @Test
    fun `addCorrectAnswer easy adds 1 point`() {
        val stats = PlayerStats(playerId, playerName)
        stats.addCorrectAnswer("easy")
        assertEquals(1, stats.totalCorrect)
        assertEquals(1, stats.easyCorrect)
        assertEquals(0, stats.mediumCorrect)
        assertEquals(0, stats.hardCorrect)
        assertEquals(1, stats.points)
    }

    @Test
    fun `addCorrectAnswer medium adds 2 points`() {
        val stats = PlayerStats(playerId, playerName)
        stats.addCorrectAnswer("medium")
        assertEquals(1, stats.totalCorrect)
        assertEquals(0, stats.easyCorrect)
        assertEquals(1, stats.mediumCorrect)
        assertEquals(0, stats.hardCorrect)
        assertEquals(2, stats.points)
    }

    @Test
    fun `addCorrectAnswer hard adds 3 points`() {
        val stats = PlayerStats(playerId, playerName)
        stats.addCorrectAnswer("hard")
        assertEquals(1, stats.totalCorrect)
        assertEquals(3, stats.points)
    }

    @Test
    fun `multiple answers accumulate correctly`() {
        val stats = PlayerStats(playerId, playerName)
        stats.addCorrectAnswer("easy")
        stats.addCorrectAnswer("medium")
        stats.addCorrectAnswer("hard")
        assertEquals(3, stats.totalCorrect)
        assertEquals(1, stats.easyCorrect)
        assertEquals(1, stats.mediumCorrect)
        assertEquals(1, stats.hardCorrect)
        assertEquals(6, stats.points) // 1 + 2 + 3
    }

    @Test
    fun `compareTo sorts by points descending`() {
        val a = PlayerStats(UUID.randomUUID(), "A")
        val b = PlayerStats(UUID.randomUUID(), "B")

        // Equal points → tie
        assertEquals(0, a.compareTo(b))

        // A has more points → A < B (descending)
        a.addCorrectAnswer("hard") // 3 points
        assertTrue(a.compareTo(b) < 0)

        // B has more points → A > B (descending)
        b.addCorrectAnswer("hard") // 3
        b.addCorrectAnswer("medium") // +2 = 5
        assertTrue(a.compareTo(b) > 0)
    }

    @Test
    fun `playerName is preserved`() {
        val stats = PlayerStats(playerId, "Alice")
        assertEquals("Alice", stats.playerName)
        stats.addCorrectAnswer("easy")
        assertEquals("Alice", stats.playerName)
    }
}
