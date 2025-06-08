package net.lumalyte.trivia.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.lumalyte.trivia.TriviaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class MessageUtil {
    private static final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder()
        .character('&')
        .hexColors()
        .useUnusualXRepeatedCharacterHexFormat()
        .build();

    private static final MiniMessage miniMessage = MiniMessage.builder().build();

    public static void broadcast(String message) {
        Component component = formatMultiLine(parseGlobalPlaceholders(message));
        Bukkit.getServer().broadcast(component);
    }

    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(formatMultiLine(parseGlobalPlaceholders(message)));
    }

    private static String parseGlobalPlaceholders(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        TriviaPlugin plugin = TriviaPlugin.getInstance();
        return message
            .replace("%prefix%", plugin.getConfig().getString("messages.prefix", "&6[&bLumaTrivia&6]&r "))
            .replace("%separator%", plugin.getConfig().getString("messages.separator", "&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private static Component formatMultiLine(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        // Split by newlines while preserving empty lines
        String[] lines = message.split("\\R", -1);
        Component result = formatLegacy(lines[0]);

        // Add newlines and subsequent lines
        for (int i = 1; i < lines.length; i++) {
            result = result.append(Component.newline())
                         .append(formatLegacy(lines[i]));
        }

        return result;
    }

    public static Component formatLegacy(String message) {
        return legacySerializer.deserialize(message).decoration(TextDecoration.ITALIC, false);
    }

    public static Component formatMini(String message) {
        return miniMessage.deserialize(message).decoration(TextDecoration.ITALIC, false);
    }

    public static String stripColor(String message) {
        return legacySerializer.serialize(legacySerializer.deserialize(message));
    }
} 