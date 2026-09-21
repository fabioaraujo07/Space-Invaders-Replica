package com.example.game.graphics

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Utilitário para gerar Bitmaps de sprites arcade retro em tempo de execução
 * a partir de padrões de pixels (0 = transparente, 1 = cor primária).
 * Evita dependências de ficheiros externos e preserva estética 8-bit autêntica.
 */
object RetroSprites {

    // Sprite do canhão do jogador (13 colunas x 8 linhas)
    private val PLAYER_CANNON_MAP = arrayOf(
        "     ███     ",
        "     ███     ",
        "    █████    ",
        " ███████████ ",
        "█████████████",
        "█████████████",
        "█████████████",
        "█████████████"
    )

    // Lula (Squid) - Fila Superior - 8x8
    private val SQUID_F1 = arrayOf(
        "   ██   ",
        "  ████  ",
        " ██████ ",
        "██ ██ ██",
        "████████",
        "  █  █  ",
        " █ ██ █ ",
        "█ █  █ █"
    )
    private val SQUID_F2 = arrayOf(
        "   ██   ",
        "  ████  ",
        " ██████ ",
        "██ ██ ██",
        "████████",
        " █    █ ",
        "  █  █  ",
        " █    █ "
    )

    // Caranguejo (Crab) - Filas Intermédias - 11x8
    private val CRAB_F1 = arrayOf(
        "  █     █  ",
        "   █   █   ",
        "  ███████  ",
        " ██ ███ ██ ",
        "███████████",
        "█ ███████ █",
        "█ █     █ █",
        "   ██ ██   "
    )
    private val CRAB_F2 = arrayOf(
        "  █     █  ",
        "█  █   █  █",
        "█ ███████ █",
        "███ ███ ███",
        "███████████",
        " █████████ ",
        "  █     █  ",
        " █       █ "
    )

    // Polvo (Octopus) - Fila Inferior - 12x8
    private val OCTOPUS_F1 = arrayOf(
        "    ████    ",
        "  ████████  ",
        " ██████████ ",
        "███  ██  ███",
        "████████████",
        "  ███  ███  ",
        " ██  ██  ██ ",
        "██        ██"
    )
    private val OCTOPUS_F2 = arrayOf(
        "    ████    ",
        "  ████████  ",
        " ██████████ ",
        "███  ██  ███",
        "████████████",
        "   ██  ██   ",
        "  ██ ██ ██  ",
        " █ █    █ █ "
    )

    // Sprite de explosão de alien
    private val ALIEN_EXPLOSION_MAP = arrayOf(
        " █      █ ",
        "  █ █  █  ",
        "   ████   ",
        " ████████ ",
        "   ████   ",
        "  █ █  █  ",
        " █      █ "
    )

    // Nave-Mãe Vermelha (Mystery Ship / UFO) - 16x7
    private val MYSTERY_SHIP_MAP = arrayOf(
        "     ██████     ",
        "   ██████████   ",
        "  ████████████  ",
        " ██ ██ ██ ██ ██ ",
        "████████████████",
        "  ███  ██  ███  ",
        "   █        █   "
    )

    // Barreira defensiva clássica arcade (22x16)
    val BARRIER_MAP = arrayOf(
        "      ██████████      ",
        "    ██████████████    ",
        "   ████████████████   ",
        "  ██████████████████  ",
        " ████████████████████ ",
        "██████████████████████",
        "██████████████████████",
        "██████████████████████",
        "██████████████████████",
        "██████████████████████",
        "██████████████████████",
        "██████████████████████",
        "███████        ███████",
        "██████          ██████",
        "█████            █████",
        "█████            █████"
    )

    // Cores arcade retro
    val COLOR_ARCADE_GREEN = Color.rgb(0, 255, 68)
    val COLOR_WHITE = Color.rgb(255, 255, 255)
    val COLOR_RED = Color.rgb(255, 48, 48)
    val COLOR_CYAN = Color.rgb(0, 230, 255)
    val COLOR_YELLOW = Color.rgb(255, 230, 0)

    val playerBitmap: Bitmap by lazy {
        createBitmapFromAscii(PLAYER_CANNON_MAP, COLOR_ARCADE_GREEN)
    }

    val squidF1: Bitmap by lazy { createBitmapFromAscii(SQUID_F1, COLOR_WHITE) }
    val squidF2: Bitmap by lazy { createBitmapFromAscii(SQUID_F2, COLOR_WHITE) }

    val crabF1: Bitmap by lazy { createBitmapFromAscii(CRAB_F1, COLOR_CYAN) }
    val crabF2: Bitmap by lazy { createBitmapFromAscii(CRAB_F2, COLOR_CYAN) }

    val octopusF1: Bitmap by lazy { createBitmapFromAscii(OCTOPUS_F1, COLOR_YELLOW) }
    val octopusF2: Bitmap by lazy { createBitmapFromAscii(OCTOPUS_F2, COLOR_YELLOW) }

    val explosionBitmap: Bitmap by lazy { createBitmapFromAscii(ALIEN_EXPLOSION_MAP, COLOR_WHITE) }

    val mysteryShipBitmap: Bitmap by lazy { createBitmapFromAscii(MYSTERY_SHIP_MAP, COLOR_RED) }

    fun createMutableBarrierBitmap(): Bitmap {
        return createBitmapFromAscii(BARRIER_MAP, COLOR_ARCADE_GREEN)
    }

    /**
     * Converte uma matriz ASCII para um Bitmap Android nítido (ARGB_8888).
     */
    fun createBitmapFromAscii(pattern: Array<String>, pixelColor: Int): Bitmap {
        val height = pattern.size
        val width = pattern[0].length
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height) {
            val line = pattern[y]
            for (x in 0 until width) {
                if (x < line.length && line[x] != ' ') {
                    bitmap.setPixel(x, y, pixelColor)
                } else {
                    bitmap.setPixel(x, y, Color.TRANSPARENT)
                }
            }
        }
        return bitmap
    }
}
