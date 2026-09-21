package com.example.game

import android.graphics.Canvas
import android.util.Log
import android.view.SurfaceHolder

/**
 * Thread dedicada ao Game Loop.
 * Executa as atualizações lógicas e renderização a aproximadamente 60 quadros por segundo.
 */
class GameThread(
    private val surfaceHolder: SurfaceHolder,
    private val gameView: GameView
) : Thread("SpaceInvadersGameLoop") {

    @Volatile
    private var isRunning: Boolean = false
    @Volatile
    private var isPaused: Boolean = false

    private val targetFps = 60
    private val targetFrameTimeMs = 1000L / targetFps

    fun setRunning(running: Boolean) {
        isRunning = running
    }

    fun setPaused(paused: Boolean) {
        isPaused = paused
    }

    override fun run() {
        var lastTime = System.currentTimeMillis()

        while (isRunning) {
            val currentTime = System.currentTimeMillis()
            val deltaTime = (currentTime - lastTime).coerceAtLeast(1L)
            lastTime = currentTime

            if (!isPaused) {
                // Atualização da lógica
                gameView.update(deltaTime)

                // Renderização via Canvas
                var canvas: Canvas? = null
                try {
                    canvas = surfaceHolder.lockCanvas()
                    if (canvas != null) {
                        synchronized(surfaceHolder) {
                            gameView.render(canvas)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GameThread", "Erro durante renderização no Canvas", e)
                } finally {
                    if (canvas != null) {
                        try {
                            surfaceHolder.unlockCanvasAndPost(canvas)
                        } catch (e: Exception) {
                            Log.e("GameThread", "Erro ao fazer unlockCanvasAndPost", e)
                        }
                    }
                }
            }

            // Controle de cadência para ~60 FPS
            val frameDuration = System.currentTimeMillis() - currentTime
            val sleepTime = targetFrameTimeMs - frameDuration
            if (sleepTime > 0) {
                try {
                    sleep(sleepTime)
                } catch (_: InterruptedException) {
                    // Thread interrompida para encerramento
                }
            }
        }
    }
}
