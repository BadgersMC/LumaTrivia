package net.lumalyte.trivia.managers;

import net.lumalyte.trivia.TriviaPlugin;
import net.lumalyte.trivia.models.PlayerData;
import net.lumalyte.trivia.tasks.MuteTask;
import net.lumalyte.trivia.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager implements Listener {
    private final TriviaPlugin plugin;
    private final Map<UUID, PlayerData> playerData;
    private final Map<UUID, MuteTask> muteTasks;

    public PlayerManager(TriviaPlugin plugin) {
        this.plugin = plugin;
        this.playerData = new ConcurrentHashMap<>();
        this.muteTasks = new ConcurrentHashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public PlayerData getPlayerData(Player player) {
        return playerData.computeIfAbsent(player.getUniqueId(),
            uuid -> new PlayerData(uuid, player.getName()));
    }

    public void mutePlayer(Player player, long duration) {
        if (player.hasPermission("lumatrivia.mute.bypass")) {
            return;
        }

        PlayerData data = getPlayerData(player);
        data.mute(duration);

        // Cancel existing mute task if any
        MuteTask existingTask = muteTasks.remove(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }

        // Start new mute task
        MuteTask task = new MuteTask(plugin, data);
        muteTasks.put(player.getUniqueId(), task);

        // Send mute message
        String muteMsg = plugin.getConfig().getString("game.mute-incorrect.message",
            "&cYou are muted until this trivia game ends!");
        MessageUtil.sendMessage(player, muteMsg);
    }

    public void unmutePlayer(Player player) {
        PlayerData data = getPlayerData(player);
        data.unmute();

        // Cancel mute task if exists
        MuteTask task = muteTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public boolean isPlayerMuted(Player player) {
        if (player.hasPermission("lumatrivia.mute.bypass")) {
            return false;
        }
        PlayerData data = getPlayerData(player);
        return data.isMuted();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (isPlayerMuted(player)) {
            event.setCancelled(true);
            String muteMsg = plugin.getConfig().getString("game.mute-incorrect.message",
                "&cYou are muted until this trivia game ends!");
            MessageUtil.sendMessage(player, muteMsg);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load or create player data
        getPlayerData(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Cancel mute task if exists
        MuteTask task = muteTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }

        // Remove player data
        playerData.remove(playerId);
    }
} 