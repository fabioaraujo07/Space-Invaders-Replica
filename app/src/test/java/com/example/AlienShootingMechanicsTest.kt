package com.example

import com.example.game.GameState
import com.example.game.entities.Bullet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlienShootingMechanicsTest {

    private lateinit var gameState: GameState

    @Before
    fun setUp() {
        gameState = GameState()
    }

    @Test
    fun `test alien bottom shooters selection rule`() {
        // Inicialmente há 11 colunas ativas. Os atiradores devem ser exclusivamente da fila inferior (row 4)
        val shooters = gameState.formation.getBottomShooters()
        assertEquals(11, shooters.size)
        assertTrue(shooters.all { it.row == 4 })

        // Se matarmos o alien da fila 4 na coluna 0, o alien da fila 3 passa a ser o atirador dessa coluna
        val col0Row4 = gameState.formation.invaders.first { it.col == 0 && it.row == 4 }
        col0Row4.isAlive = false

        val newShooters = gameState.formation.getBottomShooters()
        assertEquals(11, newShooters.size)
        val col0Shooter = newShooters.first { it.col == 0 }
        assertEquals(3, col0Shooter.row)
    }

    @Test
    fun `test alien shooting spawns downward bullet`() {
        val bullet = gameState.formation.attemptAlienShot()
        assertNotNull("Alien bullet must be spawned", bullet)
        assertTrue("Alien bullet must be active", bullet!!.isActive)
        assertTrue("Alien bullet must move downward (positive speedY)", bullet.speedY > 0)
    }

    @Test
    fun `test alien bullet damages barrier from above`() {
        val barrier = gameState.barriers[1]
        val initialPixels = barrier.solidPixelCount

        // Posiciona projétil alien logo acima do topo da barreira
        val alienBullet = Bullet(speedY = 110f)
        alienBullet.spawn(barrier.x + barrier.width / 2f, barrier.y - 1f)

        // Simula colisão
        val hit = barrier.checkBulletCollision(alienBullet)
        assertTrue("Alien bullet should hit barrier from above", hit)
        assertFalse("Alien bullet should deactivate after hitting barrier", alienBullet.isActive)
        assertTrue("Barrier solid pixels should decrease after alien hit", barrier.solidPixelCount < initialPixels)
    }

    @Test
    fun `test alien bullet hits player and deducts lives`() {
        val initialLives = gameState.lives
        assertEquals(3, initialLives)

        // Dispara projétil alien diretamente contra o canhão do jogador
        val player = gameState.player
        val alienBullet = gameState.formation.alienBullets[0]
        alienBullet.spawn(player.x + 2f, player.y)

        // Executa atualização do GameState
        gameState.update(16)

        assertEquals("Player should lose a life", 2, gameState.lives)
        assertTrue("Player should enter hit state", gameState.isPlayerHit)
        assertFalse("Alien bullet should be deactivated after hitting player", alienBullet.isActive)
    }
}
