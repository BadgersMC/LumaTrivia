package net.badgersmc.trivia.infrastructure.persistence

import org.jetbrains.exposed.sql.Database
import java.io.File

/**
 * Opens and manages a SQLite database connection (REQ-018).
 */
class DatabaseFactory(private val dataFolder: File, private val fileName: String) {

    val database: Database by lazy {
        dataFolder.mkdirs()
        Database.connect(
            url = "jdbc:sqlite:${dataFolder.absolutePath}/$fileName",
            driver = "org.sqlite.JDBC",
        )
    }

    fun close() {}
}
