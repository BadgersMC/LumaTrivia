package net.badgersmc.trivia.infrastructure.persistence

import net.badgersmc.trivia.domain.PlayerStats
import java.util.UUID

/** Repository for persistent player trivia statistics. */
interface StatsRepository {
    /** Save or update player stats. Upserts by playerId. */
    fun save(stats: PlayerStats)

    /** Find stats by player UUID, or null if not found. */
    fun findByPlayerId(playerId: UUID): PlayerStats?

    /** Get the top N players sorted by points descending. */
    fun getTopPlayers(limit: Int): List<PlayerStats>
}
