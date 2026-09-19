package com.visualtasker.ime.feature

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object ExpressionEvaluator {
    fun evaluate(expression: String): Double {
        return Parser(expression).parse()
    }

    private class Parser(raw: String) {
        private val input: String = raw.replace(" ", "")
        private var pos = 0

        fun parse(): Double {
            if (input.isBlank()) error("Empty expression")
            val value = parseExpression()
            if (pos < input.length) {
                error("Unexpected token at $pos")
            }
            return value
        }

        private fun parseExpression(): Double {
            var value = parseTerm()
            while (pos < input.length) {
                when (input[pos]) {
                    '+' -> {
                        pos++
                        value += parseTerm()
                    }
                    '-' -> {
                        pos++
                        value -= parseTerm()
                    }
                    else -> return value
                }
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parsePower()
            while (pos < input.length) {
                when (input[pos]) {
                    '*' -> {
                        pos++
                        value *= parsePower()
                    }
                    '/' -> {
                        pos++
                        value /= parsePower()
                    }
                    '%' -> {
                        pos++
                        value %= parsePower()
                    }
                    else -> return value
                }
            }
            return value
        }

        private fun parsePower(): Double {
            var base = parseUnary()
            while (pos < input.length && input[pos] == '^') {
                pos++
                val exponent = parseUnary()
                base = base.pow(exponent)
            }
            return base
        }

        private fun parseUnary(): Double {
            if (pos >= input.length) error("Unexpected end of input")
            return when (input[pos]) {
                '+' -> {
                    pos++
                    parseUnary()
                }
                '-' -> {
                    pos++
                    -parseUnary()
                }
                else -> parsePrimary()
            }
        }

        private fun parsePrimary(): Double {
            if (pos >= input.length) error("Unexpected end of input")
            val c = input[pos]
            if (c == '(') {
                pos++
                val value = parseExpression()
                expect(')')
                return value
            }
            if (c.isDigit() || c == '.') {
                return parseNumber()
            }
            if (c.isLetter()) {
                val name = parseIdentifier()
                if (name == "pi") return Math.PI
                if (name == "e") return Math.E
                expect('(')
                val arg = parseExpression()
                expect(')')
                return applyFunction(name, arg)
            }
            error("Unexpected token '$c' at $pos")
        }

        private fun parseNumber(): Double {
            val start = pos
            while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) {
                pos++
            }
            return input.substring(start, pos).toDouble()
        }

        private fun parseIdentifier(): String {
            val start = pos
            while (pos < input.length && input[pos].isLetter()) {
                pos++
            }
            return input.substring(start, pos).lowercase()
        }

        private fun applyFunction(name: String, arg: Double): Double {
            return when (name) {
                "sin" -> sin(Math.toRadians(arg))
                "cos" -> cos(Math.toRadians(arg))
                "tan" -> tan(Math.toRadians(arg))
                "sqrt" -> sqrt(arg)
                "abs" -> abs(arg)
                "log" -> log10(arg)
                "ln" -> ln(arg)
                else -> error("Unknown function: $name")
            }
        }

        private fun expect(expected: Char) {
            if (pos >= input.length || input[pos] != expected) {
                error("Expected '$expected' at $pos")
            }
            pos++
        }
    }
}
