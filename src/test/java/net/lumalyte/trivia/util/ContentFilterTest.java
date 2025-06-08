package net.lumalyte.trivia.util;

import net.lumalyte.trivia.TriviaPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContentFilterTest {
    @Mock
    private TriviaPlugin plugin;
    
    @Mock
    private Logger logger;

    private YamlConfiguration config;
    private ContentFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up config
        config = new YamlConfiguration();
        config.set("content-filter.enabled", true);
        config.set("content-filter.log-filtered", true);
        config.set("content-filter.blocked-patterns", Arrays.asList(
            "(?i)suicide|death|kill",
            "(?i)sex|nsfw|adult",
            "(?i)violence"
        ));
        
        // Set up mocks
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(logger);
        
        filter = new ContentFilter(plugin);
    }

    @Test
    void testBlockedPatterns() {
        // Test blocked words
        assertFalse(filter.isAllowed("This question contains death", "History"));
        assertFalse(filter.isAllowed("What happens after SUICIDE?", "Philosophy"));
        assertFalse(filter.isAllowed("How many people did the killer kill?", "Crime"));
        assertFalse(filter.isAllowed("What is the meaning of SEX?", "Biology"));
        assertFalse(filter.isAllowed("This contains VIOLENCE!", "History"));
        
        // Test case sensitivity
        assertFalse(filter.isAllowed("This contains DeAtH!", "History"));
        assertFalse(filter.isAllowed("This contains KILL!", "History"));
        
        // Test safe questions
        assertTrue(filter.isAllowed("What is the capital of France?", "Geography"));
        assertTrue(filter.isAllowed("Who wrote Romeo and Juliet?", "Literature"));
        assertTrue(filter.isAllowed("What is 2+2?", "Mathematics"));
    }

    @Test
    void testRequiredPattern() {
        // Set up required pattern
        config.set("content-filter.required-pattern", "^[A-Za-z\\s\\?]+$");
        filter = new ContentFilter(plugin);
        
        // Test valid questions (only letters, spaces, and question marks)
        assertTrue(filter.isAllowed("What is the capital of France?", "Geography"));
        assertTrue(filter.isAllowed("Who wrote this book?", "Literature"));
        
        // Test invalid questions (contains numbers or special characters)
        assertFalse(filter.isAllowed("What is 2+2?", "Mathematics"));
        assertFalse(filter.isAllowed("What happened in 1492?", "History"));
        assertFalse(filter.isAllowed("What is π?", "Mathematics"));
    }

    @Test
    void testDisabledFilter() {
        // Disable filter
        config.set("content-filter.enabled", false);
        filter = new ContentFilter(plugin);
        
        // All questions should pass when filter is disabled
        assertTrue(filter.isAllowed("This contains death", "History"));
        assertTrue(filter.isAllowed("This contains VIOLENCE!", "History"));
        assertTrue(filter.isAllowed("What is 2+2?", "Mathematics"));
    }

    @Test
    void testInvalidPatterns() {
        // Set up invalid patterns
        config.set("content-filter.blocked-patterns", Arrays.asList(
            "(?i)valid|pattern",
            "[invalid(pattern",  // Invalid regex
            "(?i)another|valid|pattern"
        ));
        filter = new ContentFilter(plugin);
        
        // Test that valid patterns still work
        assertFalse(filter.isAllowed("This contains valid content", "History"));
        assertFalse(filter.isAllowed("This contains another thing", "History"));
        
        // Verify warning was logged for invalid pattern
        verify(logger).warning(contains("Invalid regex pattern"));
    }
} 