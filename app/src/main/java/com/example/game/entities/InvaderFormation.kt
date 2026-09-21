package com.example.game.entities

import android.graphics.Canvas
import android.graphics.RectF

/**
 * Gere a grelha de 55 invasores (5 filas x 11 colunas).
 * Controla o movimento em zigue-zague, descida nas margens,
 * alternância de frames e aceleração progressiva.
 */
class InvaderFormation(
    val rows: Int = 5,
    val cols: Int = 11,
    private val minArenaX: Float = 14f,
    private val maxArenaX: Float = 306f
) {
    val totalInvaders: Int = rows * cols
    val invaders: MutableList<Invader> = ArrayList(totalInvaders)

    var direction: Int = 1 // +1 para a direita, -1 para a esquerda
    var currentFrame: Int = 0 // 0 ou 1 (dois frames de animação)

    // Cadência e velocidade
    private val maxStepInterval: Float = 0.70f // Segundos por passo quando todos estão vivos
    private val minStepInterval: Float = 0.045f // Segundos por passo com 1 invasor restante
    private val stepX: Float = 3.2f // Deslocamento horizontal por passo
    private val dropY: Float = 8f // Descida ao atingir uma extremidade

    private var stepTimer: Float = 0f
    var aliveCount: Int = totalInvaders

    fun recalculateAliveCount(): Int {
        aliveCount = invaders.count { it.isAlive }
        return aliveCount
    }

    // Beat index para o ritmo sonoro de 4 notas descendentes
    var soundBeatIndex: Int = 0
        private set
    var onStepMade: ((Int) -> Unit)? = null

    // Pool de projéteis dos invasores (máximo clássico de 3 tiros em simultâneo no ecrã)
    val alienBullets: List<Bullet> = listOf(
        Bullet(speedY = 110f),
        Bullet(speedY = 110f),
        Bullet(speedY = 110f)
    )

    private var shootTimer: Float = 0f
    private val shootInterval: Float = 0.90f // Cadência de tiro dos invasores

    init {
        resetFormation()
    }

    fun resetFormation(startY: Float = 36f) {
        invaders.clear()
        direction = 1
        currentFrame = 0
        stepTimer = 0f
        soundBeatIndex = 0

        val colSpacing = 16f
        val rowSpacing = 13f
        val startX = 30f

        for (r in 0 until rows) {
            val type = when (r) {
                0 -> InvaderType.SQUID       // Fila superior (30 pts)
                1, 2 -> InvaderType.CRAB     // Filas intermédias (20 pts)
                else -> InvaderType.OCTOPUS  // Filas inferiores (10 pts)
            }

            for (c in 0 until cols) {
                // Centraliza os sprites de larguras diferentes dentro da coluna
                val offsetX = (12f - type.width) / 2f
                val x = startX + (c * colSpacing) + offsetX
                val y = startY + (r * rowSpacing)
                invaders.add(Invader(row = r, col = c, type = type, x = x, y = y))
            }
        }
        aliveCount = invaders.size
    }

    fun update(deltaSeconds: Float) {
        // Atualiza temporizadores de explosão
        for (i in 0 until invaders.size) {
            val inv = invaders[i]
            if (inv.isExploding) {
                inv.updateExplosion(deltaSeconds)
            }
        }

        // Atualiza projéteis dos invasores (movimento para baixo)
        for (i in 0 until alienBullets.size) {
            val b = alienBullets[i]
            if (b.isActive) {
                b.update(deltaSeconds, minY = 10f, maxY = 236f)
            }
        }

        if (aliveCount == 0) return

        // Gere cadência e disparo dos invasores
        shootTimer += deltaSeconds
        if (shootTimer >= shootInterval) {
            shootTimer = 0f
            attemptAlienShot()
        }

        // Calcula o intervalo dinâmico de passo baseado no número de vivos (Aceleração)
        val ratio = (aliveCount.toFloat() / totalInvaders.toFloat()).coerceIn(0f, 1f)
        val currentStepInterval = minStepInterval + (ratio * (maxStepInterval - minStepInterval))

        stepTimer += deltaSeconds
        if (stepTimer >= currentStepInterval) {
            stepTimer = 0f
            performStep()
        }
    }

    /**
     * Identifica os aliens situados na parte mais inferior de cada coluna ativa
     * e seleciona um para disparar um projétil descendente.
     */
    fun attemptAlienShot(): Bullet? {
        val availableBullet = alienBullets.firstOrNull { !it.isActive } ?: return null

        val bottomShooters = getBottomShooters()
        if (bottomShooters.isEmpty()) return null

        val shooter = bottomShooters.random()
        val spawnX = shooter.x + (shooter.width / 2f) - (availableBullet.width / 2f)
        val spawnY = shooter.y + shooter.height + 1f
        availableBullet.spawn(spawnX, spawnY)
        return availableBullet
    }

    /**
     * Retorna a lista de aliens da base de cada coluna ativa (os únicos autorizados a disparar).
     */
    fun getBottomShooters(): List<Invader> {
        val bottomByCol = HashMap<Int, Invader>()
        for (i in 0 until invaders.size) {
            val inv = invaders[i]
            if (inv.isAlive) {
                val current = bottomByCol[inv.col]
                if (current == null || inv.row > current.row) {
                    bottomByCol[inv.col] = inv
                }
            }
        }
        return bottomByCol.values.toList()
    }

    private fun performStep() {
        // Alterna o frame de animação (Dois frames obrigatórios [CA-12])
        currentFrame = 1 - currentFrame

        // Avança o beat sonoro (0, 1, 2, 3)
        soundBeatIndex = (soundBeatIndex + 1) % 4
        onStepMade?.invoke(soundBeatIndex)

        // 1. Verifica se algum alien vivo atinge as extremidades
        var needDropAndReverse = false

        if (direction > 0) {
            // Movendo para a direita: procura o alien mais à direita
            for (i in 0 until invaders.size) {
                val inv = invaders[i]
                if (inv.isAlive && (inv.x + inv.width + stepX >= maxArenaX)) {
                    needDropAndReverse = true
                    break
                }
            }
        } else {
            // Movendo para a esquerda: procura o alien mais à esquerda
            for (i in 0 until invaders.size) {
                val inv = invaders[i]
                if (inv.isAlive && (inv.x - stepX <= minArenaX)) {
                    needDropAndReverse = true
                    break
                }
            }
        }

        // 2. Aplica o movimento a todos os invasores
        if (needDropAndReverse) {
            // Desce e inverte direção ([CA-14])
            for (i in 0 until invaders.size) {
                val inv = invaders[i]
                inv.y += dropY
            }
            direction = -direction
        } else {
            // Deslocamento horizontal ([CA-13])
            val dx = direction * stepX
            for (i in 0 until invaders.size) {
                val inv = invaders[i]
                inv.x += dx
            }
        }
    }

    /**
     * Testa colisão do projétil do jogador contra a formação de aliens.
     * Retorna o Invader atingido ou null se não houver colisão.
     */
    fun checkBulletCollision(bullet: Bullet): Invader? {
        if (!bullet.isActive) return null

        val bulletBox = bullet.getBoundingBox()

        // Itera de baixo para cima para priorizar aliens mais próximos do jogador
        for (i in invaders.indices.reversed()) {
            val invader = invaders[i]
            if (invader.isAlive) {
                val invaderBox = invader.getBoundingBox()
                if (RectF.intersects(bulletBox, invaderBox)) {
                    invader.triggerExplosion()
                    bullet.deactivate()
                    aliveCount--
                    return invader
                }
            }
        }
        return null
    }

    /**
     * Retorna a coordenada Y mais baixa de qualquer alien ainda vivo.
     * Usado para avaliar a condição de invasão da base.
     */
    fun getLowestY(): Float {
        var lowest = 0f
        for (i in 0 until invaders.size) {
            val inv = invaders[i]
            if (inv.isAlive) {
                val bottom = inv.y + inv.height
                if (bottom > lowest) {
                    lowest = bottom
                }
            }
        }
        return lowest
    }

    fun render(canvas: Canvas) {
        for (i in 0 until invaders.size) {
            val inv = invaders[i]
            if (inv.isAlive || inv.isExploding) {
                inv.render(canvas, currentFrame)
            }
        }

        // Renderiza tiros ativos dos invasores
        for (i in 0 until alienBullets.size) {
            val b = alienBullets[i]
            if (b.isActive) {
                b.render(canvas)
            }
        }
    }
}
