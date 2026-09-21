package com.example

import com.example.game.entities.Player
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
class PlayerMechanicsTest {

    private lateinit var player: Player

    @Before
    fun setUp() {
        player = Player()
    }

    @Test
    fun `test CA-04 - player is positioned at the bottom`() {
        assertEquals(216f, player.y, 0.01f)
    }

    @Test
    fun `test CA-05 and CA-06 - player horizontal movement within limits`() {
        // Move to far left
        player.moveToX(-100f)
        assertTrue(player.x >= 14f)

        // Move to far right
        player.moveToX(500f)
        assertTrue(player.x + player.width <= 306f)
    }

    @Test
    fun `test CA-07 and CA-08 - single active bullet rule`() {
        assertFalse(player.bullet.isActive)

        // First shot succeeds
        val firstShot = player.fire()
        assertTrue(firstShot)
        assertTrue(player.bullet.isActive)

        // Second shot while first is active MUST fail
        val secondShot = player.fire()
        assertFalse(secondShot)
        assertTrue(player.bullet.isActive)

        // Simulate bullet moving off screen and deactivating
        player.bullet.y = -10f
        player.bullet.update(0.1f, minY = 20f)
        assertFalse(player.bullet.isActive)

        // Now new shot MUST be permitted
        val thirdShot = player.fire()
        assertTrue(thirdShot)
        assertTrue(player.bullet.isActive)
    }
}
