package net.badgersmc.trivia.infrastructure.persistence

import org.jetbrains.exposed.sql.Table

/** Exposed table definition for player trivia statistics. */
object StatsTable : Table("player_stats") {
    val playerId = text("player_id")
    val playerName = text("player_name")
    val easyCorrect = integer("easy_correct").default(0)
    val mediumCorrect = integer("medium_correct").default(0)
    val hardCorrect = integer("hard_correct").default(0)
    val totalCorrect = integer("total_correct").default(0)
    val points = integer("points").default(0)

    override val primaryKey = PrimaryKey(playerId)
}
