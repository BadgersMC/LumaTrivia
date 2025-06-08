package net.lumalyte.trivia.util;

import net.lumalyte.trivia.TriviaPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ContentFilter {
    private final TriviaPlugin plugin;
    private final List<Pattern> blockedPatterns;
    private final boolean enabled;
    private final boolean logFiltered;
    private final Pattern requiredPattern;

    public ContentFilter(TriviaPlugin plugin) {
        this.plugin = plugin;
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("content-filter");
        
        if (config == null) {
            this.enabled = false;
            this.logFiltered = false;
            this.blockedPatterns = new ArrayList<>();
            this.requiredPattern = null;
            return;
        }

        this.enabled = config.getBoolean("enabled", true);
        this.logFiltered = config.getBoolean("log-filtered", true);
        
        // Compile blocked patterns
        this.blockedPatterns = new ArrayList<>();
        for (String pattern : config.getStringList("blocked-patterns")) {
            try {
                this.blockedPatterns.add(Pattern.compile(pattern));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid regex pattern in content filter: " + pattern);
            }
        }

        // Compile required pattern
        Pattern tempPattern = null;
        String requiredPatternStr = config.getString("required-pattern", "");
        if (!requiredPatternStr.isEmpty()) {
            try {
                tempPattern = Pattern.compile(requiredPatternStr);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid required pattern in content filter: " + requiredPatternStr);
            }
        }
        this.requiredPattern = tempPattern;
    }

    /**
     * Check if a question should be filtered based on its content.
     * 
     * @param question The question text to check
     * @param category The question category
     * @return true if the question should be allowed, false if it should be filtered
     */
    public boolean isAllowed(String question, String category) {
        if (!enabled) {
            return true;
        }

        // Check required pattern
        if (requiredPattern != null && !requiredPattern.matcher(question).find()) {
            logFiltered(question, "Did not match required pattern");
            return false;
        }

        // Check blocked patterns
        for (Pattern pattern : blockedPatterns) {
            if (pattern.matcher(question).find()) {
                logFiltered(question, "Matched blocked pattern: " + pattern.pattern());
                return false;
            }
        }

        return true;
    }

    private void logFiltered(String question, String reason) {
        if (logFiltered) {
            plugin.getLogger().info("Filtered question: \"" + question + "\" - " + reason);
        }
    }
} 