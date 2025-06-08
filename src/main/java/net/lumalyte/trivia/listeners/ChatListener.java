package net.lumalyte.trivia.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.TextComponent;
import net.lumalyte.trivia.managers.TriviaManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {
    private final TriviaManager triviaManager;

    public ChatListener(TriviaManager triviaManager) {
        this.triviaManager = triviaManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncChatEvent event) {
        // Get the raw message from the TextComponent
        if (!(event.message() instanceof TextComponent textComponent)) {
            return;
        }
        
        String message = textComponent.content().trim().toLowerCase();
        
        // Check if it's a valid answer format
        boolean isValidAnswer = false;
        
        // Check for multiple choice (A, B, C, D)
        if (message.length() == 1 && message.matches("[abcd]")) {
            isValidAnswer = true;
        }
        
        // Check for true/false (T, F, True, False)
        if (message.matches("^(t(rue)?|f(alse)?)$")) {
            // Normalize to single letter
            message = message.substring(0, 1);
            isValidAnswer = true;
        }
        
        if (isValidAnswer) {
            // If player has already answered, cancel their message
            if (triviaManager.hasPlayerAnswered(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
                return;
            }
            
            triviaManager.checkAnswer(event.getPlayer(), message);
        }
    }
} 