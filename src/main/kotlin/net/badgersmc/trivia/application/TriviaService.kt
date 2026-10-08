package net.badgersmc.trivia.application

import net.badgersmc.nexus.scheduler.NexusScheduler
import net.badgersmc.trivia.domain.PlayerStats
import net.badgersmc.trivia.domain.Question
import net.badgersmc.trivia.infrastructure.config.TriviaConfig
import net.badgersmc.trivia.infrastructure.persistence.StatsRepository
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Core trivia game lifecycle service (REQ-001..007, REQ-012, REQ-015).
 * Manages game state, answer validation, cooldowns, deferred rewards, and scheduled games.
 */
class TriviaService(
    private val plugin: JavaPlugin,
    private var config: TriviaConfig,
    var fetcher: QuestionFetcher,
    private val statsRepo: StatsRepository,
    private val scheduler: NexusScheduler,
    private val chat: ChatPlatform,
) {
    enum class AnswerResult { CORRECT, WRONG, ALREADY_ANSWERED, NO_GAME }

    /** Callback for broadcasting components to all players. */
    var broadcast: ((message: Component) -> Unit)? = null
    /** Callback for fetching questions asynchronously (called when cache is empty). */
    var fetchCallback: (() -> Unit)? = null

    /** Guard against concurrent fetch-then-start cycles. */
    private var fetching: Boolean = false

    private var gameActive: Boolean = false
    private var gameTaskId: Int = -1
    private var cooldownUntil: Long = 0L
    private data class CorrectSubmission(
        val playerId: UUID,
        val playerName: String,
        val ipAddress: String?,
    )

    private var currentQuestionData: Question? = null
    private val answeredPlayers: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    private val correctSubmissions = CopyOnWriteArrayList<CorrectSubmission>()

    val currentQuestion: Question? get() = currentQuestionData
    val isActive: Boolean get() = gameActive

    fun isGameActive(): Boolean = gameActive

    /**
     * Atomically claims the answer slot for this player.
     * Returns true if the player hadn't answered yet, false if already claimed.
     * Thread-safe — callable from any thread.
     */
    fun tryClaimAnswer(uuid: UUID): Boolean = answeredPlayers.add(uuid)

    /** Update config at runtime (for reload). */
    fun updateConfig(newConfig: TriviaConfig) {
        config = newConfig
    }

    /** Called by the plugin after async fetch completes, regardless of outcome. */
    fun onFetchDone() {
        fetching = false
    }

    /** Start initial async cache fill via guarded path (prevents concurrent fetches). */
    fun startPrewarm() {
        if (!fetching) {
            fetching = true
            fetchCallback?.invoke()
        }
    }

    /** Start a trivia game. Returns true if the game was started. */
    fun startGame(): Boolean {
        if (gameActive) return false
        if (System.currentTimeMillis() < cooldownUntil) return false

        if (fetcher.isEmpty) {
            if (!fetching) {
                fetching = true
                fetchCallback?.invoke()
            }
            return false
        }

        val question = fetcher.poll() ?: return false
        currentQuestionData = question
        answeredPlayers.clear()
        correctSubmissions.clear()
        gameActive = true

        val mm = MiniMessage.miniMessage()
        broadcast?.invoke(mm.deserialize("<yellow>A new trivia question has been asked!</yellow>"))
        broadcast?.invoke(mm.deserialize("<aqua>${mm.escapeTags(question.question)}</aqua>"))
        broadcast?.invoke(buildClickableOptions(question))

        val answerTime = config.game.answerTime
        gameTaskId = plugin.server.scheduler.scheduleSyncDelayedTask(plugin, { timeUp() }, answerTime * 20L)

        return true
    }

    /**
     * Process a player's locked-in answer. Caller must first claim via [tryClaimAnswer].
     * Correctness is intentionally not announced or rewarded until [timeUp].
     */
    fun checkAnswer(player: Player, answer: String): AnswerResult {
        if (!gameActive) return AnswerResult.NO_GAME
        val question = currentQuestionData ?: return AnswerResult.NO_GAME

        if (question.isCorrectAnswer(answer)) {
            correctSubmissions += CorrectSubmission(
                playerId = player.uniqueId,
                playerName = player.name,
                ipAddress = player.address?.address?.hostAddress?.substringBefore('%'),
            )
            return AnswerResult.CORRECT
        }

        // Wrong answers are intentionally silent. The player has still used their one submission.
        return AnswerResult.WRONG
    }

    /** Called when the answer timer expires. Resolves the entire round at once. */
    fun timeUp() {
        if (!gameActive) return
        val question = currentQuestionData
        val winners = correctSubmissions.toList()
        endGame()

        if (question == null) return

        // Every correct answer counts toward stats/leaderboard, even when the
        // player is outside the economy payout window or shares an IP.
        winners.forEach { winner ->
            recordWin(winner.playerId, winner.playerName, question)
        }

        // Economy rewards are intentionally scarcer: earliest qualifying
        // correct submissions win, with optional one-paid-account-per-IP gating.
        selectPaidWinners(winners).forEachIndexed { index, winner ->
            giveRewards(winner.playerName, question.difficulty, index + 1)
        }

        val mm = MiniMessage.miniMessage()
        broadcast?.invoke(
            mm.deserialize(
                "<red>Time's up!</red> <gray>The correct answer was:</gray> <gold>${mm.escapeTags(question.correctAnswer)}</gold> <dark_gray>(${question.correctAnswerLetter})</dark_gray>"
            )
        )

        if (winners.isEmpty()) {
            broadcast?.invoke(mm.deserialize("<gray>No one answered correctly this round.</gray>"))
        } else {
            val names = winners.joinToString(", ") { mm.escapeTags(it.playerName) }
            broadcast?.invoke(mm.deserialize("<green>Correct:</green> <white>$names</white>"))
        }
    }

    /** Get the cooldown remaining in seconds, or 0 if no cooldown. */
    fun cooldownRemaining(): Long {
        val remaining = (cooldownUntil - System.currentTimeMillis()) / 1000
        return if (remaining > 0) remaining else 0
    }

    private fun buildClickableOptions(question: Question): Component {
        var options = Component.text("Options:", NamedTextColor.YELLOW, TextDecoration.BOLD)
        val answerTexts = if (question.type.equals("boolean", ignoreCase = true)) {
            listOf("True", "False")
        } else {
            question.shuffledAnswers
        }

        answerTexts.forEachIndexed { index, answerText ->
            val answerLetter = ('a'.code + index).toChar()
            val displayLetter = answerLetter.uppercaseChar()
            val option = Component.text("$displayLetter) ", NamedTextColor.YELLOW)
                .append(Component.text(answerText, NamedTextColor.WHITE))
                .clickEvent(ClickEvent.runCommand("/trivia answer $answerLetter"))
                .hoverEvent(
                    HoverEvent.showText(
                        Component.text("Click to lock in $displayLetter", NamedTextColor.GRAY)
                    )
                )
            options = options.append(Component.newline()).append(option)
        }

        return options
    }

    private fun endGame() {
        gameActive = false
        if (gameTaskId != -1) {
            plugin.server.scheduler.cancelTask(gameTaskId)
            gameTaskId = -1
        }
        // Release any legacy/platform mutes — round is over.
        chat.clearMutes()
        answeredPlayers.clear()
        correctSubmissions.clear()
        cooldownUntil = System.currentTimeMillis() + (config.game.cooldown * 1000L)
    }

    private fun recordWin(playerId: UUID, playerName: String, question: Question) {
        val existing = statsRepo.findByPlayerId(playerId)
        val stats = existing ?: PlayerStats(playerId, playerName)
        stats.playerName = playerName
        val diff = question.difficulty.lowercase()
        val pts = config.rewards[diff]?.points ?: when (diff) { "hard" -> 3; "medium" -> 2; else -> 1 }
        stats.addCorrectAnswer(
            difficulty = question.difficulty,
            easyPoints = if (diff == "easy") pts else (config.rewards["easy"]?.points ?: 1),
            mediumPoints = if (diff == "medium") pts else (config.rewards["medium"]?.points ?: 2),
            hardPoints = if (diff == "hard") pts else (config.rewards["hard"]?.points ?: 3),
        )
        statsRepo.save(stats)
    }

    private fun selectPaidWinners(winners: List<CorrectSubmission>): List<CorrectSubmission> {
        val payout = config.game.rewardPayout
        if (payout.maxPaidWinners <= 0) return emptyList()

        val claimedIps = mutableSetOf<String>()
        val paid = ArrayList<CorrectSubmission>(payout.maxPaidWinners)
        for (winner in winners) {
            if (payout.onePerIp) {
                val ip = winner.ipAddress
                if (ip != null && !claimedIps.add(ip)) continue
            }
            paid += winner
            if (paid.size >= payout.maxPaidWinners) break
        }
        return paid
    }

    private fun giveRewards(playerName: String, difficulty: String, place: Int) {
        val payout = config.game.rewardPayout
        val commands = if (payout.placementCommands.containsKey(place)) {
            payout.placementCommands[place].orEmpty()
        } else {
            config.rewards[difficulty.lowercase()]?.commands.orEmpty()
        }

        for (command in commands) {
            plugin.server.dispatchCommand(
                plugin.server.consoleSender,
                command
                    .replace("%player%", playerName)
                    .replace("%place%", place.toString())
            )
        }
    }
}
