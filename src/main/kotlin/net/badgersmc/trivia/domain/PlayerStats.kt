package net.badgersmc.trivia.domain

import java.util.UUID

/** Per-player trivia statistics. Sorts by points descending. */
data class PlayerStats(
val playerId: UUID,
var playerName: String,
    var totalCorrect: Int = 0,
    var easyCorrect: Int = 0,
    var mediumCorrect: Int = 0,
    var hardCorrect: Int = 0,
    var points: Int = 0,
) : Comparable<PlayerStats> {

    /** Increments counters based on difficulty using configurable point values. */
    fun addCorrectAnswer(difficulty: String, easyPoints: Int = 1, mediumPoints: Int = 2, hardPoints: Int = 3) {
        totalCorrect++
        when (difficulty.lowercase()) {
            "easy" -> { easyCorrect++; points += easyPoints }
            "medium" -> { mediumCorrect++; points += mediumPoints }
            "hard" -> { hardCorrect++; points += hardPoints }
        }
    }

    /** Sorts descending by points (higher points = "less than" in comparison). */
    override fun compareTo(other: PlayerStats): Int =
        other.points.compareTo(points)
}
