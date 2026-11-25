package com.dgp.boosterpump.config

/**
 * Lightweight, zero-dependency, RFC-8259 compliant JSON AST, recursive-descent parser, and
 * pretty-printer. Runs identically on Android devices and pure-JVM unit tests while preserving
 * exact numeric types (integer vs floating-point) for strict parameter validation.
 */
sealed interface JsonValue {
    data class Obj(val entries: Map<String, JsonValue>) : JsonValue
    data class Arr(val items: List<JsonValue>) : JsonValue
    data class Str(val value: String) : JsonValue
    data class IntNum(val value: Long) : JsonValue
    data class DoubleNum(val value: Double) : JsonValue
    data class Bool(val value: Boolean) : JsonValue
    data object Null : JsonValue
}

class JsonParseException(message: String, val position: Int) : IllegalArgumentException("$message (at index $position)")

object MinimalJson {

    /** Parses a JSON string into a [JsonValue] or throws [JsonParseException]. */
    fun parse(input: String): JsonValue {
        val parser = Parser(input)
        val value = parser.parseValue()
        parser.skipWhitespace()
        if (!parser.isAtEnd()) {
            throw JsonParseException("Unexpected trailing character '${parser.peek()}'", parser.pos)
        }
        return value
    }

    /** Serializes a [JsonValue] into cleanly indented UTF-8 JSON. */
    fun stringify(value: JsonValue, indentSpaces: Int = 2): String = buildString {
        writeValue(this, value, indentSpaces, depth = 0)
        append('\n')
    }

    private fun writeValue(sb: StringBuilder, value: JsonValue, indent: Int, depth: Int) {
        when (value) {
            JsonValue.Null -> sb.append("null")
            is JsonValue.Bool -> sb.append(if (value.value) "true" else "false")
            is JsonValue.IntNum -> sb.append(value.value)
            is JsonValue.DoubleNum -> {
                if (value.value.isNaN() || value.value.isInfinite()) sb.append("null") else sb.append(value.value)
            }
            is JsonValue.Str -> writeEscapedString(sb, value.value)
            is JsonValue.Arr -> {
                if (value.items.isEmpty()) {
                    sb.append("[]")
                } else {
                    sb.append("[\n")
                    val childPad = " ".repeat((depth + 1) * indent)
                    value.items.forEachIndexed { idx, item ->
                        sb.append(childPad)
                        writeValue(sb, item, indent, depth + 1)
                        if (idx < value.items.lastIndex) sb.append(',')
                        sb.append('\n')
                    }
                    sb.append(" ".repeat(depth * indent)).append(']')
                }
            }
            is JsonValue.Obj -> {
                if (value.entries.isEmpty()) {
                    sb.append("{}")
                } else {
                    sb.append("{\n")
                    val childPad = " ".repeat((depth + 1) * indent)
                    val list = value.entries.entries.toList()
                    list.forEachIndexed { idx, (k, v) ->
                        sb.append(childPad)
                        writeEscapedString(sb, k)
                        sb.append(": ")
                        writeValue(sb, v, indent, depth + 1)
                        if (idx < list.lastIndex) sb.append(',')
                        sb.append('\n')
                    }
                    sb.append(" ".repeat(depth * indent)).append('}')
                }
            }
        }
    }

    private fun writeEscapedString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (ch in s) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (ch.code < 0x20) {
                        sb.append(String.format("\\u%04x", ch.code))
                    } else {
                        sb.append(ch)
                    }
                }
            }
        }
        sb.append('"')
    }

    private class Parser(private val src: String) {
        var pos = 0
            private set

        fun isAtEnd(): Boolean = pos >= src.length
        fun peek(): Char = src[pos]

        fun skipWhitespace() {
            while (!isAtEnd() && src[pos] in " \t\r\n") pos++
        }

        fun parseValue(): JsonValue {
            skipWhitespace()
            if (isAtEnd()) throw JsonParseException("Unexpected end of JSON input", pos)
            return when (src[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> JsonValue.Str(parseString())
                't' -> parseLiteral("true", JsonValue.Bool(true))
                'f' -> parseLiteral("false", JsonValue.Bool(false))
                'n' -> parseLiteral("null", JsonValue.Null)
                '-', in '0'..'9' -> parseNumber()
                else -> throw JsonParseException("Unexpected character '${src[pos]}'", pos)
            }
        }

        private fun parseObject(): JsonValue.Obj {
            expect('{')
            skipWhitespace()
            val map = LinkedHashMap<String, JsonValue>()
            if (!isAtEnd() && peek() == '}') {
                pos++
                return JsonValue.Obj(map)
            }
            while (true) {
                skipWhitespace()
                if (isAtEnd() || peek() != '"') {
                    throw JsonParseException("Expected object property name string", pos)
                }
                val key = parseString()
                skipWhitespace()
                expect(':')
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                if (isAtEnd()) throw JsonParseException("Unterminated JSON object", pos)
                when (peek()) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return JsonValue.Obj(map)
                    }
                    else -> throw JsonParseException("Expected ',' or '}' in object, found '${peek()}'", pos)
                }
            }
        }

        private fun parseArray(): JsonValue.Arr {
            expect('[')
            skipWhitespace()
            val items = mutableListOf<JsonValue>()
            if (!isAtEnd() && peek() == ']') {
                pos++
                return JsonValue.Arr(items)
            }
            while (true) {
                items += parseValue()
                skipWhitespace()
                if (isAtEnd()) throw JsonParseException("Unterminated JSON array", pos)
                when (peek()) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return JsonValue.Arr(items)
                    }
                    else -> throw JsonParseException("Expected ',' or ']' in array, found '${peek()}'", pos)
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (!isAtEnd()) {
                val ch = src[pos++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (isAtEnd()) throw JsonParseException("Unterminated escape sequence", pos)
                        when (val esc = src[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > src.length) throw JsonParseException("Incomplete unicode escape", pos)
                                val hex = src.substring(pos, pos + 4)
                                val code = hex.toIntOrNull(16)
                                    ?: throw JsonParseException("Invalid unicode escape \\u$hex", pos)
                                sb.append(code.toChar())
                                pos += 4
                            }
                            else -> throw JsonParseException("Invalid escape character '\\$esc'", pos - 1)
                        }
                    }
                    else -> {
                        if (ch.code < 0x20) throw JsonParseException("Unescaped control char in string", pos - 1)
                        sb.append(ch)
                    }
                }
            }
            throw JsonParseException("Unterminated string literal", pos)
        }

        private fun parseNumber(): JsonValue {
            val start = pos
            if (peek() == '-') pos++
            if (isAtEnd() || peek() !in '0'..'9') throw JsonParseException("Invalid number", start)
            if (peek() == '0') {
                pos++
            } else {
                while (!isAtEnd() && peek() in '0'..'9') pos++
            }
            var isFloat = false
            if (!isAtEnd() && peek() == '.') {
                isFloat = true
                pos++
                if (isAtEnd() || peek() !in '0'..'9') throw JsonParseException("Invalid decimal number", start)
                while (!isAtEnd() && peek() in '0'..'9') pos++
            }
            if (!isAtEnd() && (peek() == 'e' || peek() == 'E')) {
                isFloat = true
                pos++
                if (!isAtEnd() && (peek() == '+' || peek() == '-')) pos++
                if (isAtEnd() || peek() !in '0'..'9') throw JsonParseException("Invalid exponent", start)
                while (!isAtEnd() && peek() in '0'..'9') pos++
            }
            val token = src.substring(start, pos)
            return if (isFloat) {
                val d = token.toDoubleOrNull() ?: throw JsonParseException("Invalid double '$token'", start)
                JsonValue.DoubleNum(d)
            } else {
                val l = token.toLongOrNull() ?: throw JsonParseException("Invalid integer '$token'", start)
                JsonValue.IntNum(l)
            }
        }

        private fun parseLiteral(literal: String, result: JsonValue): JsonValue {
            if (!src.startsWith(literal, pos)) {
                throw JsonParseException("Expected '$literal'", pos)
            }
            pos += literal.length
            return result
        }

        private fun expect(expected: Char) {
            if (isAtEnd() || src[pos] != expected) {
                throw JsonParseException("Expected '$expected'", pos)
            }
            pos++
        }
    }
}
