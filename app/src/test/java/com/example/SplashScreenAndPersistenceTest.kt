package com.example

import com.example.game.GameState
import com.example.game.GameStateMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SplashScreenAndPersistenceTest {

    @Test
    fun `test initial state is SPLASH_SCREEN and transitions automatically`() {
        val gameState = GameState(initialMode = GameStateMode.SPLASH_SCREEN)
        assertEquals("Initial mode must be SPLASH_SCREEN", GameStateMode.SPLASH_SCREEN, gameState.mode)

        // Simula avanço de tempo da splash screen (2.8s)
        gameState.update(3000)
        assertEquals("Should transition to RUNNING after splash timer expires", GameStateMode.RUNNING, gameState.mode)
    }

    @Test
    fun `test splash screen can be skipped on tap`() {
        val gameState = GameState(initialMode = GameStateMode.SPLASH_SCREEN)
        assertEquals(GameStateMode.SPLASH_SCREEN, gameState.mode)

        val handled = gameState.handleFire()
        assertTrue("Firing on splash should be handled", handled)
        assertEquals("Should immediately switch to RUNNING on tap", GameStateMode.RUNNING, gameState.mode)
    }

    @Test
    fun `test persistent hiScore initializes and triggers save callback on beating record`() {
        var persistedValue = 500
        val gameState = GameState(
            initialHiScore = persistedValue,
            onHiScoreChanged = { newHi ->
                persistedValue = newHi
            }
        )

        assertEquals("HI-SCORE should load from initial persisted value", 500, gameState.hiScore)

        gameState.handleFire() // Inicia o jogo

        // Elimina um alien para pontuar
        val alien = gameState.formation.invaders[0]
        gameState.player.bullet.spawn(alien.x, alien.y)
        gameState.update(16) // +30 pts (score = 30, hiScore continua 500)
        assertEquals(500, gameState.hiScore)
        assertEquals(500, persistedValue)

        // Supera o HI-SCORE através da Mystery Ship
        gameState.mysteryShip.spawn()
        gameState.mysteryShip.x = 100f
        gameState.player.bullet.spawn(gameState.mysteryShip.x + 2f, gameState.mysteryShip.y)
        // Invoca colisão via update de simulação
        gameState.update(16)

        // Se abater com sucesso
        if (gameState.score > 500) {
            assertEquals("persistedValue should be updated to new high score", gameState.score, persistedValue)
            assertEquals("gameState hiScore should match new score", gameState.score, gameState.hiScore)
        } else {
            // Garante ultrapassagem de 500
            val target = gameState.formation.invaders.first { it.isAlive }
            // Simula múltiplos abates se necessário
            for (alien in gameState.formation.invaders) {
                if (alien.isAlive) {
                    gameState.player.bullet.spawn(alien.x, alien.y)
                    gameState.update(16)
                    if (gameState.score > 500) break
                }
            }
            assertTrue("Score should have surpassed 500", gameState.score > 500)
            assertEquals(gameState.score, persistedValue)
        }
    }
}
