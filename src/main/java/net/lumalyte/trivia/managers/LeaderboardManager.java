package net.lumalyte.trivia.managers;

import net.lumalyte.trivia.TriviaPlugin;
import net.lumalyte.trivia.models.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LeaderboardManager {
    private final TriviaPlugin plugin;
    private final Map<UUID, PlayerStats> stats;
    private final File statsFile;
    private final YamlConfiguration statsConfig;
    private boolean saving = false;

    public LeaderboardManager(TriviaPlugin plugin) {
        this.plugin = plugin;
        this.stats = new ConcurrentHashMap<>();
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        this.statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        loadStats();
    }

    public void addCorrectAnswer(Player player, String difficulty) {
        UUID playerId = player.getUniqueId();
        PlayerStats playerStats = stats.computeIfAbsent(playerId,
            id -> new PlayerStats(id, player.getName()));
        playerStats.addCorrectAnswer(difficulty);
        
        if (plugin.getConfig().getBoolean("performance.async-saving", true)) {
            saveStatsAsync();
        } else {
            saveStats();
        }
    }

    public PlayerStats getPlayerStats(UUID playerId) {
        return stats.get(playerId);
    }

    public List<PlayerStats> getTopPlayers(int limit) {
        return stats.values().stream()
            .sorted()
            .limit(limit)
            .collect(Collectors.toList());
    }

    public String formatLeaderboard(int limit) {
        List<PlayerStats> top = getTopPlayers(limit);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < top.size(); i++) {
            PlayerStats ps = top.get(i);
            sb.append(String.format("&e%d. &f%s &7- &6%d points &7(&f%d correct)\n",
                i + 1, ps.getPlayerName(), ps.getPoints(), ps.getTotalCorrect()));
        }
        return sb.toString();
    }

    private void loadStats() {
        if (!statsFile.exists()) {
            return;
        }

        for (String uuidStr : statsConfig.getKeys(false)) {
            UUID playerId = UUID.fromString(uuidStr);
            String path = uuidStr + ".";
            String playerName = statsConfig.getString(path + "name");
            PlayerStats ps = new PlayerStats(playerId, playerName);

            // Load all stats
            for (int i = 0; i < statsConfig.getInt(path + "total"); i++) {
                ps.addCorrectAnswer(statsConfig.getString(path + "difficulties." + i));
            }

            stats.put(playerId, ps);
        }
    }

    private void saveStatsAsync() {
        if (saving) return;
        saving = true;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                saveStats();
            } finally {
                saving = false;
            }
        });
    }

    private synchronized void saveStats() {
        for (PlayerStats ps : stats.values()) {
            String path = ps.getPlayerId().toString() + ".";
            statsConfig.set(path + "name", ps.getPlayerName());
            statsConfig.set(path + "total", ps.getTotalCorrect());
            statsConfig.set(path + "easy", ps.getEasyCorrect());
            statsConfig.set(path + "medium", ps.getMediumCorrect());
            statsConfig.set(path + "hard", ps.getHardCorrect());
            statsConfig.set(path + "points", ps.getPoints());
        }

        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save stats: " + e.getMessage());
        }
    }
} 