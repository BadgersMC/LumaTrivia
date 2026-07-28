package net.badgersmc.trivia.infrastructure.persistence

import net.badgersmc.trivia.domain.PlayerStats
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/** SQLite-backed implementation of [StatsRepository] using Exposed ORM. */
class SqliteStatsRepository(private val dbFactory: DatabaseFactory) : StatsRepository {

    init {
        transaction(dbFactory.database) {
            SchemaUtils.create(StatsTable)
        }
    }

    override fun save(stats: PlayerStats) {
        transaction(dbFactory.database) {
            val existing = StatsTable.selectAll()
                .where { StatsTable.playerId eq stats.playerId.toString() }
                .singleOrNull()

            if (existing != null) {
                StatsTable.update({ StatsTable.playerId eq stats.playerId.toString() }) {
                    it[playerName] = stats.playerName
                    it[easyCorrect] = stats.easyCorrect
                    it[mediumCorrect] = stats.mediumCorrect
                    it[hardCorrect] = stats.hardCorrect
                    it[totalCorrect] = stats.totalCorrect
                    it[points] = stats.points
                }
            } else {
                StatsTable.insert {
                    it[playerId] = stats.playerId.toString()
                    it[playerName] = stats.playerName
                    it[easyCorrect] = stats.easyCorrect
                    it[mediumCorrect] = stats.mediumCorrect
                    it[hardCorrect] = stats.hardCorrect
                    it[totalCorrect] = stats.totalCorrect
                    it[points] = stats.points
                }
            }
        }
    }

    override fun findByPlayerId(playerId: UUID): PlayerStats? {
        return transaction(dbFactory.database) {
            StatsTable.selectAll()
                .where { StatsTable.playerId eq playerId.toString() }
                .singleOrNull()
                ?.let { rowToStats(it) }
        }
    }

    override fun getTopPlayers(limit: Int): List<PlayerStats> {
        return transaction(dbFactory.database) {
            StatsTable.selectAll()
                .orderBy(StatsTable.points to SortOrder.DESC)
                .limit(limit)
                .map { rowToStats(it) }
        }
    }

    private fun rowToStats(row: ResultRow): PlayerStats {
        return PlayerStats(
            playerId = UUID.fromString(row[StatsTable.playerId]),
            playerName = row[StatsTable.playerName],
            totalCorrect = row[StatsTable.totalCorrect],
            easyCorrect = row[StatsTable.easyCorrect],
            mediumCorrect = row[StatsTable.mediumCorrect],
            hardCorrect = row[StatsTable.hardCorrect],
            points = row[StatsTable.points],
        )
    }
}
