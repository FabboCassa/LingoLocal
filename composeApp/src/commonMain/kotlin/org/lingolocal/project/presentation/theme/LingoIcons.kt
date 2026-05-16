package org.lingolocal.project.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * ImageVector inline per i pochi simboli di cui ha bisogno l'app (Material Symbols, Apache 2.0).
 * Evita la dipendenza da `material-icons-extended`, che non è pubblicato come
 * artifact multipiattaforma per Compose Multiplatform 1.10+.
 */
internal object LingoIcons {

    val Home: ImageVector by lazy { materialVector("Home", "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z") }

    val Storage: ImageVector by lazy {
        materialVector(
            "Storage",
            "M2 20h20v-4H2v4zm2-3h2v2H4v-2zM2 4v4h20V4H2zm4 3H4V5h2v2zm-4 7h20v-4H2v4zm2-3h2v2H4v-2z"
        )
    }

    val Settings: ImageVector by lazy {
        materialVector(
            "Settings",
            "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94 0 .31.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"
        )
    }

    val Science: ImageVector by lazy {
        materialVector(
            "Science",
            "M19.8 18.4 14 10.67V6.5l1.35-1.69c.26-.33.03-.81-.39-.81H9.04c-.42 0-.65.48-.39.81L10 6.5v4.17L4.2 18.4c-.49.66-.02 1.6.8 1.6h14c.82 0 1.29-.94.8-1.6z"
        )
    }

    val KeyboardArrowRight: ImageVector by lazy {
        materialVector("KeyboardArrowRight", "M8.59 16.59 13.17 12 8.59 7.41 10 6l6 6-6 6z")
    }

    private fun materialVector(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                SvgPathParser(pathData).applyTo(this)
            }
        }.build()
}

/**
 * Parser SVG-path minimale che supporta i comandi usati dalle icone Material Symbols
 * (M/m, L/l, H/h, V/v, C/c, Z/z) inclusa la continuazione implicita.
 */
private class SvgPathParser(private val data: String) {

    private var idx = 0

    fun applyTo(builder: PathBuilder) {
        var lastCmd: Char = ' '
        skipSeparators()
        while (idx < data.length) {
            val ch = data[idx]
            val cmd: Char = if (ch.isLetter()) {
                idx++
                lastCmd = ch
                ch
            } else {
                // Continuazione implicita: dopo M diventa L, dopo m diventa l
                when (lastCmd) {
                    'M' -> 'L'
                    'm' -> 'l'
                    ' ' -> error("SVG path missing initial command")
                    else -> lastCmd
                }
            }
            executeCommand(cmd, builder)
            skipSeparators()
        }
    }

    private fun executeCommand(cmd: Char, b: PathBuilder) {
        when (cmd) {
            'M' -> b.moveTo(readFloat(), readFloat())
            'm' -> b.moveToRelative(readFloat(), readFloat())
            'L' -> b.lineTo(readFloat(), readFloat())
            'l' -> b.lineToRelative(readFloat(), readFloat())
            'H' -> b.horizontalLineTo(readFloat())
            'h' -> b.horizontalLineToRelative(readFloat())
            'V' -> b.verticalLineTo(readFloat())
            'v' -> b.verticalLineToRelative(readFloat())
            'C' -> b.curveTo(readFloat(), readFloat(), readFloat(), readFloat(), readFloat(), readFloat())
            'c' -> b.curveToRelative(readFloat(), readFloat(), readFloat(), readFloat(), readFloat(), readFloat())
            'S' -> b.reflectiveCurveTo(readFloat(), readFloat(), readFloat(), readFloat())
            's' -> b.reflectiveCurveToRelative(readFloat(), readFloat(), readFloat(), readFloat())
            'Q' -> b.quadTo(readFloat(), readFloat(), readFloat(), readFloat())
            'q' -> b.quadToRelative(readFloat(), readFloat(), readFloat(), readFloat())
            'T' -> b.reflectiveQuadTo(readFloat(), readFloat())
            't' -> b.reflectiveQuadToRelative(readFloat(), readFloat())
            'A' -> b.arcTo(readFloat(), readFloat(), readFloat(), readFlag(), readFlag(), readFloat(), readFloat())
            'a' -> b.arcToRelative(readFloat(), readFloat(), readFloat(), readFlag(), readFlag(), readFloat(), readFloat())
            'Z', 'z' -> b.close()
            else -> error("Unsupported SVG command: $cmd")
        }
    }

    private fun skipSeparators() {
        while (idx < data.length && (data[idx] == ' ' || data[idx] == ',' || data[idx] == '\t' || data[idx] == '\n')) {
            idx++
        }
    }

    private fun readFlag(): Boolean {
        skipSeparators()
        val ch = data[idx]
        require(ch == '0' || ch == '1') { "Expected SVG flag (0/1) at $idx in '$data'" }
        idx++
        return ch == '1'
    }

    private fun readFloat(): Float {
        skipSeparators()
        val start = idx
        if (idx < data.length && (data[idx] == '-' || data[idx] == '+')) idx++
        var dotSeen = false
        var eSeen = false
        while (idx < data.length) {
            val c = data[idx]
            when {
                c.isDigit() -> idx++
                c == '.' && !dotSeen -> { dotSeen = true; idx++ }
                (c == 'e' || c == 'E') && !eSeen -> { eSeen = true; idx++; if (idx < data.length && (data[idx] == '+' || data[idx] == '-')) idx++ }
                else -> break
            }
        }
        require(idx > start) { "Expected number at $idx in '$data'" }
        return data.substring(start, idx).toFloat()
    }
}
