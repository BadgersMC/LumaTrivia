package net.badgersmc.trivia.application

import io.mockk.*
import net.badgersmc.nexus.scheduler.NexusScheduler
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
    private lateinit var chat: ChatPlatform
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
        chat = mockk(relaxed = true)

        every { plugin.server } returns server
        every { server.consoleSender } returns console
        every { server.scheduler } returns mockk(relaxed = true)
        every { statsRepo.findByPlayerId(any()) } returns null

        config = TriviaConfig(
            api = ApiConfig("", 24, 10000),
            game = GameConfig(
                answerTime = 30, cooldown = 300,
                muteIncorrect = MuteIncorrectConfig(true),
                schedule = ScheduleConfig(false, emptyList()),
                channel = "global",
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

        service = TriviaService(plugin, config, fetcher, statsRepo, scheduler, chat)
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

        assertFalse(service.startGame())
    }

    @Test
    fun `correct answer stays secret and unrewarded until time up`() {
        val question = startQuestion()
        val player = mockPlayer()
        val playerName = player.name

        service.tryClaimAnswer(player.uniqueId)
        val result = service.checkAnswer(player, question.correctAnswerLetter.lowercase())

        assertEquals(TriviaService.AnswerResult.CORRECT, result)
        assertTrue(service.isGameActive())
        verify(exactly = 0) { statsRepo.save(any()) }
        verify(exactly = 0) { server.dispatchCommand(any(), any()) }

        service.timeUp()

        assertFalse(service.isGameActive())
        verify(exactly = 1) { statsRepo.save(any()) }
        verify { server.dispatchCommand(console, "eco give $playerName 100") }
    }

    @Test
    fun `all correct players are rewarded when timer expires`() {
        val question = startQuestion()
        val first = mockPlayer()
        val second = mockPlayer()
        val firstName = first.name
        val secondName = second.name
        val answer = question.correctAnswerLetter.lowercase()

        service.tryClaimAnswer(first.uniqueId)
        service.checkAnswer(first, answer)
        service.tryClaimAnswer(second.uniqueId)
        service.checkAnswer(second, answer)

        verify(exactly = 0) { statsRepo.save(any()) }
        service.timeUp()

        verify(exactly = 2) { statsRepo.save(any()) }
        verify { server.dispatchCommand(console, "eco give $firstName 100") }
        verify { server.dispatchCommand(console, "eco give $secondName 100") }
    }

    @Test
    fun `wrong answer is silent and does not mute or reward`() {
        val question = startQuestion()
        val player = mockPlayer()

        service.tryClaimAnswer(player.uniqueId)
        val result = service.checkAnswer(player, wrongLetter(question))

        assertEquals(TriviaService.AnswerResult.WRONG, result)
        assertTrue(service.isGameActive())
        verify(exactly = 0) { chat.mutePlayer(any(), any()) }
        verify(exactly = 0) { statsRepo.save(any()) }

        service.timeUp()

        verify(exactly = 0) { statsRepo.save(any()) }
        verify(exactly = 0) { server.dispatchCommand(any(), any()) }
    }

    @Test
    fun `already answered player cannot submit again`() {
        val question = startQuestion()
        val player = mockPlayer()

        assertTrue(service.tryClaimAnswer(player.uniqueId))
        service.checkAnswer(player, wrongLetter(question))
        assertFalse(service.tryClaimAnswer(player.uniqueId))
    }

    @Test
    fun `cooldown starts only when round resolves`() {
        val question = startQuestion()
        val player = mockPlayer()

        service.tryClaimAnswer(player.uniqueId)
        service.checkAnswer(player, question.correctAnswerLetter.lowercase())
        assertTrue(service.isGameActive())

        service.timeUp()
        assertFalse(service.startGame())
    }

    @Test
    fun `time up ends game with no answers`() {
        startQuestion()

        service.timeUp()

        assertFalse(service.isGameActive())
        verify(exactly = 0) { statsRepo.save(any()) }
    }

    @Test
    fun `current question is exposed`() {
        startQuestion()

        assertNotNull(service.currentQuestion)
        assertEquals("What is the capital?", service.currentQuestion?.question)
    }

    private fun startQuestion(): Question {
        val question = createQuestion("Paris")
        every { fetcher.isEmpty } returns false
        every { fetcher.poll() } returns question
        assertTrue(service.startGame())
        return question
    }

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
