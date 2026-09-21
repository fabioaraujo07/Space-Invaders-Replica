package com.example

import com.example.game.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AudioAndHapticsMechanicsTest {

    private lateinit var gameState: GameState

    @Before
    fun setUp() {
        gameState = GameState()
    }

    @Test
    fun `test alien march triggers rhythmic sound beats`() {
        val playedBeats = mutableListOf<Int>()
        gameState.onPlayMarchBeat = { beat ->
            playedBeats.add(beat)
        }

        // Executa passos na formação de aliens
        for (i in 0 until 5) {
            gameState.formation.update(0.8f) // Cada intervalo é ~0.7s
        }

        assertTrue("March beats should be recorded", playedBeats.isNotEmpty())
        // Os beats devem ciclar em ordem (ex: 0, 1, 2, 3...)
        assertTrue("Beats must be in the range 0..3", playedBeats.all { it in 0..3 })
    }

    @Test
    fun `test player fire triggers shooting audio event`() {
        var shootSoundTriggered = false
        gameState.onPlayPlayerShoot = {
            shootSoundTriggered = true
        }

        val fired = gameState.handleFire()
        assertTrue("Player should successfully fire", fired)
        assertTrue("Shooting sound callback should be invoked", shootSoundTriggered)
    }

    @Test
    fun `test invader destruction triggers explosion audio event`() {
        var invaderExplosionTriggered = false
        gameState.onPlayInvaderExplosion = {
            invaderExplosionTriggered = true
        }

        val targetAlien = gameState.formation.invaders.first { it.isAlive }
        gameState.player.bullet.spawn(targetAlien.x, targetAlien.y)
        gameState.update(16)

        assertTrue("Invader explosion sound should be triggered", invaderExplosionTriggered)
    }

    @Test
    fun `test player hit triggers player explosion audio event`() {
        var playerExplosionTriggered = false
        gameState.onPlayPlayerExplosion = {
            playerExplosionTriggered = true
        }

        val alienBullet = gameState.formation.alienBullets[0]
        alienBullet.spawn(gameState.player.x, gameState.player.y)
        gameState.update(16)

        assertTrue("Player explosion audio event should be triggered", playerExplosionTriggered)
    }
}
