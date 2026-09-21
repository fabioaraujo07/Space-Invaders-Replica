package com.example.game.entities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.example.game.graphics.RetroSprites

enum class InvaderType(val points: Int, val width: Float, val height: Float) {
    SQUID(30, 8f, 8f),      // Lula (fila superior) - 30 pontos
    CRAB(20, 11f, 8f),      // Caranguejo (filas intermédias) - 20 pontos
    OCTOPUS(10, 12f, 8f)    // Polvo (filas inferiores) - 10 pontos
}

/**
 * Representa um invasor individual dentro da formação.
 * Possui 2 frames de animação que alternam a cada passo.
 */
class Invader(
    val row: Int,
    val col: Int,
    val type: InvaderType,
    var x: Float,
    var y: Float
) {
    var isAlive: Boolean = true
    var isExploding: Boolean = false
    private var explosionTimer: Float = 0f

    val width: Float = type.width
    val height: Float = type.height
    val points: Int = type.points

    private val bounds = RectF()
    private val paint = Paint().apply { isFilterBitmap = false }

    fun updateExplosion(deltaSeconds: Float) {
        if (isExploding) {
            explosionTimer -= deltaSeconds
            if (explosionTimer <= 0f) {
                isExploding = false
            }
        }
    }

    fun triggerExplosion() {
        isAlive = false
        isExploding = true
        explosionTimer = 0.15f // 150ms de exibição da explosão
    }

    fun getBoundingBox(): RectF {
        bounds.set(x, y, x + width, y + height)
        return bounds
    }

    fun render(canvas: Canvas, frame: Int) {
        if (isExploding) {
            val expBmp = RetroSprites.explosionBitmap
            canvas.drawBitmap(expBmp, x - 1f, y, paint)
            return
        }

        if (!isAlive) return

        val bmp: Bitmap = when (type) {
            InvaderType.SQUID -> if (frame == 0) RetroSprites.squidF1 else RetroSprites.squidF2
            InvaderType.CRAB -> if (frame == 0) RetroSprites.crabF1 else RetroSprites.crabF2
            InvaderType.OCTOPUS -> if (frame == 0) RetroSprites.octopusF1 else RetroSprites.octopusF2
        }

        canvas.drawBitmap(bmp, x, y, paint)
    }
}
