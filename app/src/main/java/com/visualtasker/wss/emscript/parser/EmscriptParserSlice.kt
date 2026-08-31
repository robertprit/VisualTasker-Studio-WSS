package com.visualtasker.wss.emscript.parser

data class EmscriptParseIssue(
    val line: Int,
    val column: Int,
    val message: String,
)

data class EmscriptParseResult(
    val ir: EmscriptIrScript?,
    val issues: List<EmscriptParseIssue>,
) {
    val isSuccess: Boolean
        get() = ir != null && issues.isEmpty()
}

data class EmscriptIrScript(
    val statements: List<EmscriptIrStatement>,
)

sealed interface EmscriptIrStatement {
    data class Let(
        val variable: String,
        val value: EmscriptIrExpression,
    ) : EmscriptIrStatement

    data class Set(
        val variable: String,
        val value: EmscriptIrExpression,
    ) : EmscriptIrStatement

    data class If(
        val condition: EmscriptIrExpression,
        val thenBranch: List<EmscriptIrStatement>,
    ) : EmscriptIrStatement
}

sealed interface EmscriptIrExpression {
    data class VariableRef(val name: String) : EmscriptIrExpression
    data class NumberLiteral(val value: Double, val raw: String) : EmscriptIrExpression
    data class StringLiteral(val value: String) : EmscriptIrExpression
    data class BooleanLiteral(val value: Boolean) : EmscriptIrExpression

    data class Binary(
        val left: EmscriptIrExpression,
        val op: EmscriptBinaryOp,
        val right: EmscriptIrExpression,
    ) : EmscriptIrExpression
}

enum class EmscriptBinaryOp {
    ADD,
    SUB,
    MUL,
    DIV,
    MOD,
    EQ,
    NEQ,
    LT,
    LTE,
    GT,
    GTE,
}

class EmscriptParserSlice {
    fun parse(script: String): EmscriptParseResult {
        return runCatching {
            val lexer = Lexer(script)
            val tokens = lexer.lex()
            val parser = Parser(tokens)
            EmscriptParseResult(
                ir = EmscriptIrScript(parser.parseStatements(untilEndKeyword = false)),
                issues = emptyList(),
            )
        }.getOrElse { error ->
            val failure = (error as? ParseException) ?: ParseException(
                line = 1,
                column = 1,
                message = error.message ?: "Unbekannter Parserfehler",
            )
            EmscriptParseResult(
                ir = null,
                issues = listOf(
                    EmscriptParseIssue(
                        line = failure.line,
                        column = failure.column,
                        message = failure.message,
                    ),
                ),
            )
        }
    }
}

private data class Token(
    val type: TokenType,
    val lexeme: String,
    val line: Int,
    val column: Int,
)

private enum class TokenType {
    IDENT,
    NUMBER,
    STRING,
    TRUE,
    FALSE,
    LET,
    SET,
    IF,
    END,
    ASSIGN,
    PLUS,
    MINUS,
    STAR,
    SLASH,
    PERCENT,
    EQEQ,
    NEQ,
    LT,
    LTE,
    GT,
    GTE,
    LPAREN,
    RPAREN,
    NEWLINE,
    EOF,
}

private class Lexer(private val source: String) {
    private var index: Int = 0
    private var line: Int = 1
    private var column: Int = 1
    private val tokens = mutableListOf<Token>()

    fun lex(): List<Token> {
        while (!isAtEnd()) {
            when (val ch = peek()) {
                ' ', '\t', '\r' -> advance()
                '\n' -> {
                    tokens += token(TokenType.NEWLINE, "\n")
                    advanceLine()
                }
                '/' -> {
                    if (peekNext() == '/') {
                        skipComment()
                    } else {
                        tokens += token(TokenType.SLASH, "/")
                        advance()
                    }
                }
                '+' -> {
                    tokens += token(TokenType.PLUS, "+")
                    advance()
                }
                '-' -> {
                    tokens += token(TokenType.MINUS, "-")
                    advance()
                }
                '*' -> {
                    tokens += token(TokenType.STAR, "*")
                    advance()
                }
                '%' -> {
                    tokens += token(TokenType.PERCENT, "%")
                    advance()
                }
                '(' -> {
                    tokens += token(TokenType.LPAREN, "(")
                    advance()
                }
                ')' -> {
                    tokens += token(TokenType.RPAREN, ")")
                    advance()
                }
                '=' -> {
                    if (peekNext() == '=') {
                        tokens += token(TokenType.EQEQ, "==")
                        advance()
                        advance()
                    } else {
                        tokens += token(TokenType.ASSIGN, "=")
                        advance()
                    }
                }
                '!' -> {
                    if (peekNext() == '=') {
                        tokens += token(TokenType.NEQ, "!=")
                        advance()
                        advance()
                    } else {
                        throw ParseException(line, column, "Unerwartetes Zeichen '!'.")
                    }
                }
                '<' -> {
                    if (peekNext() == '=') {
                        tokens += token(TokenType.LTE, "<=")
                        advance()
                        advance()
                    } else {
                        tokens += token(TokenType.LT, "<")
                        advance()
                    }
                }
                '>' -> {
                    if (peekNext() == '=') {
                        tokens += token(TokenType.GTE, ">=")
                        advance()
                        advance()
                    } else {
                        tokens += token(TokenType.GT, ">")
                        advance()
                    }
                }
                '"' -> lexString()
                else -> when {
                    ch.isDigit() -> lexNumber()
                    ch.isLetter() || ch == '_' -> lexIdentifier()
                    else -> throw ParseException(line, column, "Unerwartetes Zeichen '$ch'.")
                }
            }
        }
        tokens += Token(TokenType.EOF, "", line, column)
        return tokens
    }

    private fun lexString() {
        val startLine = line
        val startColumn = column
        advance()
        val builder = StringBuilder()
        while (!isAtEnd() && peek() != '"') {
            val ch = peek()
            if (ch == '\n') {
                throw ParseException(startLine, startColumn, "String-Literal darf keine neue Zeile enthalten.")
            }
            builder.append(ch)
            advance()
        }
        if (isAtEnd()) {
            throw ParseException(startLine, startColumn, "Unterminiertes String-Literal.")
        }
        advance()
        tokens += Token(TokenType.STRING, builder.toString(), startLine, startColumn)
    }

    private fun lexNumber() {
        val startLine = line
        val startColumn = column
        val builder = StringBuilder()
        while (!isAtEnd() && peek().isDigit()) {
            builder.append(peek())
            advance()
        }
        if (!isAtEnd() && peek() == '.') {
            builder.append('.')
            advance()
            while (!isAtEnd() && peek().isDigit()) {
                builder.append(peek())
                advance()
            }
        }
        tokens += Token(TokenType.NUMBER, builder.toString(), startLine, startColumn)
    }

    private fun lexIdentifier() {
        val startLine = line
        val startColumn = column
        val builder = StringBuilder()
        while (!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_')) {
            builder.append(peek())
            advance()
        }
        val raw = builder.toString()
        val upper = raw.uppercase()
        val type = when (upper) {
            "LET" -> TokenType.LET
            "SET" -> TokenType.SET
            "IF" -> TokenType.IF
            "END" -> TokenType.END
            "TRUE" -> TokenType.TRUE
            "FALSE" -> TokenType.FALSE
            else -> TokenType.IDENT
        }
        tokens += Token(type, raw, startLine, startColumn)
    }

    private fun skipComment() {
        while (!isAtEnd() && peek() != '\n') {
            advance()
        }
    }

    private fun token(type: TokenType, lexeme: String): Token = Token(type, lexeme, line, column)

    private fun isAtEnd(): Boolean = index >= source.length

    private fun peek(): Char = source[index]

    private fun peekNext(): Char? = source.getOrNull(index + 1)

    private fun advance() {
        if (isAtEnd()) return
        index += 1
        column += 1
    }

    private fun advanceLine() {
        index += 1
        line += 1
        column = 1
    }
}

private class Parser(private val tokens: List<Token>) {
    private var current: Int = 0

    fun parseStatements(untilEndKeyword: Boolean): List<EmscriptIrStatement> {
        val statements = mutableListOf<EmscriptIrStatement>()
        skipNewlines()
        while (!isAtEnd()) {
            if (untilEndKeyword && check(TokenType.END)) break
            statements += parseStatement()
            skipNewlines()
        }
        return statements
    }

    private fun parseStatement(): EmscriptIrStatement {
        return when {
            match(TokenType.LET) -> parseLet()
            match(TokenType.SET) -> parseSet()
            match(TokenType.IF) -> parseIf()
            else -> {
                val token = peek()
                throw ParseException(token.line, token.column, "Unerwartetes Token '${token.lexeme}'.")
            }
        }
    }

    private fun parseLet(): EmscriptIrStatement.Let {
        val variable = consume(TokenType.IDENT, "Variablenname nach LET erwartet.")
        consume(TokenType.ASSIGN, "'=' nach Variablenname erwartet.")
        val value = parseExpression()
        return EmscriptIrStatement.Let(variable.lexeme, value)
    }

    private fun parseSet(): EmscriptIrStatement.Set {
        val variable = consume(TokenType.IDENT, "Variablenname nach SET erwartet.")
        consume(TokenType.ASSIGN, "'=' nach Variablenname erwartet.")
        val value = parseExpression()
        return EmscriptIrStatement.Set(variable.lexeme, value)
    }

    private fun parseIf(): EmscriptIrStatement.If {
        val condition = parseExpression()
        skipNewlines()
        val thenBranch = parseStatements(untilEndKeyword = true)
        consume(TokenType.END, "END zum Schließen von IF erwartet.")
        consume(TokenType.IF, "IF nach END erwartet.")
        return EmscriptIrStatement.If(condition = condition, thenBranch = thenBranch)
    }

    private fun parseExpression(): EmscriptIrExpression = parseComparison()

    private fun parseComparison(): EmscriptIrExpression {
        var expr = parseAdditive()
        while (true) {
            val operator = when {
                match(TokenType.EQEQ) -> EmscriptBinaryOp.EQ
                match(TokenType.NEQ) -> EmscriptBinaryOp.NEQ
                match(TokenType.LT) -> EmscriptBinaryOp.LT
                match(TokenType.LTE) -> EmscriptBinaryOp.LTE
                match(TokenType.GT) -> EmscriptBinaryOp.GT
                match(TokenType.GTE) -> EmscriptBinaryOp.GTE
                else -> null
            } ?: break
            val right = parseAdditive()
            expr = EmscriptIrExpression.Binary(expr, operator, right)
        }
        return expr
    }

    private fun parseAdditive(): EmscriptIrExpression {
        var expr = parseMultiplicative()
        while (true) {
            val operator = when {
                match(TokenType.PLUS) -> EmscriptBinaryOp.ADD
                match(TokenType.MINUS) -> EmscriptBinaryOp.SUB
                else -> null
            } ?: break
            val right = parseMultiplicative()
            expr = EmscriptIrExpression.Binary(expr, operator, right)
        }
        return expr
    }

    private fun parseMultiplicative(): EmscriptIrExpression {
        var expr = parseUnary()
        while (true) {
            val operator = when {
                match(TokenType.STAR) -> EmscriptBinaryOp.MUL
                match(TokenType.SLASH) -> EmscriptBinaryOp.DIV
                match(TokenType.PERCENT) -> EmscriptBinaryOp.MOD
                else -> null
            } ?: break
            val right = parseUnary()
            expr = EmscriptIrExpression.Binary(expr, operator, right)
        }
        return expr
    }

    private fun parseUnary(): EmscriptIrExpression {
        if (match(TokenType.MINUS)) {
            val right = parsePrimary()
            return EmscriptIrExpression.Binary(
                left = EmscriptIrExpression.NumberLiteral(0.0, "0"),
                op = EmscriptBinaryOp.SUB,
                right = right,
            )
        }
        return parsePrimary()
    }

    private fun parsePrimary(): EmscriptIrExpression {
        return when {
            match(TokenType.NUMBER) -> {
                val token = previous()
                EmscriptIrExpression.NumberLiteral(
                    value = token.lexeme.toDoubleOrNull() ?: 0.0,
                    raw = token.lexeme,
                )
            }
            match(TokenType.STRING) -> EmscriptIrExpression.StringLiteral(previous().lexeme)
            match(TokenType.TRUE) -> EmscriptIrExpression.BooleanLiteral(true)
            match(TokenType.FALSE) -> EmscriptIrExpression.BooleanLiteral(false)
            match(TokenType.IDENT) -> EmscriptIrExpression.VariableRef(previous().lexeme)
            match(TokenType.LPAREN) -> {
                val expr = parseExpression()
                consume(TokenType.RPAREN, "Schließende Klammer ')' erwartet.")
                expr
            }
            else -> {
                val token = peek()
                throw ParseException(token.line, token.column, "Ausdruck erwartet.")
            }
        }
    }

    private fun skipNewlines() {
        while (match(TokenType.NEWLINE)) {
            // no-op
        }
    }

    private fun match(type: TokenType): Boolean {
        if (!check(type)) return false
        advance()
        return true
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        val token = peek()
        throw ParseException(token.line, token.column, message)
    }

    private fun check(type: TokenType): Boolean = !isAtEnd() && peek().type == type

    private fun advance(): Token {
        if (!isAtEnd()) current += 1
        return previous()
    }

    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF

    private fun peek(): Token = tokens[current]

    private fun previous(): Token = tokens[current - 1]
}

private class ParseException(
    val line: Int,
    val column: Int,
    override val message: String,
) : RuntimeException(message)
