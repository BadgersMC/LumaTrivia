package net.badgersmc.trivia.domain

/** A trivia question with shuffled multiple-choice answers. */
class Question(
    val question: String,
    val correctAnswer: String,
    val incorrectAnswers: List<String>,
    val category: String,
    val difficulty: String,
    val type: String,
) {
    /** All answers in shuffled order. Correct answer is at [correctAnswerIndex]. */
    val shuffledAnswers: List<String>

    /** Index of [correctAnswer] within [shuffledAnswers]. */
    private val correctAnswerIndex: Int

    init {
        val all = mutableListOf<String>()
        all.addAll(incorrectAnswers)
        all.add(correctAnswer)
        all.shuffle()
        shuffledAnswers = all.toList()
        correctAnswerIndex = shuffledAnswers.indexOf(correctAnswer)
    }

    /**
     * Validates a player's answer. Accepts:
     * - Single letter a/b/c/d (case-insensitive) mapping to shuffled answer index
     * - t/f/true/false for boolean questions (maps to A/B)
     * - Direct text match (case-insensitive)
     */
    fun isCorrectAnswer(answer: String): Boolean {
        val normalized = answer.trim().lowercase()

        // True/false questions
        if (type.equals("boolean", ignoreCase = true)) {
            val mapped = when (normalized) {
                "t", "true" -> "true"
                "f", "false" -> "false"
                else -> normalized
            }
            return correctAnswer.equals(mapped, ignoreCase = true)
        }

        // Single-letter answers
        if (normalized.length == 1 && normalized[0] in 'a'..'d') {
            val index = normalized[0] - 'a'
            return index == correctAnswerIndex
        }

        // Direct text comparison
        return correctAnswer.equals(normalized, ignoreCase = true)
    }

    /** Returns MiniMessage-formatted answer options (A) B) C) D) or True/False). */
    val formattedAnswers: String
        get() {
            val sb = StringBuilder()

            if (type.equals("boolean", ignoreCase = true)) {
                sb.append("<newline><yellow>A) <white>True")
                sb.append("<newline><yellow>B) <white>False")
                return sb.toString()
            }

            for (i in shuffledAnswers.indices) {
                sb.append("<newline><yellow>")
                sb.append(('A' + i))
                sb.append(") <white>")
                sb.append(shuffledAnswers[i])
            }
            return sb.toString()
        }

    /** Returns the letter (A-D or A/B) of the correct answer. */
    val correctAnswerLetter: String
        get() {
            if (type.equals("boolean", ignoreCase = true)) {
                return if (correctAnswer.equals("True", ignoreCase = true)) "A" else "B"
            }
            return ('A' + correctAnswerIndex).toString()
        }
}
