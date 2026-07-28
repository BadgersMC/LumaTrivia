package net.badgersmc.trivia.application

import net.badgersmc.trivia.infrastructure.config.ContentFilterConfig
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentFilterTest {

    private val config = ContentFilterConfig(
        enabled = true,
        logFiltered = true,
        blockedPatterns = listOf(
            "(?i)suicide|death|kill",
            "(?i)sex|nsfw|adult",
            "(?i)violence"
        ),
        requiredPattern = ""
    )

    @Test
    fun `blocked patterns filter questions`() {
        val filter = ContentFilter(config)
        assertFalse(filter.isAllowed("This question contains death", "History"))
        assertFalse(filter.isAllowed("What happens after SUICIDE?", "Philosophy"))
        assertFalse(filter.isAllowed("How many people did the killer kill?", "Crime"))
        assertFalse(filter.isAllowed("This contains VIOLENCE!", "History"))
    }

    @Test
    fun `case insensitive matching`() {
        val filter = ContentFilter(config)
        assertFalse(filter.isAllowed("This contains DeAtH!", "History"))
        assertFalse(filter.isAllowed("This contains KILL!", "History"))
    }

    @Test
    fun `safe questions pass through`() {
        val filter = ContentFilter(config)
        assertTrue(filter.isAllowed("What is the capital of France?", "Geography"))
        assertTrue(filter.isAllowed("Who wrote Romeo and Juliet?", "Literature"))
        assertTrue(filter.isAllowed("What is 2+2?", "Mathematics"))
    }

    @Test
    fun `disabled filter passes everything`() {
        val disabled = config.copy(enabled = false)
        val filter = ContentFilter(disabled)
        assertTrue(filter.isAllowed("This contains death", "History"))
        assertTrue(filter.isAllowed("This contains VIOLENCE!", "History"))
    }

    @Test
    fun `required pattern blocks non-matching questions`() {
        val withRequired = config.copy(requiredPattern = "^[A-Za-z\\s\\?]+\$")
        val filter = ContentFilter(withRequired)
        assertTrue(filter.isAllowed("What is the capital of France?", "Geography"))
        assertFalse(filter.isAllowed("What is 2+2?", "Mathematics"))
    }

    @Test
    fun `invalid regex patterns are skipped with warning`() {
        // Invalid regex should be caught, valid patterns should still work
        val withInvalid = config.copy(
            blockedPatterns = listOf("(?i)valid|pattern", "[invalid(pattern", "(?i)another")
        )
        val filter = ContentFilter(withInvalid)
        assertFalse(filter.isAllowed("This contains valid content", "History"))
        assertFalse(filter.isAllowed("This contains another thing", "History"))
    }
}
