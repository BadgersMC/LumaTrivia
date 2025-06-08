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
        // Get message content
        if (!(event.message() instanceof TextComponent textComponent)) {
            return;
        }

        // Get and normalize the message
        String originalMessage = textComponent.content().trim().toLowerCase();
        String answer = originalMessage;

        // Check if player is muted (do this for ALL messages)
        if (triviaManager.handleChat(event)) {
            event.setCancelled(true);
            return;
        }

        // Check if it's a valid answer format
        boolean isValidAnswer = false;
        
        // Check for multiple choice (A, B, C, D)
        if (answer.length() == 1 && answer.matches("[abcd]")) {
            isValidAnswer = true;
        }

        // Check for true/false (T, F, True, False)
        if (answer.matches("^(t(rue)?|f(alse)?)$")) {
            // Normalize to single letter
            answer = answer.substring(0, 1);
            isValidAnswer = true;
        }
        
        if (isValidAnswer) {
            // Store final answer for lambda
            final String finalAnswer = answer;
            
            // Cancel the chat message
            event.setCancelled(true);

            // Process the answer on the main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> 
                triviaManager.checkAnswer(event.getPlayer(), finalAnswer));
        }
    }
} 