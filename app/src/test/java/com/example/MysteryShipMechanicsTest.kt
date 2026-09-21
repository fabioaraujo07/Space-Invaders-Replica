package com.example

import com.example.game.GameState
import com.example.game.entities.Bullet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MysteryShipMechanicsTest {

    private lateinit var gameState: GameState

    @Before
    fun setUp() {
        gameState = GameState()
    }

    @Test
    fun `test mystery ship spawns and traverses horizontally`() {
        val ship = gameState.mysteryShip
        assertFalse("Initially mystery ship should not be active", ship.isActive)

        ship.spawn()
        assertTrue("Mystery ship should be active upon spawn", ship.isActive)
        val initialX = ship.x

        // Atualiza a posição durante 0.5s
        ship.update(0.5f)
        assertNotEquals("Ship should move horizontally", initialX, ship.x)
    }

    @Test
    fun `test bullet collision awards bonus points from classical range`() {
        val ship = gameState.mysteryShip
        ship.spawn()
        ship.x = 100f

        val bullet = Bullet(speedY = -160f)
        bullet.spawn(ship.x + 2f, ship.y + ship.height - 1f)

        val awardedPoints = ship.checkBulletCollision(bullet)

        // Deve conceder um dos valores clássicos de bónus arcade
        val validBonusValues = setOf(50, 100, 150, 300)
        assertTrue("Awarded points must be 50, 100, 150 or 300", validBonusValues.contains(awardedPoints))
        assertTrue("Ship should enter hit state", ship.isHit)
        assertFalse("Bullet must be deactivated upon hitting mystery ship", bullet.isActive)
    }

    @Test
    fun `test full simulation adds mystery ship points to score`() {
        val ship = gameState.mysteryShip
        ship.spawn()
        ship.x = 150f
        ship.direction = 0 // Mantém fixa durante o teste de colisão

        // Remove aliens da coluna central para o projétil passar limpo até ao topo
        gameState.formation.invaders.forEach { inv ->
            if (inv.col in 4..6) inv.isAlive = false
        }

        // Jogador dispara diretamente alinhado com a Mystery Ship
        gameState.player.moveToX(ship.x + ship.width / 2f)
        gameState.handleFire()

        val initialScore = gameState.score

        // Itera frames até o projétil atingir a nave no topo (y=216 descendo até y=26 leva ~1.2s a 160px/s)
        var hitOccurred = false
        for (i in 0 until 120) {
            gameState.update(16)
            if (gameState.score > initialScore) {
                hitOccurred = true
                break
            }
        }

        assertTrue("Bullet must reach and destroy mystery ship, increasing score", hitOccurred)
        assertTrue("Score increase must be >= 50", gameState.score - initialScore >= 50)
    }
}
