package com.example.game.entities

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.example.game.graphics.RetroSprites

/**
 * Representa o Canhão do Jogador.
 * Movimenta-se exclusivamente no eixo horizontal e gere a regra rígida de disparo único.
 */
class Player(
    var x: Float = 153.5f,
    val y: Float = 216f,
    private val minX: Float = 14f,
    private val maxX: Float = 306f
) {
    val width: Float = 13f
    val height: Float = 8f
    val speed: Float = 110f // Pixels por segundo no espaço lógico

    // Único tiro ativo permitido
    val bullet: Bullet = Bullet()

    private val playerBitmap = RetroSprites.playerBitmap
    private val paint = Paint().apply { isFilterBitmap = false }
    private val bounds = RectF()

    var moveDirection: Float = 0f // -1f (esquerda), 0f (parado), +1f (direita)

    fun setMovement(direction: Float) {
        moveDirection = direction.coerceIn(-1f, 1f)
    }

    /**
     * Move o canhão diretamente para uma coordenada X desejada (usado no arraste por toque),
     * respeitando os limites da arena.
     */
    fun moveToX(targetCenterX: Float) {
        val newX = targetCenterX - (width / 2f)
        x = newX.coerceIn(minX, maxX - width)
    }

    fun update(deltaSeconds: Float) {
        if (moveDirection != 0f) {
            val nextX = x + (moveDirection * speed * deltaSeconds)
            x = nextX.coerceIn(minX, maxX - width)
        }

        // Atualiza o tiro ativo do jogador
        if (bullet.isActive) {
            bullet.update(deltaSeconds, minY = 20f)
        }
    }

    /**
     * REGRA OBRIGATÓRIA:
     * O jogador só pode ter UM tiro ativo de cada vez.
     * Retorna true se disparou com sucesso, ou false se o tiro foi bloqueado por já haver um ativo.
     */
    fun fire(): Boolean {
        if (bullet.isActive) {
            return false // Bloqueado: projétil anterior ainda em voo
        }

        val bulletStartX = x + (width / 2f) - (bullet.width / 2f)
        val bulletStartY = y - bullet.height
        bullet.spawn(bulletStartX, bulletStartY)
        return true
    }

    fun getBoundingBox(): RectF {
        bounds.set(x, y, x + width, y + height)
        return bounds
    }

    fun render(canvas: Canvas) {
        // Renderiza o projétil ativo
        if (bullet.isActive) {
            bullet.render(canvas)
        }

        // Renderiza o canhão do jogador
        canvas.drawBitmap(playerBitmap, x, y, paint)
    }

    fun resetPosition() {
        x = 160f - (width / 2f)
        moveDirection = 0f
        bullet.deactivate()
    }
}
