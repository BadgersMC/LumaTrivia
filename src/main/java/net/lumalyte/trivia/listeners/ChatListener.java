package net.lumalyte.trivia.listeners;

import net.lumalyte.trivia.TriviaPlugin;
import net.lumalyte.trivia.managers.TriviaManager;
import net.lumalyte.trivia.util.MessageUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    private final TriviaPlugin plugin;
    private final TriviaManager triviaManager;

    public ChatListener(TriviaPlugin plugin, TriviaManager triviaManager) {
        this.plugin = plugin;
        this.triviaManager = triviaManager;
        
        // Register events manually to ensure we get priority
        plugin.getServer().getPluginManager().registerEvent(
            AsyncPlayerChatEvent.class,
            this,
            EventPriority.LOWEST,
            (listener, event) -> onLegacyChat((AsyncPlayerChatEvent) event),
            plugin,
            true
        );
        
        plugin.getServer().getPluginManager().registerEvent(
            AsyncChatEvent.class,
            this,
            EventPriority.LOWEST,
            (listener, event) -> onModernChat((AsyncChatEvent) event),
            plugin,
            true
        );
    }

    private void onLegacyChat(AsyncPlayerChatEvent event) {
        if (triviaManager.handleChat(event)) {
            event.setCancelled(true);
            String muteMsg = plugin.getConfig().getString("game.mute-incorrect.message",
                "&cYou are muted until this trivia game ends!");
            MessageUtil.sendMessage(event.getPlayer(), muteMsg);
            return;
        }

        // Get and normalize the message
        String originalMessage = MessageUtil.stripColor(event.getMessage().trim().toLowerCase());
        String answer = originalMessage;

        // Check if it's a valid answer format
        boolean isValidAnswer = false;
        
        // Check for multiple choice (A, B, C, D)
        if (answer.length() == 1 && answer.matches("[abcd]")) {
            isValidAnswer = true;
        }

        // Check for true/false (T, F, True, False)
        if (answer.matches("^(t(rue)?|f(alse)?)$")) {
            // Normalize to single letter
            answer = answer.startsWith("t") ? "a" : "b";
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

    private void onModernChat(AsyncChatEvent event) {
        if (triviaManager.handleChat(event)) {
            event.setCancelled(true);
            String muteMsg = plugin.getConfig().getString("game.mute-incorrect.message",
                "&cYou are muted until this trivia game ends!");
            MessageUtil.sendMessage(event.getPlayer(), muteMsg);
            return;
        }

        // Get message content
        if (!(event.message() instanceof TextComponent textComponent)) {
            return;
        }

        // Get and normalize the message
        String originalMessage = MessageUtil.stripColor(textComponent.content().trim().toLowerCase());
        String answer = originalMessage;

        // Check if it's a valid answer format
        boolean isValidAnswer = false;
        
        // Check for multiple choice (A, B, C, D)
        if (answer.length() == 1 && answer.matches("[abcd]")) {
            isValidAnswer = true;
        }

        // Check for true/false (T, F, True, False)
        if (answer.matches("^(t(rue)?|f(alse)?)$")) {
            // Normalize to single letter
            answer = answer.startsWith("t") ? "a" : "b";
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