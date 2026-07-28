package net.badgersmc.trivia.application

import net.badgersmc.trivia.infrastructure.config.ContentFilterConfig
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import java.util.logging.Logger

/**
 * Filters trivia questions based on configurable regex patterns (REQ-009).
 */
class ContentFilter(private val config: ContentFilterConfig) {

    private val blockedPatterns: List<Pattern> = config.blockedPatterns.mapNotNull { pattern ->
        try {
            Pattern.compile(pattern)
        } catch (e: PatternSyntaxException) {
            Logger.getLogger(ContentFilter::class.java.name)
                .warning("Invalid regex pattern in content filter: $pattern")
            null
        }
    }

    private val requiredPattern: Pattern? = config.requiredPattern.takeIf { it.isNotBlank() }?.let {
        try {
            Pattern.compile(it)
        } catch (e: PatternSyntaxException) {
            Logger.getLogger(ContentFilter::class.java.name)
                .warning("Invalid required pattern in content filter: $it")
            null
        }
    }

    fun isAllowed(question: String, category: String): Boolean {
        if (!config.enabled) return true

        if (requiredPattern != null && !requiredPattern.matcher(question).find()) {
            return false
        }

        for (pattern in blockedPatterns) {
            if (pattern.matcher(question).find()) return false
        }

        return true
    }
}
