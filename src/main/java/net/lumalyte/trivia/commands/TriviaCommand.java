package net.lumalyte.trivia.commands;

import net.lumalyte.trivia.TriviaPlugin;
import net.lumalyte.trivia.managers.TriviaManager;
import net.lumalyte.trivia.models.PlayerStats;
import net.lumalyte.trivia.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TriviaCommand extends Command {
    private final TriviaPlugin plugin;

    public TriviaCommand(TriviaPlugin plugin, TriviaManager triviaManager) {
        super("trivia");
        this.plugin = plugin;
        this.setDescription("Main command for trivia games");
        this.setUsage("/<command> [start|stats|top|reload]");
        this.setAliases(List.of("tr"));
        this.setPermission("lumatrivia.use");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                if (!sender.hasPermission("lumatrivia.start")) {
                    MessageUtil.sendMessage(sender, plugin.getConfig().getString("messages.error.no-permission"));
                    return true;
                }
                plugin.getTriviaManager().startGame();
                break;

            case "stats":
                if (!sender.hasPermission("lumatrivia.use")) {
                    MessageUtil.sendMessage(sender, plugin.getConfig().getString("messages.error.no-permission"));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    MessageUtil.sendMessage(sender, plugin.getConfig().getString("messages.error.player-only"));
                    return true;
                }
                showStats((Player) sender);
                break;

            case "top":
                if (!sender.hasPermission("lumatrivia.use")) {
                    MessageUtil.sendMessage(sender, plugin.getConfig().getString("messages.error.no-permission"));
                    return true;
                }
                showLeaderboard(sender);
                break;

            case "reload":
                if (!sender.hasPermission("lumatrivia.admin")) {
                    MessageUtil.sendMessage(sender, plugin.getConfig().getString("messages.error.no-permission"));
                    return true;
                }
                plugin.reloadConfig();
                MessageUtil.sendMessage(sender, plugin.getConfig().getString("messages.commands.reload"));
                break;

            default:
                sendUsage(sender);
                break;
        }

        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            if (sender.hasPermission("lumatrivia.start")) {
                completions.add("start");
            }
            if (sender.hasPermission("lumatrivia.use")) {
                completions.add("stats");
                completions.add("top");
            }
            if (sender.hasPermission("lumatrivia.admin")) {
                completions.add("reload");
            }
        }
        
        return completions;
    }

    private void showStats(Player player) {
        PlayerStats stats = plugin.getTriviaManager().getLeaderboardManager().getPlayerStats(player.getUniqueId());
        if (stats == null) {
            stats = new PlayerStats(player.getUniqueId(), player.getName());
        }

        String message = plugin.getConfig().getString("messages.commands.stats")
            .replace("%player%", player.getName())
            .replace("%total%", String.valueOf(stats.getTotalCorrect()))
            .replace("%easy%", String.valueOf(stats.getEasyCorrect()))
            .replace("%medium%", String.valueOf(stats.getMediumCorrect()))
            .replace("%hard%", String.valueOf(stats.getHardCorrect()))
            .replace("%points%", String.valueOf(stats.getPoints()));

        MessageUtil.sendMessage(player, message);
    }

    private void showLeaderboard(CommandSender sender) {
        String leaderboard = plugin.getTriviaManager().getLeaderboardManager().formatLeaderboard(10);
        String message = plugin.getConfig().getString("messages.commands.leaderboard")
            .replace("%leaderboard%", leaderboard);
        MessageUtil.sendMessage(sender, message);
    }

    private void sendUsage(CommandSender sender) {
        StringBuilder usage = new StringBuilder();
        if (sender.hasPermission("lumatrivia.start")) {
            usage.append("\n&e/trivia start &7- Start a new trivia game");
        }
        if (sender.hasPermission("lumatrivia.use")) {
            usage.append("\n&e/trivia stats &7- View your trivia stats");
            usage.append("\n&e/trivia top &7- View the leaderboard");
        }
        if (sender.hasPermission("lumatrivia.admin")) {
            usage.append("\n&e/trivia reload &7- Reload the plugin configuration");
        }
        
        MessageUtil.sendMessage(sender, "&6=== LumaTrivia Commands ===" + usage.toString());
    }
} 