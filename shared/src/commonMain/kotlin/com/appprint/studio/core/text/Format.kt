package com.appprint.studio.core.text

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

/**
 * Small, locale-independent subset of Java's `String.format`, available on every
 * Kotlin target (including Kotlin/Wasm, where `String.format` is not in stdlib).
 *
 * Supported today: `%%`, `%s`, `%d`, `%f` with precision and `%x` with zero
 * padding — exactly the placeholders used by AppPrint's Arabic strings and
 * generated HTML.
 */
fun String.fmt(vararg args: Any?): String {
    val out = StringBuilder()
    var cursor = 0
    var nextArg = 0

    while (cursor < length) {
        if (this[cursor] != '%') {
            out.append(this[cursor++])
            continue
        }

        if (cursor + 1 >= length) {
            out.append('%')
            break
        }
        if (this[cursor + 1] == '%') {
            out.append('%')
            cursor += 2
            continue
        }

        var i = cursor + 1
        var explicitIndex: Int? = null

        val indexStart = i
        while (i < length && this[i].isDigit()) i++
        if (i > indexStart && i < length && this[i] == '$') {
            explicitIndex = substring(indexStart, i).toIntOrNull()?.minus(1)
            i++
        } else {
            i = indexStart
        }

        var zeroPad = false
        var leftAlign = false
        while (i < length && this[i] in "0-+ #") {
            when (this[i]) {
                '0' -> zeroPad = true
                '-' -> leftAlign = true
            }
            i++
        }

        val widthStart = i
        while (i < length && this[i].isDigit()) i++
        val width = substring(widthStart, i).toIntOrNull() ?: 0

        var precision = -1
        if (i < length && this[i] == '.') {
            i++
            val precisionStart = i
            while (i < length && this[i].isDigit()) i++
            precision = substring(precisionStart, i).toIntOrNull() ?: 0
        }

        if (i >= length) {
            out.append('%')
            break
        }

        val conversion = this[i++]
        val argIndex = explicitIndex ?: nextArg++
        val value = args.getOrNull(argIndex)

        val rendered = when (conversion) {
            's' -> value?.toString() ?: "null"
            'd' -> when (value) {
                is Number -> value.toLong().toString()
                else -> value?.toString() ?: "null"
            }
            'f' -> formatFixed(
                value = (value as? Number)?.toDouble() ?: 0.0,
                precision = if (precision < 0) 6 else precision,
            )
            'x', 'X' -> {
                val hex = ((value as? Number)?.toLong() ?: 0L)
                    .toString(radix = 16)
                    .uppercase()
                val padded = if (zeroPad && width > 0) hex.padStart(width, '0') else hex
                if (conversion == 'X') padded else padded.lowercase()
            }
            else -> {
                out.append('%').append(conversion)
                cursor = i
                continue
            }
        }

        val finalWidth = if (zeroPad && conversion != 'f' && width > 0) width else 0
        out.append(
            when {
                leftAlign && width > rendered.length -> rendered + " ".repeat(width - rendered.length)
                finalWidth > rendered.length -> "0".repeat(finalWidth - rendered.length) + rendered
                width > rendered.length -> " ".repeat(width - rendered.length) + rendered
                else -> rendered
            }
        )
        cursor = i
    }

    return out.toString()
}

private fun formatFixed(value: Double, precision: Int): String {
    if (value.isNaN() || value.isInfinite()) return value.toString()
    val negative = value < 0.0
    val scale = 10.0.pow(precision)
    val rounded = floor(abs(value) * scale + 0.5) / scale
    val whole = rounded.toLong()
    val fraction = ((rounded - whole) * scale).toLong()
    return buildString {
        if (negative) append('-')
        append(whole)
        if (precision > 0) {
            append('.')
            append(fraction.toString().padStart(precision, '0'))
        }
    }
}
