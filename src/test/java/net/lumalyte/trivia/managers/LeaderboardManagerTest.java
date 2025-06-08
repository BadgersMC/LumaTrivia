package net.lumalyte.trivia.managers;

import net.lumalyte.trivia.TriviaPlugin;
import net.lumalyte.trivia.models.PlayerStats;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LeaderboardManagerTest {
    @TempDir
    Path tempDir;

    @Mock
    private TriviaPlugin plugin;

    @Mock
    private Player player1;

    @Mock
    private Player player2;

    private LeaderboardManager leaderboardManager;
    private File statsFile;
    private UUID player1Id;
    private UUID player2Id;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Set up temp directory for stats file
        statsFile = new File(tempDir.toFile(), "stats.yml");
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());

        // Mock config
        YamlConfiguration config = new YamlConfiguration();
        config.set("performance.async-saving", false);
        when(plugin.getConfig()).thenReturn(config);

        // Mock player data
        player1Id = UUID.randomUUID();
        player2Id = UUID.randomUUID();
        when(player1.getUniqueId()).thenReturn(player1Id);
        when(player2.getUniqueId()).thenReturn(player2Id);
        when(player1.getName()).thenReturn("Player1");
        when(player2.getName()).thenReturn("Player2");

        // Create manager instance
        leaderboardManager = new LeaderboardManager(plugin);
    }

    @Test
    void testAddCorrectAnswer() {
        // Add some correct answers
        leaderboardManager.addCorrectAnswer(player1, "easy");
        leaderboardManager.addCorrectAnswer(player1, "medium");
        leaderboardManager.addCorrectAnswer(player2, "hard");

        // Check player1's stats
        PlayerStats stats1 = leaderboardManager.getPlayerStats(player1.getUniqueId());
        assertNotNull(stats1);
        assertEquals(2, stats1.getTotalCorrect());
        assertEquals(1, stats1.getEasyCorrect());
        assertEquals(1, stats1.getMediumCorrect());
        assertEquals(0, stats1.getHardCorrect());
        assertEquals(3, stats1.getPoints()); // 1 + 2 points

        // Check player2's stats
        PlayerStats stats2 = leaderboardManager.getPlayerStats(player2.getUniqueId());
        assertNotNull(stats2);
        assertEquals(1, stats2.getTotalCorrect());
        assertEquals(0, stats2.getEasyCorrect());
        assertEquals(0, stats2.getMediumCorrect());
        assertEquals(1, stats2.getHardCorrect());
        assertEquals(3, stats2.getPoints()); // 3 points
    }

    @Test
    void testGetTopPlayers() {
        // Add some scores
        leaderboardManager.addCorrectAnswer(player1, "hard"); // 3 points
        leaderboardManager.addCorrectAnswer(player2, "hard"); // 3 points
        leaderboardManager.addCorrectAnswer(player2, "medium"); // +2 points

        List<PlayerStats> top = leaderboardManager.getTopPlayers(10);
        assertEquals(2, top.size());
        
        // Player2 should be first (5 points)
        assertEquals(player2.getName(), top.get(0).getPlayerName());
        assertEquals(5, top.get(0).getPoints());
        
        // Player1 should be second (3 points)
        assertEquals(player1.getName(), top.get(1).getPlayerName());
        assertEquals(3, top.get(1).getPoints());
    }

    @Test
    void testFormatLeaderboard() {
        // Add some scores
        leaderboardManager.addCorrectAnswer(player1, "hard");
        leaderboardManager.addCorrectAnswer(player2, "medium");
        leaderboardManager.addCorrectAnswer(player2, "easy");

        String leaderboard = leaderboardManager.formatLeaderboard(10);
        
        // Check formatting
        assertTrue(leaderboard.contains("1."));
        assertTrue(leaderboard.contains("2."));
        assertTrue(leaderboard.contains(player1.getName()));
        assertTrue(leaderboard.contains(player2.getName()));
        assertTrue(leaderboard.contains("3 points")); // Player1's points
        assertTrue(leaderboard.contains("3 points")); // Player2's points (1 + 2)
    }

    @Test
    void testPersistence() throws Exception {
        // Skip persistence test for now as it requires more complex mocking
        // TODO: Implement proper persistence testing with file system mocking
    }
} 