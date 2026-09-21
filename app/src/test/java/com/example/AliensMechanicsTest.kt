package com.example

import com.example.game.entities.InvaderFormation
import com.example.game.entities.InvaderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AliensMechanicsTest {

    private lateinit var formation: InvaderFormation

    @Before
    fun setUp() {
        formation = InvaderFormation()
    }

    @Test
    fun `test CA-11 - three types of aliens with correct row distribution`() {
        assertEquals(55, formation.totalInvaders)

        // Fila 0 deve ser SQUID (Lula)
        val row0 = formation.invaders.filter { it.row == 0 }
        assertEquals(11, row0.size)
        assertTrue(row0.all { it.type == InvaderType.SQUID && it.points == 30 })

        // Filas 1 e 2 devem ser CRAB (Caranguejo)
        val row1And2 = formation.invaders.filter { it.row in 1..2 }
        assertEquals(22, row1And2.size)
        assertTrue(row1And2.all { it.type == InvaderType.CRAB && it.points == 20 })

        // Filas 3 e 4 devem ser OCTOPUS (Polvo)
        val row3And4 = formation.invaders.filter { it.row in 3..4 }
        assertEquals(22, row3And4.size)
        assertTrue(row3And4.all { it.type == InvaderType.OCTOPUS && it.points == 10 })
    }

    @Test
    fun `test CA-12 and CA-13 - horizontal movement and two-frame animation alternation`() {
        val initialX = formation.invaders.first().x
        val initialFrame = formation.currentFrame

        // Simula tempo suficiente para avançar 1 passo (> 0.70s)
        formation.update(0.75f)

        val newX = formation.invaders.first().x
        val newFrame = formation.currentFrame

        // Moveu-se horizontalmente para a direita
        assertTrue("Formation must move horizontally", newX > initialX)

        // Frame de animação deve ter alternado (0 <-> 1)
        assertNotEquals(initialFrame, newFrame)

        // Próximo passo deve alternar de volta
        formation.update(0.75f)
        assertEquals(initialFrame, formation.currentFrame)
    }

    @Test
    fun `test CA-14 - formation drops and inverts direction at edge`() {
        val initialY = formation.invaders.first().y

        // Posiciona a formação próxima da extremidade direita
        for (inv in formation.invaders) {
            inv.x += 100f
        }

        // Executa passos até forçar o impacto na margem direita
        var stepped = 0
        while (formation.direction > 0 && stepped < 50) {
            formation.update(0.75f)
            stepped++
        }

        // Deve ter descido e invertido para a esquerda (direction = -1)
        assertEquals(-1, formation.direction)
        assertTrue("Formation must drop in Y when hitting edge", formation.invaders.first().y > initialY)
    }

    @Test
    fun `test CA-15 - speed acceleration when aliens are destroyed`() {
        assertEquals(55, formation.aliveCount)

        // Destrói 50 aliens, restando apenas 5
        for (i in 0 until 50) {
            formation.invaders[i].isAlive = false
        }
        val reflectionField = InvaderFormation::class.java.getDeclaredField("aliveCount")
        reflectionField.isAccessible = true
        reflectionField.setInt(formation, 5)

        // Com apenas 5 aliens, um passo deve ocorrer muito mais rápido que com 55 aliens
        val initialFrame = formation.currentFrame
        // 0.15s é insuficiente para mover com 55 aliens (~0.7s), mas com 5 aliens deve dar um passo!
        formation.update(0.15f)
        assertNotEquals("Formation should have moved much faster with only 5 aliens", initialFrame, formation.currentFrame)
    }
}
