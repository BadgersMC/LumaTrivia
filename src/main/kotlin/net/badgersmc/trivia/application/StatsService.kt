package net.badgersmc.trivia.application

import net.badgersmc.trivia.domain.PlayerStats
import net.badgersmc.trivia.infrastructure.persistence.StatsRepository
import java.util.UUID

/** Thin wrapper around StatsRepository (REQ-011). */
class StatsService(private val repo: StatsRepository) {
    fun getStats(playerId: UUID): PlayerStats? = repo.findByPlayerId(playerId)
    fun getTopPlayers(limit: Int): List<PlayerStats> = repo.getTopPlayers(limit)
}
