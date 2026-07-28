package net.badgersmc.trivia.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import org.junit.jupiter.api.Test

class LayerRulesTest {

    @Test
    fun `domain layer depends on nothing outside domain and kotlin stdlib`() {
        Konsist
            .scopeFromProduction()
            .assertArchitecture {
                val domain = Layer("domain", "net.badgersmc.trivia.domain..")
                val application = Layer("application", "net.badgersmc.trivia.application..")
                val infrastructure = Layer("infrastructure", "net.badgersmc.trivia.infrastructure..")

                domain.dependsOnNothing()
                application.dependsOn(domain)
                infrastructure.dependsOn(application, domain)
            }
    }
}
