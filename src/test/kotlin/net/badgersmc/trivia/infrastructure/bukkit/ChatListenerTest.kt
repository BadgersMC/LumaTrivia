package net.badgersmc.trivia.infrastructure.bukkit

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.trivia.application.ChatPlatform
import net.badgersmc.trivia.application.TriviaService
import net.badgersmc.trivia.infrastructure.persistence.ParticipationPreferences
import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.junit.jupiter.api.Test
import java.util.UUID

class ChatListenerTest {
    @Test
    fun `opted out player chat passes through untouched during active trivia`() {
        val trivia = mockk<TriviaService>()
        val chat = mockk<ChatPlatform>()
        val lang = mockk<LangService>()
        val preferences = mockk<ParticipationPreferences>()
        val player = mockk<Player>()
        val event = mockk<AsyncPlayerChatEvent>()
        val playerId = UUID.randomUUID()

        every { trivia.isActive } returns true
        every { player.uniqueId } returns playerId
        every { event.player } returns player
        every { preferences.isOptedIn(playerId) } returns false

        ChatListener(trivia, lang, chat, preferences).onChat(event)

        verify(exactly = 0) { event.isCancelled = any() }
        verify(exactly = 0) { chat.isAnswerChat(any(), any()) }
        verify(exactly = 0) { trivia.tryClaimAnswer(any()) }
    }
}
