package net.lumalyte.trivia.listeners;

import net.lumalyte.trivia.TriviaPlugin;
import net.lumalyte.trivia.managers.TriviaManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {
    private final TriviaPlugin plugin;
    private final TriviaManager triviaManager;

    public ChatListener(TriviaPlugin plugin, TriviaManager triviaManager) {
        this.plugin = plugin;
        this.triviaManager = triviaManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncChatEvent event) {
        // Check if player is muted first
        if (triviaManager.handleChat(event)) {
            event.setCancelled(true);
            return;
        }

        // Get message content
        if (!(event.message() instanceof TextComponent textComponent)) {
            return;
        }

        String message = textComponent.content().trim();
        if (!message.startsWith("!")) {
            return;
        }

        String answer = message.substring(1).trim();
        if (answer.isEmpty()) {
            return;
        }

        // Cancel the chat message
        event.setCancelled(true);

        // Process the answer on the main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> 
            triviaManager.checkAnswer(event.getPlayer(), answer));
    }
} 