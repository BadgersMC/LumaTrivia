package net.lumalyte.trivia.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

public class PlayerStatsTest {
    private PlayerStats stats;
    private final UUID playerId = UUID.randomUUID();
    private final String playerName = "TestPlayer";

    @BeforeEach
    void setUp() {
        stats = new PlayerStats(playerId, playerName);
    }

    @Test
    void testInitialValues() {
        assertEquals(0, stats.getTotalCorrect());
        assertEquals(0, stats.getEasyCorrect());
        assertEquals(0, stats.getMediumCorrect());
        assertEquals(0, stats.getHardCorrect());
        assertEquals(0, stats.getPoints());
        assertEquals(playerId, stats.getPlayerId());
        assertEquals(playerName, stats.getPlayerName());
    }

    @Test
    void testAddCorrectAnswer() {
        // Test easy question
        stats.addCorrectAnswer("easy");
        assertEquals(1, stats.getTotalCorrect());
        assertEquals(1, stats.getEasyCorrect());
        assertEquals(0, stats.getMediumCorrect());
        assertEquals(0, stats.getHardCorrect());
        assertEquals(1, stats.getPoints());

        // Test medium question
        stats.addCorrectAnswer("medium");
        assertEquals(2, stats.getTotalCorrect());
        assertEquals(1, stats.getEasyCorrect());
        assertEquals(1, stats.getMediumCorrect());
        assertEquals(0, stats.getHardCorrect());
        assertEquals(3, stats.getPoints()); // 1 + 2 points

        // Test hard question
        stats.addCorrectAnswer("hard");
        assertEquals(3, stats.getTotalCorrect());
        assertEquals(1, stats.getEasyCorrect());
        assertEquals(1, stats.getMediumCorrect());
        assertEquals(1, stats.getHardCorrect());
        assertEquals(6, stats.getPoints()); // 1 + 2 + 3 points
    }

    @Test
    void testCompareTo() {
        PlayerStats otherStats = new PlayerStats(UUID.randomUUID(), "OtherPlayer");
        
        // Both have 0 points
        assertEquals(0, stats.compareTo(otherStats));
        
        // This player has more points
        stats.addCorrectAnswer("hard"); // 3 points
        assertTrue(stats.compareTo(otherStats) < 0); // Should be "less than" for descending order
        
        // Other player has more points
        otherStats.addCorrectAnswer("hard"); // 3 points
        otherStats.addCorrectAnswer("medium"); // +2 points
        assertTrue(stats.compareTo(otherStats) > 0); // Should be "greater than" for descending order
    }
} 