package com.example.game

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.game.audio.RetroSoundManager

/**
 * SurfaceView principal do jogo, responsável por integrar o SurfaceHolder com a GameThread,
 * o ciclo de vida do Android e a captura de eventos de toque e teclado.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    val gameState = GameState()
    val gameRenderer = GameRenderer()
    val soundManager = RetroSoundManager(context)
    private var gameThread: GameThread? = null

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()

        // Vincula eventos de áudio e hápticos ao sintetizador de 1978
        gameState.onPlayMarchBeat = { beat -> soundManager.playMarchBeat(beat) }
        gameState.onPlayPlayerShoot = { soundManager.playPlayerShoot() }
        gameState.onPlayInvaderExplosion = { soundManager.playInvaderExplosion() }
        gameState.onPlayPlayerExplosion = { soundManager.playPlayerExplosion() }
        gameState.onPlayUfoSound = { soundManager.playUfoSound() }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        startThread()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        gameRenderer.onSurfaceChanged(width, height, gameState.logicalWidth, gameState.logicalHeight)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopThread()
    }

    private fun startThread() {
        if (gameThread == null || !gameThread!!.isAlive) {
            gameThread = GameThread(holder, this).apply {
                setRunning(true)
                start()
            }
        }
    }

    private fun stopThread() {
        var retry = true
        gameThread?.setRunning(false)
        while (retry) {
            try {
                gameThread?.join(500)
                retry = false
            } catch (_: InterruptedException) {
                // Tenta novamente até finalizar com segurança
            }
        }
        gameThread = null
    }

    fun pause() {
        gameThread?.setPaused(true)
        gameState.pause()
    }

    fun resume() {
        gameThread?.setPaused(false)
        gameState.resume()
    }

    fun update(deltaTimeMs: Long) {
        gameState.update(deltaTimeMs)
    }

    fun render(canvas: Canvas) {
        gameRenderer.render(canvas, gameState, width, height)
    }

    /**
     * Tratamento de controlos táteis (Multi-touch):
     * - Polegar esquerdo: botões virtuais ou deslize horizontal
     * - Polegar direito: botão de disparo ou toque no lado direito
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val actionMasked = event.actionMasked
        val pointerIndex = event.actionIndex

        var movingLeft = false
        var movingRight = false

        // Deteta novo toque (disparo pontual)
        if (actionMasked == MotionEvent.ACTION_DOWN || actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            val px = gameRenderer.screenToLogicalX(event.getX(pointerIndex))
            val py = gameRenderer.screenToLogicalY(event.getY(pointerIndex))

            // Toque no botão de disparo da base OU metade direita inferior do ecrã
            if ((px in 230f..320f && py in 220f..240f) || (px > 240f && py > 140f)) {
                gameState.handleFire()
            }
        }

        // Avalia todos os dedos em contacto ativo com o ecrã
        if (actionMasked != MotionEvent.ACTION_UP && actionMasked != MotionEvent.ACTION_CANCEL) {
            for (i in 0 until event.pointerCount) {
                if (actionMasked == MotionEvent.ACTION_POINTER_UP && i == pointerIndex) {
                    continue // Dedo foi levantado
                }

                val lx = gameRenderer.screenToLogicalX(event.getX(i))
                val ly = gameRenderer.screenToLogicalY(event.getY(i))

                // Área de botão Esquerda (com margem de tolerância para o polegar)
                if (lx in 0f..44f && ly >= 220f) {
                    movingLeft = true
                }
                // Área de botão Direita (com margem de tolerância para o polegar)
                else if (lx in 45f..90f && ly >= 220f) {
                    movingRight = true
                }
                // Arraste horizontal direto na metade inferior do campo de batalha
                else if (ly in 120f..225f && lx in 20f..300f) {
                    gameState.handleDragTo(lx)
                }
            }
        }

        // Aplica direção do movimento
        val dir = when {
            movingLeft && !movingRight -> -1f
            movingRight && !movingLeft -> 1f
            else -> 0f
        }
        gameState.handleMove(dir)

        return true
    }

    /**
     * Suporte a teclado físico ou comandos de emulador
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> {
                gameState.handleMove(-1f)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> {
                gameState.handleMove(1f)
                true
            }
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> {
                gameState.handleFire()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A,
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> {
                gameState.handleMove(0f)
                true
            }
            else -> super.onKeyUp(keyCode, event)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        soundManager.release()
    }
}
