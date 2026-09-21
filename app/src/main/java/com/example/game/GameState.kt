package com.example.game

import com.example.game.entities.Barrier
import com.example.game.entities.InvaderFormation
import com.example.game.entities.MysteryShip
import com.example.game.entities.Player

enum class GameStateMode {
    SPLASH_SCREEN,
    INITIALIZING,
    READY,
    RUNNING,
    PAUSED,
    WAVE_CLEARED,
    GAME_OVER
}

/**
 * Representa o estado do jogo e as propriedades lógicas da simulação.
 * Mantém a resolução lógica fixa em 320x240 para escala independente de dispositivo.
 */
class GameState(
    initialHiScore: Int = 0,
    initialMode: GameStateMode = GameStateMode.READY,
    private val onHiScoreChanged: ((Int) -> Unit)? = null
) {
    val logicalWidth: Int = 320
    val logicalHeight: Int = 240

    val player = Player()
    val formation = InvaderFormation()
    val mysteryShip = MysteryShip()

    // EXATAMENTE 4 BARREIRAS VERDES ([CA-09])
    val barriers: List<Barrier> = listOf(
        Barrier(x = 46f, y = 182f),
        Barrier(x = 114f, y = 182f),
        Barrier(x = 182f, y = 182f),
        Barrier(x = 250f, y = 182f)
    )

    var score: Int = 0
        private set

    var hiScore: Int = initialHiScore
        private set

    var wave: Int = 1
        private set

    var lives: Int = 3
        private set

    var isPlayerHit: Boolean = false
        private set

    private var respawnTimer: Float = 0f
    private var waveClearTimer: Float = 0f
    var splashTimer: Float = 2.8f
        private set

    var mode: GameStateMode = initialMode
        private set

    var frameCount: Long = 0L
        private set

    // Callbacks de Áudio e Hápticos Arcade
    var onPlayMarchBeat: ((Int) -> Unit)? = null
    var onPlayPlayerShoot: (() -> Unit)? = null
    var onPlayInvaderExplosion: (() -> Unit)? = null
    var onPlayPlayerExplosion: (() -> Unit)? = null
    var onPlayUfoSound: (() -> Unit)? = null

    var currentFps: Int = 60
        private set
    private var fpsCounter: Int = 0
    private var fpsTimerAccumulator: Long = 0L

    init {
        formation.onStepMade = { beat ->
            onPlayMarchBeat?.invoke(beat)
        }
    }

    fun update(deltaTimeMs: Long) {
        frameCount++
        fpsCounter++
        fpsTimerAccumulator += deltaTimeMs
        if (fpsTimerAccumulator >= 1000L) {
            currentFps = fpsCounter
            fpsCounter = 0
            fpsTimerAccumulator = 0L
        }

        if (mode == GameStateMode.SPLASH_SCREEN) {
            val deltaSec = (deltaTimeMs / 1000f).coerceIn(0.001f, 10.0f)
            splashTimer -= deltaSec
            if (splashTimer <= 0f) {
                mode = GameStateMode.RUNNING
            }
            return
        }

        if (mode == GameStateMode.READY) {
            mode = GameStateMode.RUNNING
        }

        if (mode == GameStateMode.RUNNING) {
            val deltaSeconds = (deltaTimeMs / 1000f).coerceIn(0.001f, 2.0f)

            if (isPlayerHit) {
                respawnTimer -= deltaSeconds
                if (respawnTimer <= 0f) {
                    isPlayerHit = false
                    player.resetPosition()
                    if (lives <= 0) {
                        mode = GameStateMode.GAME_OVER
                    }
                }
            } else {
                player.update(deltaSeconds)
            }

            formation.update(deltaSeconds)
            mysteryShip.update(deltaSeconds)

            // Colisão do projétil do jogador com as 4 barreiras ([CA-10])
            if (player.bullet.isActive) {
                for (b in barriers) {
                    if (b.checkBulletCollision(player.bullet)) {
                        break // Projétil colidiu com a barreira, sofreu dano e desativou
                    }
                }
            }

            // Colisão dos projéteis dos invasores contra as 4 barreiras (danos descendentes)
            for (ab in formation.alienBullets) {
                if (ab.isActive) {
                    for (b in barriers) {
                        if (b.checkBulletCollision(ab)) {
                            break // Projétil alien colidiu com o topo/corpo da barreira
                        }
                    }
                }
            }

            // Colisão entre projétil do jogador e projéteis dos invasores (aniquilação mútua arcade)
            if (player.bullet.isActive) {
                val pBox = player.bullet.getBoundingBox()
                for (ab in formation.alienBullets) {
                    if (ab.isActive && android.graphics.RectF.intersects(pBox, ab.getBoundingBox())) {
                        player.bullet.deactivate()
                        ab.deactivate()
                        break
                    }
                }
            }

            // Colisão entre projétil do jogador e a Nave-Mãe (Mystery Ship)
            if (player.bullet.isActive && mysteryShip.isActive) {
                val bonus = mysteryShip.checkBulletCollision(player.bullet)
                if (bonus > 0) {
                    score += bonus
                    if (score > hiScore) {
                        hiScore = score
                        onHiScoreChanged?.invoke(hiScore)
                    }
                    onPlayInvaderExplosion?.invoke()
                }
            }

            // Colisão entre projétil do jogador e invasores
            if (player.bullet.isActive) {
                val hitInvader = formation.checkBulletCollision(player.bullet)
                if (hitInvader != null) {
                    score += hitInvader.points
                    if (score > hiScore) {
                        hiScore = score
                        onHiScoreChanged?.invoke(hiScore)
                    }
                    onPlayInvaderExplosion?.invoke()

                    // Condição de Vitória da Vaga: todos os 55 aliens eliminados
                    if (formation.aliveCount == 0) {
                        mode = GameStateMode.WAVE_CLEARED
                        waveClearTimer = 1.5f // Breve pausa de celebração antes da próxima vaga
                    }
                }
            }

            // Colisão de projéteis dos invasores contra o canhão do jogador
            if (!isPlayerHit) {
                val playerBox = player.getBoundingBox()
                for (ab in formation.alienBullets) {
                    if (ab.isActive && android.graphics.RectF.intersects(playerBox, ab.getBoundingBox())) {
                        ab.deactivate()
                        triggerPlayerHit()
                        break
                    }
                }
            }

            // Condição de Derrota Crítica por Invasão: os aliens atingem o canhão / solo
            val lowestInvaderY = formation.getLowestY()
            if (lowestInvaderY >= player.y) {
                // Invasão consumada: Game Over instantâneo!
                lives = 0
                mode = GameStateMode.GAME_OVER
            } else if (lowestInvaderY >= 180f) {
                // Erosão das barreiras caso desçam até a sua linha
                for (invader in formation.invaders) {
                    if (invader.isAlive && invader.y + invader.height >= 182f) {
                        val invBox = invader.getBoundingBox()
                        for (b in barriers) {
                            b.erodeByInvader(invBox)
                        }
                    }
                }
            }
        } else if (mode == GameStateMode.WAVE_CLEARED) {
            val deltaSeconds = (deltaTimeMs / 1000f).coerceIn(0.001f, 2.0f)
            waveClearTimer -= deltaSeconds
            if (waveClearTimer <= 0f) {
                advanceToNextWave()
            }
        }
    }

    /**
     * Avança para a vaga seguinte:
     * Aumenta o contador de vaga, reinicia a formação de aliens ligeiramente mais baixa
     * e repara parcialmente as barreiras.
     */
    fun advanceToNextWave() {
        wave++
        // As vagas subsequentes começam ligeiramente mais abaixo (limite clássico arcade até y=60)
        val newStartY = (36f + ((wave - 1) * 6f)).coerceAtMost(60f)
        formation.resetFormation(startY = newStartY)
        player.resetPosition()
        barriers.forEach { it.reset() }
        mode = GameStateMode.RUNNING
    }

    /**
     * Reinicia totalmente o jogo após Game Over.
     */
    fun restartGame() {
        score = 0
        lives = 3
        wave = 1
        isPlayerHit = false
        formation.resetFormation(startY = 36f)
        player.resetPosition()
        barriers.forEach { it.reset() }
        mode = GameStateMode.RUNNING
    }

    private fun triggerPlayerHit() {
        lives--
        isPlayerHit = true
        respawnTimer = 1.0f // 1 segundo de pausa e animação de impacto
        onPlayPlayerExplosion?.invoke()
        if (lives <= 0) {
            mode = GameStateMode.GAME_OVER
        }
    }

    fun handleMove(direction: Float) {
        player.setMovement(direction)
    }

    fun handleDragTo(logicalX: Float) {
        player.moveToX(logicalX)
    }

    fun handleFire(): Boolean {
        if (mode == GameStateMode.SPLASH_SCREEN) {
            mode = GameStateMode.RUNNING
            return true
        }
        if (mode == GameStateMode.GAME_OVER) {
            restartGame()
            return true
        }
        if (mode != GameStateMode.RUNNING && mode != GameStateMode.READY) return false
        if (mode == GameStateMode.READY) mode = GameStateMode.RUNNING
        val fired = player.fire()
        if (fired) {
            onPlayPlayerShoot?.invoke()
        }
        return fired
    }

    fun pause() {
        if (mode == GameStateMode.RUNNING) {
            mode = GameStateMode.PAUSED
        }
    }

    fun resume() {
        if (mode == GameStateMode.PAUSED) {
            mode = GameStateMode.RUNNING
        }
    }
}
