package com.example.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.game.graphics.RetroSprites
import kotlin.math.min

/**
 * Renderizador responsável por projetar a área lógica de 320x240 para a resolução física
 * do dispositivo, preservando a proporção de ecrã (Letterboxing / Pillarboxing).
 */
class GameRenderer {

    var scale: Float = 1.0f
        private set
    var offsetX: Float = 0f
        private set
    var offsetY: Float = 0f
        private set

    val logicalRect = RectF(0f, 0f, 320f, 240f)

    private val backgroundPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint().apply {
        color = Color.rgb(0, 255, 68) // Verde arcade retro
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private val titlePaint = Paint().apply {
        color = Color.rgb(255, 255, 255)
        typeface = Typeface.MONOSPACE
        textSize = 14f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = false // Look retro pixelizado
    }

    private val subtitlePaint = Paint().apply {
        color = Color.rgb(0, 255, 68)
        typeface = Typeface.MONOSPACE
        textSize = 9f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = false
    }

    private val infoPaint = Paint().apply {
        color = Color.rgb(180, 180, 180)
        typeface = Typeface.MONOSPACE
        textSize = 8f
        textAlign = Paint.Align.CENTER
        isAntiAlias = false
    }

    fun onSurfaceChanged(width: Int, height: Int, logicalWidth: Int, logicalHeight: Int) {
        val scaleX = width.toFloat() / logicalWidth.toFloat()
        val scaleY = height.toFloat() / logicalHeight.toFloat()
        scale = min(scaleX, scaleY)
        offsetX = (width - logicalWidth * scale) / 2f
        offsetY = (height - logicalHeight * scale) / 2f
    }

    val btnLeftRect = RectF(6f, 227f, 42f, 238f)
    val btnRightRect = RectF(46f, 227f, 82f, 238f)
    val btnFireRect = RectF(244f, 227f, 314f, 238f)

    private val buttonBgPaint = Paint().apply {
        color = Color.rgb(20, 50, 20)
        style = Paint.Style.FILL
    }

    private val buttonBorderPaint = Paint().apply {
        color = Color.rgb(0, 255, 68)
        style = Paint.Style.STROKE
        strokeWidth = 0.8f
    }

    private val buttonTextPaint = Paint().apply {
        color = Color.rgb(0, 255, 68)
        typeface = Typeface.MONOSPACE
        textSize = 6.5f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = false
    }

    private val hudPaint = Paint().apply {
        color = Color.rgb(255, 255, 255)
        typeface = Typeface.MONOSPACE
        textSize = 9f
        textAlign = Paint.Align.LEFT
        isFakeBoldText = true
        isAntiAlias = false
    }

    private val hudScorePaint = Paint().apply {
        color = Color.rgb(0, 255, 68)
        typeface = Typeface.MONOSPACE
        textSize = 9f
        textAlign = Paint.Align.LEFT
        isFakeBoldText = true
        isAntiAlias = false
    }

    private val statusActivePaint = Paint().apply {
        color = Color.rgb(255, 80, 80)
        typeface = Typeface.MONOSPACE
        textSize = 6.5f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = false
    }

    private val statusReadyPaint = Paint().apply {
        color = Color.rgb(0, 255, 68)
        typeface = Typeface.MONOSPACE
        textSize = 6.5f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = false
    }

    fun render(canvas: Canvas, gameState: GameState, viewWidth: Int, viewHeight: Int) {
        // 1. Limpa todo o ecrã físico em preto absoluto
        canvas.drawRect(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat(), backgroundPaint)

        // 2. Transforma o Canvas para coordenadas lógicas 320x240
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        // 3. Delimita o viewport lógico (320x240)
        canvas.clipRect(0f, 0f, gameState.logicalWidth.toFloat(), gameState.logicalHeight.toFloat())

        // Fundo da área lógica
        canvas.drawRect(0f, 0f, gameState.logicalWidth.toFloat(), gameState.logicalHeight.toFloat(), backgroundPaint)

        // Borda verde da arena arcade
        canvas.drawRect(2f, 2f, gameState.logicalWidth - 2f, gameState.logicalHeight - 2f, borderPaint)

        // Linha divisória clássica arcade na base
        canvas.drawLine(4f, 225f, gameState.logicalWidth - 4f, 225f, borderPaint)

        // --- HUD SUPERIOR RETRO ---
        canvas.drawText("SCORE<1>", 14f, 14f, hudPaint)
        val scoreStr = "%04d".format(gameState.score)
        canvas.drawText(scoreStr, 14f, 24f, hudScorePaint)

        canvas.drawText("HI-SCORE", 125f, 14f, hudPaint)
        val hiScoreStr = "%04d".format(gameState.hiScore)
        canvas.drawText(hiScoreStr, 130f, 24f, hudScorePaint)

        canvas.drawText("WAVE ${gameState.wave}", 240f, 14f, hudPaint)
        canvas.drawText("${gameState.formation.aliveCount}/55", 240f, 24f, subtitlePaint)

        // --- RENDERIZA A NAVE-MÃE (MYSTERY SHIP) NO TOPO ---
        gameState.mysteryShip.render(canvas)

        // --- RENDERIZA A FORMAÇÃO DE ALIENS ---
        gameState.formation.render(canvas)

        // --- RENDERIZA AS 4 BARREIRAS VERDES ([CA-09]) ---
        for (barrier in gameState.barriers) {
            barrier.render(canvas)
        }

        // --- RENDERIZA O JOGADOR E O TIRO ÚNICO ---
        if (gameState.isPlayerHit) {
            // Desenha explosão do jogador se atingido
            val expBmp = RetroSprites.explosionBitmap
            canvas.drawBitmap(expBmp, gameState.player.x, gameState.player.y, Paint())
        } else {
            gameState.player.render(canvas)
        }

        // --- VIDAS DO JOGADOR NO HUD INFERIOR (CANTOS INFERIORES) ---
        canvas.drawText("VIDAS: ${gameState.lives}", 160f, 222f, hudPaint)

        // --- BOTÕES TÁTEIS RETRO NA BASE ---
        // Botão Esquerda
        canvas.drawRect(btnLeftRect, buttonBgPaint)
        canvas.drawRect(btnLeftRect, buttonBorderPaint)
        canvas.drawText("◄ ESQ", btnLeftRect.centerX(), btnLeftRect.centerY() + 2.5f, buttonTextPaint)

        // Botão Direita
        canvas.drawRect(btnRightRect, buttonBgPaint)
        canvas.drawRect(btnRightRect, buttonBorderPaint)
        canvas.drawText("DIR ►", btnRightRect.centerX(), btnRightRect.centerY() + 2.5f, buttonTextPaint)

        // Indicador central de estado do tiro
        if (gameState.player.bullet.isActive) {
            canvas.drawText("TIRO EM VOO", 160f, 235f, statusActivePaint)
        } else {
            canvas.drawText("CANHÃO PRONTO", 160f, 235f, statusReadyPaint)
        }

        // Botão Disparo
        canvas.drawRect(btnFireRect, buttonBgPaint)
        canvas.drawRect(btnFireRect, buttonBorderPaint)
        canvas.drawText("DISPARO", btnFireRect.centerX(), btnFireRect.centerY() + 2.5f, buttonTextPaint)

        // --- OVERLAYS DE ESTADO DE JOGO (GAME OVER / WAVE CLEARED) ---
        if (gameState.mode == GameStateMode.GAME_OVER) {
            val overlayPaint = Paint().apply {
                color = Color.argb(190, 0, 0, 0)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, gameState.logicalWidth.toFloat(), gameState.logicalHeight.toFloat(), overlayPaint)

            val gameOverPaint = Paint().apply {
                color = Color.rgb(255, 48, 48)
                typeface = Typeface.MONOSPACE
                textSize = 18f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            canvas.drawText("GAME OVER", 160f, 110f, gameOverPaint)
            canvas.drawText("TOQUE NO DISPARO P/ REINICIAR", 160f, 130f, subtitlePaint)
        } else if (gameState.mode == GameStateMode.WAVE_CLEARED) {
            val waveClearedPaint = Paint().apply {
                color = Color.rgb(0, 255, 68)
                typeface = Typeface.MONOSPACE
                textSize = 14f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            canvas.drawText("VAGA CONCLUÍDA!", 160f, 115f, waveClearedPaint)
        }

        canvas.restore()
    }

    /**
     * Converte coordenada X do ecrã físico para o espaço lógico de 320px
     */
    fun screenToLogicalX(screenX: Float): Float {
        return (screenX - offsetX) / scale
    }

    /**
     * Converte coordenada Y do ecrã físico para o espaço lógico de 240px
     */
    fun screenToLogicalY(screenY: Float): Float {
        return (screenY - offsetY) / scale
    }
}
