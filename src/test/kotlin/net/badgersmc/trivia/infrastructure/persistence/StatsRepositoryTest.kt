package net.badgersmc.trivia.infrastructure.persistence

import net.badgersmc.trivia.domain.PlayerStats
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StatsRepositoryTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var dbFactory: DatabaseFactory
    private lateinit var repo: StatsRepository

    @BeforeEach
    fun setUp() {
        dbFactory = DatabaseFactory(tempDir.toFile(), "test.db")
        repo = SqliteStatsRepository(dbFactory)
    }

    @AfterEach
    fun tearDown() {
        dbFactory.close()
    }

    @Test
    fun `save and load stats round trip`() {
        val playerId = UUID.randomUUID()
        val stats = PlayerStats(playerId, "Alice")
        stats.addCorrectAnswer("easy")
        stats.addCorrectAnswer("medium")

        repo.save(stats)

        val loaded = repo.findByPlayerId(playerId)
        assertNotNull(loaded)
        assertEquals("Alice", loaded.playerName)
        assertEquals(2, loaded.totalCorrect)
        assertEquals(1, loaded.easyCorrect)
        assertEquals(1, loaded.mediumCorrect)
        assertEquals(0, loaded.hardCorrect)
        assertEquals(3, loaded.points) // 1 + 2
    }

    @Test
    fun `save updates existing stats`() {
        val playerId = UUID.randomUUID()
        val stats = PlayerStats(playerId, "Bob")
        stats.addCorrectAnswer("easy")
        repo.save(stats)

        stats.addCorrectAnswer("hard")
        repo.save(stats)

        val loaded = repo.findByPlayerId(playerId)
        assertNotNull(loaded)
        assertEquals(2, loaded.totalCorrect)
        assertEquals(1, loaded.easyCorrect)
        assertEquals(1, loaded.hardCorrect)
        assertEquals(4, loaded.points) // 1 + 3
    }

    @Test
    fun `findByPlayerId returns null for unknown player`() {
        assertNull(repo.findByPlayerId(UUID.randomUUID()))
    }

    @Test
    fun `getTopPlayers returns sorted by points descending`() {
        val alice = PlayerStats(UUID.randomUUID(), "Alice")
        alice.addCorrectAnswer("easy") // 1 pt

        val bob = PlayerStats(UUID.randomUUID(), "Bob")
        bob.addCorrectAnswer("hard") // 3 pts

        val charlie = PlayerStats(UUID.randomUUID(), "Charlie")
        charlie.addCorrectAnswer("medium") // 2 pts

        repo.save(alice)
        repo.save(bob)
        repo.save(charlie)

        val top = repo.getTopPlayers(10)
        assertEquals(3, top.size)
        assertEquals("Bob", top[0].playerName) // 3 pts
        assertEquals("Charlie", top[1].playerName) // 2 pts
        assertEquals("Alice", top[2].playerName) // 1 pt
    }

    @Test
    fun `getTopPlayers respects limit`() {
        val a = PlayerStats(UUID.randomUUID(), "A")
        a.addCorrectAnswer("easy")
        val b = PlayerStats(UUID.randomUUID(), "B")
        b.addCorrectAnswer("medium")
        val c = PlayerStats(UUID.randomUUID(), "C")
        c.addCorrectAnswer("hard")

        repo.save(a); repo.save(b); repo.save(c)

        val top = repo.getTopPlayers(2)
        assertEquals(2, top.size)
        assertEquals("C", top[0].playerName) // 3 pts
        assertEquals("B", top[1].playerName) // 2 pts
    }

    @Test
    fun `playerName is updated on subsequent saves`() {
        val playerId = UUID.randomUUID()
        val stats = PlayerStats(playerId, "OldName")
        stats.addCorrectAnswer("easy")
        repo.save(stats)

        // Save again with new name
        val updated = PlayerStats(playerId, "NewName")
        updated.addCorrectAnswer("hard")
        repo.save(updated)

        val loaded = repo.findByPlayerId(playerId)
        assertNotNull(loaded)
        assertEquals("NewName", loaded.playerName)
    }
}
