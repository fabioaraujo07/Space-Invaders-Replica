package com.example

import com.example.game.GameState
import com.example.game.entities.Barrier
import com.example.game.entities.Bullet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BarriersMechanicsTest {

    private lateinit var gameState: GameState

    @Before
    fun setUp() {
        gameState = GameState()
    }

    @Test
    fun `test CA-09 - exactly four barriers exist and are positioned between player and aliens`() {
        // [CA-09] Existirem exatamente quatro barreiras
        assertEquals("Must have exactly 4 barriers", 4, gameState.barriers.size)

        // As barreiras devem estar entre a formação de aliens (y ~36) e o jogador (y = 216)
        for (barrier in gameState.barriers) {
            assertTrue("Barrier should be below aliens", barrier.y > 40f)
            assertTrue("Barrier should be above player", barrier.y < gameState.player.y)
        }

        // As 4 barreiras devem estar distribuídas horizontalmente
        for (i in 0 until 3) {
            assertTrue(
                "Barriers must be ordered from left to right",
                gameState.barriers[i].x < gameState.barriers[i + 1].x
            )
        }
    }

    @Test
    fun `test CA-10 - progressive damage to barrier when hit by bullet`() {
        val barrier = gameState.barriers[0]
        val initialPixels = barrier.solidPixelCount
        assertTrue("Barrier must have initial solid pixels", initialPixels > 100)

        // Cria um projétil do jogador direcionado a uma coluna sólida da barreira
        val bullet = Bullet(speedY = -180f)
        bullet.spawn(barrier.x + 2f, barrier.y + barrier.height - 1f)

        // Testa colisão
        val hit = barrier.checkBulletCollision(bullet)

        assertTrue("Bullet must hit the barrier", hit)
        assertFalse("Bullet must be deactivated upon hitting barrier ([CA-08])", bullet.isActive)

        // [CA-10] A barreira sofreu dano progressivo (pixels foram destruídos, mas não desapareceu por inteiro)
        val pixelsAfterHit1 = barrier.solidPixelCount
        assertTrue("Solid pixels must decrease after impact", pixelsAfterHit1 < initialPixels)
        assertTrue("Barrier must NOT be entirely destroyed by a single hit", pixelsAfterHit1 > 0)

        // Segundo tiro no mesmo local cava ainda mais fundo
        val bullet2 = Bullet(speedY = -180f)
        bullet2.spawn(barrier.x + 2f, barrier.y + barrier.height - 1f)
        barrier.checkBulletCollision(bullet2)

        val pixelsAfterHit2 = barrier.solidPixelCount
        assertTrue("Subsequent hits must continue progressive erosion", pixelsAfterHit2 <= pixelsAfterHit1)
    }

    @Test
    fun `test full simulation collision with barriers via GameState update`() {
        // Posiciona o jogador diretamente abaixo da coluna sólida da primeira barreira
        val barrier = gameState.barriers[0]
        gameState.player.moveToX(barrier.x + 2f)

        // Dispara o canhão
        val fired = gameState.handleFire()
        assertTrue("Player must be able to fire", fired)
        assertTrue("Bullet is active", gameState.player.bullet.isActive)

        val initialBarrierPixels = barrier.solidPixelCount

        // Avança a simulação frame a frame até o projétil atingir a barreira
        var hitOccurred = false
        for (f in 0..60) {
            gameState.update(16) // ~16ms por frame
            if (!gameState.player.bullet.isActive) {
                hitOccurred = true
                break
            }
        }

        assertTrue("Bullet must have impacted the barrier and deactivated", hitOccurred)
        assertTrue(
            "Barrier pixels must have been eroded during GameState simulation",
            barrier.solidPixelCount < initialBarrierPixels
        )
        assertFalse("Bullet is deactivated, allowing new shot", gameState.player.bullet.isActive)
    }
}
