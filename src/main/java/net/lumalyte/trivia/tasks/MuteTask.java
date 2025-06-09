package net.lumalyte.trivia.tasks;

import net.lumalyte.trivia.TriviaPlugin;
import net.lumalyte.trivia.models.PlayerData;
import net.lumalyte.trivia.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class MuteTask extends BukkitRunnable {
    private final TriviaPlugin plugin;
    private final PlayerData data;

    public MuteTask(TriviaPlugin plugin, PlayerData data) {
        this.plugin = plugin;
        this.data = data;
        this.runTaskTimerAsynchronously(plugin, 0L, 20L); // Check every second
    }

    @Override
    public void run() {
        if (this.data == null) {
            this.cancel();
            return;
        }

        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) {
            this.cancel();
            return;
        }

        // Check if player has bypass permission
        if (player.hasPermission("lumatrivia.mute.bypass")) {
            data.unmute();
            this.cancel();
            return;
        }

        if (data.isMuteExpired()) {
            data.unmute();
            String unmuteMsg = plugin.getConfig().getString("game.mute-incorrect.unmute-message",
                "&aYou are no longer muted!");
            MessageUtil.sendMessage(player, unmuteMsg);
            this.cancel();
        }
    }
} 