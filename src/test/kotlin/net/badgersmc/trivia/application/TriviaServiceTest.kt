package net.badgersmc.trivia.application

import io.mockk.*
import net.badgersmc.nexus.scheduler.NexusScheduler
import net.badgersmc.trivia.domain.PlayerStats
import net.badgersmc.trivia.domain.Question
import net.badgersmc.trivia.infrastructure.config.*
import net.badgersmc.trivia.infrastructure.persistence.StatsRepository
import org.bukkit.Server
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.*

class TriviaServiceTest {
    private lateinit var plugin: JavaPlugin
    private lateinit var server: Server
    private lateinit var console: ConsoleCommandSender
    private lateinit var fetcher: QuestionFetcher
    private lateinit var statsRepo: StatsRepository
    private lateinit var scheduler: NexusScheduler
    private lateinit var config: TriviaConfig
    private lateinit var service: TriviaService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        plugin = mockk(relaxed = true)
        server = mockk(relaxed = true)
        console = mockk(relaxed = true)
        fetcher = mockk(relaxed = true)
        statsRepo = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)

        every { plugin.server } returns server
        every { server.consoleSender } returns console
        every { server.scheduler } returns mockk(relaxed = true)

        config = TriviaConfig(
            api = ApiConfig("", 24, 10000),
            game = GameConfig(
                answerTime = 30, cooldown = 300,
                muteIncorrect = MuteIncorrectConfig(true),
                schedule = ScheduleConfig(false, emptyList()),
                categories = emptyList(), difficulties = emptyList(),
            ),
            rewards = mapOf(
                "easy" to RewardConfig(listOf("eco give %player% 100"), 1),
                "medium" to RewardConfig(listOf("eco give %player% 250"), 2),
                "hard" to RewardConfig(listOf("eco give %player% 500"), 3),
            ),
            contentFilter = ContentFilterConfig(false, false, emptyList(), ""),
            storage = StorageConfig("sqlite", "test.db"),
        )

        service = TriviaService(plugin, config, fetcher, statsRepo, scheduler)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `start game when idle`() {
        val question = createQuestion("Paris")
        every { fetcher.isEmpty } returns false
        every { fetcher.poll() } returns question

        val started = service.startGame()
        assertTrue(started)
        assertTrue(service.isGameActive())
    }

    @Test
    fun `reject start when game already active`() {
        every { fetcher.isEmpty } returns false
        every { fetcher.poll() } returns createQuestion("Paris")
        service.startGame()

        val second = service.startGame()
        assertFalse(second)
    }

    @Test
    fun `reject start during cooldown`() {
        // Start a game and end it (correct answer) to trigger cooldown
        val question = createQuestion("Paris")
        every { fetcher.isEmpty } returns false
        every { fetcher.poll() } returns question
        service.startGame()

        val player = mockPlayer()
        service.checkAnswer(player, question.correctAnswerLetter.lowercase()) // Correct = ends game, starts cooldown

        // Second start should be rejected
        val second = service.startGame()
        assertFalse(second)
    }

    @Test
    fun `correct answer ends game and records stats`() {
        val question = createQuestion("Paris")
        every { fetcher.isEmpty } returns false
        every { fetcher.poll() } returns question
        service.startGame()

        // Compute the correct answer letter from the shuffled question
        val correctLetter = question.correctAnswerLetter.lowercase()

        val player = mockPlayer()
        val result = service.checkAnswer(player, correctLetter)

        assertEquals(TriviaService.AnswerResult.CORRECT, result)
        assertFalse(service.isGameActive())
        verify { statsRepo.save(any()) }
    }

    @Test
    fun `wrong answer mutes player and keeps game active`() {
        val question = createQuestion("Paris")
        every { fetcher.isEmpty } returns false
        every { fetcher.poll() } returns question
        service.startGame()

        val player = mockPlayer()
        val result = service.checkAnswer(player, wrongLetter(question))

        assertEquals(TriviaService.AnswerResult.WRONG, result)
        assertTrue(service.isGameActive())
        assertTrue(service.isPlayerMuted(player.uniqueId))
    }

    @Test
    fun `already answered player is rejected`() {
        val question = createQuestion("Paris")
        every { fetcher.isEmpty } returns false
        every { fetcher.poll() } returns question
        service.startGame()

        val player = mockPlayer()
        service.checkAnswer(player, wrongLetter(question)) // wrong, still active
        val second = service.checkAnswer(player, wrongLetter(question)) // try again

        assertEquals(TriviaService.AnswerResult.ALREADY_ANSWERED, second)
    }

    @Test
    fun `time up ends game`() {
        val question = createQuestion("Paris")
        every { fetcher.isEmpty } returns false
        every { fetcher.poll() } returns question
        service.startGame()

        service.timeUp()
        assertFalse(service.isGameActive())
    }

    @Test
    fun `mute bypass permission exempts from muting`() {
        val question = createQuestion("Paris")
        every { fetcher.isEmpty } returns false
        every { fetcher.poll() } returns question
        service.startGame()

        val player = mockPlayer()
        every { player.hasPermission("lumatrivia.mute.bypass") } returns true

        val result = service.checkAnswer(player, wrongLetter(question))
        assertEquals(TriviaService.AnswerResult.WRONG, result)
        assertFalse(service.isPlayerMuted(player.uniqueId))
    }

    @Test
    fun `current question is exposed`() {
        val question = createQuestion("Paris")
        every { fetcher.isEmpty } returns false
        every { fetcher.poll() } returns question
        service.startGame()

        assertNotNull(service.currentQuestion)
        assertEquals("What is the capital?", service.currentQuestion?.question)
    }


    /** Returns a letter that is NOT the correct answer. */
    private fun wrongLetter(question: Question): String {
        val correct = question.correctAnswerLetter.lowercase()[0]
        return if (correct == 'a') "b" else "a"
    }

    private fun createQuestion(answer: String) = Question(
        question = "What is the capital?",
        correctAnswer = answer,
        incorrectAnswers = listOf("London", "Berlin", "Madrid"),
        category = "Geography", difficulty = "easy", type = "multiple",
    )

    private fun mockPlayer(): Player {
        val uuid = UUID.randomUUID()
        val player: Player = mockk(relaxed = true)
        every { player.uniqueId } returns uuid
        every { player.name } returns "Player_$uuid"
        every { player.hasPermission(any<String>()) } returns false
        every { player.isOnline } returns true
        return player
    }
}
