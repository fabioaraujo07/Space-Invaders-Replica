package com.example.game.entities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.game.graphics.RetroSprites
import kotlin.math.roundToInt

/**
 * Representa uma das 4 barreiras defensivas verdes clássicas.
 * Possui um Bitmap mutável que sofre destruição progressiva e pixelizada
 * ao ser atingido por projéteis.
 */
class Barrier(
    var x: Float,
    var y: Float
) {
    val width: Float = 22f
    val height: Float = 16f

    private var bitmap: Bitmap = RetroSprites.createMutableBarrierBitmap()
    private val paint = Paint().apply { isFilterBitmap = false }
    private val bounds = RectF()

    var solidPixelCount: Int = 0
        private set

    init {
        countSolidPixels()
    }

    private fun countSolidPixels() {
        var count = 0
        val bmp = bitmap
        for (py in 0 until bmp.height) {
            for (px in 0 until bmp.width) {
                if (bmp.getPixel(px, py) != Color.TRANSPARENT) {
                    count++
                }
            }
        }
        solidPixelCount = count
    }

    fun getBoundingBox(): RectF {
        bounds.set(x, y, x + width, y + height)
        return bounds
    }

    /**
     * Testa colisão com um projétil com precisão de pixel.
     * Se colidir com pixels sólidos da barreira:
     * - Cava uma cratera irregular/pixelizada no bitmap ([CA-10])
     * - Deativa o tiro ([CA-08])
     * - Retorna true
     */
    fun checkBulletCollision(bullet: Bullet): Boolean {
        if (!bullet.isActive || solidPixelCount <= 0) return false

        val bulletBox = bullet.getBoundingBox()
        val barrierBox = getBoundingBox()

        if (!RectF.intersects(bulletBox, barrierBox)) return false

        // Converte a área de sobreposição para coordenadas locais da barreira
        val startX = (bulletBox.left - x).toInt().coerceIn(0, bitmap.width - 1)
        val endX = (bulletBox.right - x).toInt().coerceIn(0, bitmap.width - 1)
        val startY = (bulletBox.top - y).toInt().coerceIn(0, bitmap.height - 1)
        val endY = (bulletBox.bottom - y).toInt().coerceIn(0, bitmap.height - 1)

        // Se o tiro sobe (speedY < 0), a ponta que atinge primeiro é o topo (startY)
        val yProgression = if (bullet.speedY < 0) {
            startY..endY
        } else {
            endY downTo startY
        }

        for (py in yProgression) {
            for (px in startX..endX) {
                if (bitmap.getPixel(px, py) != Color.TRANSPARENT) {
                    // Impacto detetado em pixel sólido: cava cratera
                    erodeCrater(px, py, radius = 2.8f)
                    bullet.deactivate()
                    return true
                }
            }
        }

        return false
    }

    /**
     * Destrói progressivamente uma área circular/pixelizada da barreira.
     */
    fun erodeCrater(localX: Int, localY: Int, radius: Float) {
        val r = radius.toInt() + 1
        val bmp = bitmap
        val rSq = radius * radius

        for (dy in -r..r) {
            val py = localY + dy
            if (py !in 0 until bmp.height) continue

            for (dx in -r..r) {
                val px = localX + dx
                if (px !in 0 until bmp.width) continue

                if (dx * dx + dy * dy <= rSq) {
                    if (bmp.getPixel(px, py) != Color.TRANSPARENT) {
                        bmp.setPixel(px, py, Color.TRANSPARENT)
                        solidPixelCount--
                    }
                }
            }
        }
    }

    /**
     * Erode a barreira se um invasor sobrepor a mesma (quando a formação desce até a base).
     */
    fun erodeByInvader(invaderBox: RectF) {
        if (solidPixelCount <= 0) return
        val barrierBox = getBoundingBox()
        if (!RectF.intersects(invaderBox, barrierBox)) return

        val overlapLeft = (invaderBox.left - x).toInt().coerceIn(0, bitmap.width)
        val overlapRight = (invaderBox.right - x).toInt().coerceIn(0, bitmap.width)
        val overlapTop = (invaderBox.top - y).toInt().coerceIn(0, bitmap.height)
        val overlapBottom = (invaderBox.bottom - y).toInt().coerceIn(0, bitmap.height)

        for (py in overlapTop until overlapBottom) {
            for (px in overlapLeft until overlapRight) {
                if (bitmap.getPixel(px, py) != Color.TRANSPARENT) {
                    bitmap.setPixel(px, py, Color.TRANSPARENT)
                    solidPixelCount--
                }
            }
        }
    }

    fun reset() {
        bitmap = RetroSprites.createMutableBarrierBitmap()
        countSolidPixels()
    }

    fun render(canvas: Canvas) {
        if (solidPixelCount > 0) {
            canvas.drawBitmap(bitmap, x, y, paint)
        }
    }
}
