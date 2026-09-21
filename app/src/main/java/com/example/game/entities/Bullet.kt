package com.example.game.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * Representa um projétil no jogo.
 * Pode ser disparado pelo jogador (move-se para cima).
 */
class Bullet(
    var x: Float = 0f,
    var y: Float = 0f,
    val speedY: Float = -160f // Pixels por segundo (para cima)
) {
    val width: Float = 1.5f
    val height: Float = 5f
    var isActive: Boolean = false

    private val bulletPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = false
    }

    private val bounds = RectF()

    fun spawn(startX: Float, startY: Float) {
        x = startX
        y = startY
        isActive = true
    }

    fun update(deltaSeconds: Float, minY: Float = 10f, maxY: Float = 236f) {
        if (!isActive) return

        y += speedY * deltaSeconds

        // Se sair do campo de jogo superior ou inferior, é desativado
        if (speedY < 0 && y + height < minY) {
            deactivate()
        } else if (speedY > 0 && y > maxY) {
            deactivate()
        }
    }

    fun deactivate() {
        isActive = false
    }

    fun getBoundingBox(): RectF {
        bounds.set(x, y, x + width, y + height)
        return bounds
    }

    fun render(canvas: Canvas) {
        if (!isActive) return
        canvas.drawRect(x, y, x + width, y + height, bulletPaint)
    }
}
