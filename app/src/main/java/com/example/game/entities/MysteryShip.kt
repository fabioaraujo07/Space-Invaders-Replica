package com.example.game.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.game.graphics.RetroSprites
import kotlin.random.Random

/**
 * Representa a Nave-Mãe Vermelha (Mystery Ship / UFO).
 * Surge ocasionalmente no topo da arena, atravessa horizontalmente a velocidade constante
 * e concede uma pontuação bónus (50, 100, 150 ou 300 pontos) quando atingida.
 */
class MysteryShip(
    private val minArenaX: Float = 14f,
    private val maxArenaX: Float = 306f,
    val y: Float = 26f
) {
    val width: Float = 16f
    val height: Float = 7f
    val speed: Float = 45f // Velocidade horizontal moderada

    var x: Float = -20f
    var direction: Int = 1 // +1 para a direita, -1 para a esquerda
    var isActive: Boolean = false
    var isHit: Boolean = false

    private var spawnTimer: Float = 0f
    private val nextSpawnDelay: Float
        get() = Random.nextFloat() * 10f + 15f // A cada 15 a 25 segundos

    private var hitDisplayTimer: Float = 0f
    var lastAwardedScore: Int = 0
        private set

    private val mysteryBitmap = RetroSprites.mysteryShipBitmap
    private val paint = Paint().apply { isFilterBitmap = false }
    private val bounds = RectF()

    private val scoreTextPaint = Paint().apply {
        color = Color.rgb(255, 48, 48) // Vermelho arcade
        textSize = 6f
        typeface = Typeface.MONOSPACE
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    init {
        spawnTimer = nextSpawnDelay
    }

    fun update(deltaSeconds: Float) {
        if (isHit) {
            hitDisplayTimer -= deltaSeconds
            if (hitDisplayTimer <= 0f) {
                isHit = false
                isActive = false
                spawnTimer = nextSpawnDelay
            }
            return
        }

        if (isActive) {
            x += direction * speed * deltaSeconds

            // Se cruzar completamente o ecrã, desativa
            if (direction > 0 && x > maxArenaX + 10f) {
                isActive = false
                spawnTimer = nextSpawnDelay
            } else if (direction < 0 && x + width < minArenaX - 10f) {
                isActive = false
                spawnTimer = nextSpawnDelay
            }
        } else {
            spawnTimer -= deltaSeconds
            if (spawnTimer <= 0f) {
                spawn()
            }
        }
    }

    fun spawn() {
        isActive = true
        isHit = false
        // Escolhe aleatoriamente se surge da esquerda para a direita ou da direita para a esquerda
        if (Random.nextBoolean()) {
            direction = 1
            x = minArenaX - width - 2f
        } else {
            direction = -1
            x = maxArenaX + 2f
        }
    }

    fun getBoundingBox(): RectF {
        bounds.set(x, y, x + width, y + height)
        return bounds
    }

    /**
     * Testa colisão do projétil do jogador contra a Nave-Mãe.
     * Retorna a pontuação bónus (50, 100, 150 ou 300) se atingida, ou 0 caso contrário.
     */
    fun checkBulletCollision(bullet: Bullet): Int {
        if (!isActive || isHit || !bullet.isActive) return 0

        val bulletBox = bullet.getBoundingBox()
        val shipBox = getBoundingBox()

        if (RectF.intersects(bulletBox, shipBox)) {
            bullet.deactivate()
            isHit = true
            hitDisplayTimer = 0.8f // Mostra o valor da pontuação durante 800ms
            // Valores clássicos de bónus: 50, 100, 150 ou 300 pontos
            val bonusScores = intArrayOf(50, 100, 150, 300)
            lastAwardedScore = bonusScores[Random.nextInt(bonusScores.size)]
            return lastAwardedScore
        }

        return 0
    }

    fun render(canvas: Canvas) {
        if (isHit) {
            // Desenha a pontuação em algarismos vermelhos no local da destruição
            canvas.drawText("$lastAwardedScore", x + (width / 2f), y + height, scoreTextPaint)
            return
        }

        if (isActive) {
            canvas.drawBitmap(mysteryBitmap, x, y, paint)
        }
    }
}
