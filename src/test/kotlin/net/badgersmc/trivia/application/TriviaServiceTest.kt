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
import java.net.InetSocketAddress
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
    private var nextIpOctet = 1

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        nextIpOctet = 1
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
                answerTime = 10, cooldown = 300,
                muteIncorrect = MuteIncorrectConfig(true),
                schedule = ScheduleConfig(false, emptyList()),
                channel = "global",
                categories = emptyList(), difficulties = emptyList(),
                rewardPayout = RewardPayoutConfig(
                    maxPaidWinners = 3,
                    onePerIp = true,
                    placementCommands = mapOf(
                        1 to listOf("eco give %player% 15"),
                        2 to listOf("eco give %player% 10"),
                        3 to listOf("eco give %player% 5"),
                    ),
                ),
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
        verify { server.dispatchCommand(console, "eco give $playerName 15") }
    }

    @Test
    fun `all correct players get stats while payouts follow placement order`() {
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
        verify { server.dispatchCommand(console, "eco give $firstName 15") }
        verify { server.dispatchCommand(console, "eco give $secondName 10") }
    }

    @Test
    fun `only first three qualifying correct players receive economy rewards`() {
        val question = startQuestion()
        val players = List(4) { mockPlayer() }
        val names = players.map { it.name }
        val answer = question.correctAnswerLetter.lowercase()

        players.forEach { player ->
            assertTrue(service.tryClaimAnswer(player.uniqueId))
            assertEquals(TriviaService.AnswerResult.CORRECT, service.checkAnswer(player, answer))
        }

        service.timeUp()

        verify(exactly = 4) { statsRepo.save(any()) }
        verify { server.dispatchCommand(console, "eco give ${names[0]} 15") }
        verify { server.dispatchCommand(console, "eco give ${names[1]} 10") }
        verify { server.dispatchCommand(console, "eco give ${names[2]} 5") }
        verify(exactly = 0) { server.dispatchCommand(console, match { it.contains(names[3]) }) }
    }

    @Test
    fun `shared IP can earn leaderboard credit but only one economy payout`() {
        val question = startQuestion()
        val first = mockPlayer("203.0.113.10")
        val alt = mockPlayer("203.0.113.10")
        val third = mockPlayer("203.0.113.11")
        val firstName = first.name
        val altName = alt.name
        val thirdName = third.name
        val answer = question.correctAnswerLetter.lowercase()

        listOf(first, alt, third).forEach { player ->
            assertTrue(service.tryClaimAnswer(player.uniqueId))
            assertEquals(TriviaService.AnswerResult.CORRECT, service.checkAnswer(player, answer))
        }

        service.timeUp()

        verify(exactly = 3) { statsRepo.save(any()) }
        verify { server.dispatchCommand(console, "eco give $firstName 15") }
        verify(exactly = 0) { server.dispatchCommand(console, match { it.contains(altName) }) }
        verify { server.dispatchCommand(console, "eco give $thirdName 10") }
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

    private fun mockPlayer(ip: String? = null): Player {
        val uuid = UUID.randomUUID()
        val resolvedIp = ip ?: "198.51.100.${nextIpOctet++}"
        val player: Player = mockk(relaxed = true)
        every { player.uniqueId } returns uuid
        every { player.name } returns "Player_$uuid"
        every { player.address } returns InetSocketAddress(resolvedIp, 25565)
        every { player.hasPermission(any<String>()) } returns false
        every { player.isOnline } returns true
        return player
    }
}
