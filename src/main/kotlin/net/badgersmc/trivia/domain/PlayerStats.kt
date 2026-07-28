package net.badgersmc.trivia.domain

import java.util.UUID

/** Per-player trivia statistics. Sorts by points descending. */
data class PlayerStats(
    val playerId: UUID,
    val playerName: String,
    var totalCorrect: Int = 0,
    var easyCorrect: Int = 0,
    var mediumCorrect: Int = 0,
    var hardCorrect: Int = 0,
    var points: Int = 0,
) : Comparable<PlayerStats> {

    /** Increments counters based on difficulty. Easy=1pt, Medium=2pt, Hard=3pt. */
    fun addCorrectAnswer(difficulty: String) {
        totalCorrect++
        when (difficulty.lowercase()) {
            "easy" -> { easyCorrect++; points += 1 }
            "medium" -> { mediumCorrect++; points += 2 }
            "hard" -> { hardCorrect++; points += 3 }
        }
    }

    /** Sorts descending by points (higher points = "less than" in comparison). */
    override fun compareTo(other: PlayerStats): Int =
        other.points.compareTo(points)
}
