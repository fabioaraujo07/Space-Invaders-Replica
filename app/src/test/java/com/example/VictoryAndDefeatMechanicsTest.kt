package com.example

import com.example.game.GameState
import com.example.game.GameStateMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VictoryAndDefeatMechanicsTest {

    private lateinit var gameState: GameState

    @Before
    fun setUp() {
        gameState = GameState()
    }

    @Test
    fun `test wave cleared when all 55 invaders are destroyed`() {
        // Elimina todos os invasores exceto 1
        gameState.formation.invaders.forEach { it.isAlive = false }
        val lastAlien = gameState.formation.invaders[0]
        lastAlien.isAlive = true
        gameState.formation.recalculateAliveCount()

        // Abate o último invasor com tiro do jogador
        gameState.player.bullet.spawn(lastAlien.x, lastAlien.y)
        gameState.update(16)

        assertEquals("Formation alive count should be 0", 0, gameState.formation.aliveCount)
        assertEquals("Game state mode should be WAVE_CLEARED", GameStateMode.WAVE_CLEARED, gameState.mode)

        // Avança o tempo até a próxima vaga
        gameState.update(1600)
        assertEquals("Wave should increment to 2", 2, gameState.wave)
        assertEquals("Game state mode should return to RUNNING", GameStateMode.RUNNING, gameState.mode)
        assertEquals("New wave must have 55 fresh invaders", 55, gameState.formation.aliveCount)
    }

    @Test
    fun `test game over when lives reach zero`() {
        assertEquals(3, gameState.lives)

        // Simula 3 perdas de vida por tiros alien
        val player = gameState.player
        val bullet = gameState.formation.alienBullets[0]

        // 1ª vida
        bullet.spawn(player.x, player.y)
        gameState.update(16)
        assertEquals(2, gameState.lives)
        gameState.update(1100) // Respawn

        // 2ª vida
        bullet.spawn(player.x, player.y)
        gameState.update(16)
        assertEquals(1, gameState.lives)
        gameState.update(1100) // Respawn

        // 3ª vida
        bullet.spawn(player.x, player.y)
        gameState.update(16)
        assertEquals(0, gameState.lives)
        gameState.update(1100) // Fim do respawn

        assertEquals("Game state mode must be GAME_OVER after losing all lives", GameStateMode.GAME_OVER, gameState.mode)
    }

    @Test
    fun `test instant game over when invaders reach player base level`() {
        // Força um alien vivo a descer até a linha do canhão do jogador (y = 216f)
        val alien = gameState.formation.invaders.last { it.isAlive }
        alien.y = gameState.player.y + 1f

        gameState.update(16)

        assertEquals("Game state mode must be GAME_OVER when invaders invade player level", GameStateMode.GAME_OVER, gameState.mode)
        assertEquals("Lives should be set to 0 on base invasion", 0, gameState.lives)
    }

    @Test
    fun `test game restarts correctly from game over`() {
        // Coloca o jogo em GAME_OVER
        val alien = gameState.formation.invaders.last { it.isAlive }
        alien.y = gameState.player.y + 1f
        gameState.update(16)
        assertEquals(GameStateMode.GAME_OVER, gameState.mode)

        // Ao premir disparo, reinicia o jogo
        val handled = gameState.handleFire()
        assertTrue("Firing in game over should restart the game", handled)
        assertEquals("Mode should be RUNNING", GameStateMode.RUNNING, gameState.mode)
        assertEquals("Lives should be reset to 3", 3, gameState.lives)
        assertEquals("Score should be reset to 0", 0, gameState.score)
        assertEquals("Wave should be reset to 1", 1, gameState.wave)
        assertEquals("All 55 invaders must be alive", 55, gameState.formation.aliveCount)
    }
}
