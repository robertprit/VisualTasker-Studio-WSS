package com.visualtasker.wss.emscript.parser

import de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog
import de.visualtasker.blockeditor.registry.CommandCatalogKind
import de.visualtasker.blockeditor.registry.CommandArgument
import de.visualtasker.blockeditor.registry.CommandArgumentType
import de.visualtasker.blockeditor.registry.CommandCatalogEntry

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
    data class CommandCall(
        val command: String,
        val arguments: String,
    ) : EmscriptIrStatement

    data class Let(
        val variable: String,
        val value: EmscriptIrExpression,
    ) : EmscriptIrStatement

    data class Set(
        val variable: String,
        val value: EmscriptIrExpression,
    ) : EmscriptIrStatement

    data class Wait(val milliseconds: EmscriptIrExpression) : EmscriptIrStatement

    data class ClickText(val text: String) : EmscriptIrStatement

    data class Output(val value: EmscriptIrExpression) : EmscriptIrStatement

    data class Beep(
        val frequency: Int? = null,
        val durationMs: Int? = null,
        val volume: Int? = null,
    ) : EmscriptIrStatement

    data class Vibrate(val pattern: List<Long>) : EmscriptIrStatement

    data class Loop(
        val times: EmscriptIrExpression,
        val body: List<EmscriptIrStatement>,
    ) : EmscriptIrStatement

    data class While(
        val condition: EmscriptIrExpression,
        val body: List<EmscriptIrStatement>,
    ) : EmscriptIrStatement

    data class If(
        val condition: EmscriptIrExpression,
        val thenBranch: List<EmscriptIrStatement>,
        val elseIfBranches: List<EmscriptElseIfBranch> = emptyList(),
        val elseBranch: List<EmscriptIrStatement> = emptyList(),
    ) : EmscriptIrStatement
}

data class EmscriptElseIfBranch(
    val condition: EmscriptIrExpression,
    val body: List<EmscriptIrStatement>,
)

sealed interface EmscriptIrExpression {
    data class VariableRef(val name: String) : EmscriptIrExpression
    data class NumberLiteral(val value: Double, val raw: String) : EmscriptIrExpression
    data class StringLiteral(val value: String) : EmscriptIrExpression
    data class BooleanLiteral(val value: Boolean) : EmscriptIrExpression
    data class FunctionCall(
        val name: String,
        val arguments: List<EmscriptIrExpression>,
    ) : EmscriptIrExpression

    data class Binary(
        val left: EmscriptIrExpression,
        val op: EmscriptBinaryOp,
        val right: EmscriptIrExpression,
    ) : EmscriptIrExpression
}

enum class EmscriptBinaryOp {
    OR,
    AND,
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
                ir = EmscriptIrScript(parser.parseStatements(untilBoundary = false)),
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
    ELSEIF,
    ELSE,
    LOOP,
    WHILE,
    END,
    ASSIGN,
    COMMA,
    DOT,
    COLON,
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
    LBRACE,
    RBRACE,
    LBRACKET,
    RBRACKET,
    SEMICOLON,
    ANDAND,
    OROR,
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
                '{' -> {
                    tokens += token(TokenType.LBRACE, "{")
                    advance()
                }
                '}' -> {
                    tokens += token(TokenType.RBRACE, "}")
                    advance()
                }
                '[' -> {
                    tokens += token(TokenType.LBRACKET, "[")
                    advance()
                }
                ']' -> {
                    tokens += token(TokenType.RBRACKET, "]")
                    advance()
                }
                ';' -> {
                    tokens += token(TokenType.SEMICOLON, ";")
                    advance()
                }
                '&' -> {
                    if (peekNext() == '&') {
                        tokens += token(TokenType.ANDAND, "&&")
                        advance()
                        advance()
                    } else {
                        throw ParseException(line, column, "Unerwartetes Zeichen '&'.")
                    }
                }
                '|' -> {
                    if (peekNext() == '|') {
                        tokens += token(TokenType.OROR, "||")
                        advance()
                        advance()
                    } else {
                        throw ParseException(line, column, "Unerwartetes Zeichen '|'.")
                    }
                }
                ',' -> {
                    tokens += token(TokenType.COMMA, ",")
                    advance()
                }
                '.' -> {
                    tokens += token(TokenType.DOT, ".")
                    advance()
                }
                ':' -> {
                    tokens += token(TokenType.COLON, ":")
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
            if (ch == '\\') {
                advance()
                if (isAtEnd()) {
                    throw ParseException(startLine, startColumn, "Unterminierte Escape-Sequenz im String-Literal.")
                }
                val escaped = when (val next = peek()) {
                    '"' -> '"'
                    '\\' -> '\\'
                    'n' -> '\n'
                    'r' -> '\r'
                    't' -> '\t'
                    else -> next
                }
                builder.append(escaped)
                advance()
            } else {
                builder.append(ch)
                advance()
            }
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
        if (upper == "REM") {
            skipComment()
            return
        }
        val type = when (upper) {
            "LET" -> TokenType.LET
            "SET" -> TokenType.SET
            "IF" -> TokenType.IF
            "ELSEIF" -> TokenType.ELSEIF
            "ELSE" -> TokenType.ELSE
            "LOOP" -> TokenType.LOOP
            "REPEAT" -> TokenType.LOOP
            "WHILE" -> TokenType.WHILE
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

    fun parseStatements(untilBoundary: Boolean): List<EmscriptIrStatement> {
        val statements = mutableListOf<EmscriptIrStatement>()
        skipSeparators()
        while (!isAtEnd()) {
            if (untilBoundary && isBlockBoundary()) break
            statements += parseStatement()
            skipSeparators()
        }
        return statements
    }

    private fun parseStatement(): EmscriptIrStatement {
        return when {
            match(TokenType.LET) -> parseLet()
            match(TokenType.SET) -> parseSet()
            match(TokenType.IF) -> parseIf()
            match(TokenType.LOOP) -> parseLoop()
            match(TokenType.WHILE) -> parseWhile()
            match(TokenType.IDENT) -> parseCommandStatement(parseQualifiedIdentifier(previous()))
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
        val braceBlock = match(TokenType.LBRACE)
        skipSeparators()
        val thenBranch = parseStatements(untilBoundary = true)
        if (braceBlock) {
            consume(TokenType.RBRACE, "'}' zum Schließen von if-Zweig erwartet.")
        }
        val elseIfBranches = mutableListOf<EmscriptElseIfBranch>()
        var elseBranch = emptyList<EmscriptIrStatement>()
        while (matchElseIf()) {
            val elseIfCondition = parseExpression()
            val elseIfBraceBlock = match(TokenType.LBRACE)
            skipSeparators()
            elseIfBranches += EmscriptElseIfBranch(
                condition = elseIfCondition,
                body = parseStatements(untilBoundary = true),
            )
            if (elseIfBraceBlock) {
                consume(TokenType.RBRACE, "'}' zum Schließen von else-if-Zweig erwartet.")
            }
        }
        if (match(TokenType.ELSE)) {
            val elseBraceBlock = match(TokenType.LBRACE)
            skipSeparators()
            elseBranch = parseStatements(untilBoundary = true)
            if (elseBraceBlock) {
                consume(TokenType.RBRACE, "'}' zum Schließen von else-Zweig erwartet.")
            }
        }
        if (!braceBlock) {
            consume(TokenType.END, "END zum Schließen von IF erwartet.")
            consume(TokenType.IF, "IF nach END erwartet.")
        }
        return EmscriptIrStatement.If(
            condition = condition,
            thenBranch = thenBranch,
            elseIfBranches = elseIfBranches,
            elseBranch = elseBranch,
        )
    }

    private fun parseLoop(): EmscriptIrStatement.Loop {
        val times = parseExpression()
        val braceBlock = match(TokenType.LBRACE)
        skipSeparators()
        val body = parseStatements(untilBoundary = true)
        if (braceBlock) {
            consume(TokenType.RBRACE, "'}' zum Schließen von repeat-Zweig erwartet.")
        } else {
            consume(TokenType.END, "END zum Schließen von LOOP erwartet.")
            consume(TokenType.LOOP, "LOOP nach END erwartet.")
        }
        return EmscriptIrStatement.Loop(times = times, body = body)
    }

    private fun parseWhile(): EmscriptIrStatement.While {
        val condition = parseExpression()
        val braceBlock = match(TokenType.LBRACE)
        skipSeparators()
        val body = parseStatements(untilBoundary = true)
        if (braceBlock) {
            consume(TokenType.RBRACE, "'}' zum Schließen von while-Zweig erwartet.")
        } else {
            consume(TokenType.END, "END zum Schließen von WHILE erwartet.")
            consume(TokenType.WHILE, "WHILE nach END erwartet.")
        }
        return EmscriptIrStatement.While(condition = condition, body = body)
    }

    private fun parseQualifiedIdentifier(first: Token): Token {
        var lexeme = first.lexeme
        while (match(TokenType.DOT)) {
            val part = consumeIdentifierPart("Identifier nach '.' erwartet.")
            lexeme += ".${part.lexeme}"
        }
        return first.copy(lexeme = lexeme)
    }

    private fun parseCommandStatement(command: Token): EmscriptIrStatement {
        return when (command.lexeme.uppercase()) {
            "WAIT" -> if (check(TokenType.LPAREN)) parseWaitFunction(command) else EmscriptIrStatement.Wait(parseExpression())
            "CLICK" -> if (check(TokenType.LPAREN)) parseClickFunction(command) else {
                EmscriptIrStatement.ClickText(consume(TokenType.STRING, "CLICK erwartet Text-String.").lexeme)
            }
            "OUTPUT" -> if (check(TokenType.LPAREN)) parseLogFunction(command) else EmscriptIrStatement.Output(parseExpression())
            "LOG" -> parseLogFunction(command)
            "BEEP" -> if (check(TokenType.LPAREN)) parseBeepFunction(command) else parseLegacyBeep()
            else -> parseFunctionCommand(command)
        }
    }

    private fun parseFunctionCommand(command: Token): EmscriptIrStatement {
        return when (command.lexeme.lowercase()) {
            "wait" -> parseWaitFunction(command)
            "click" -> parseClickFunction(command)
            "log" -> parseLogFunction(command)
            "beep" -> parseBeepFunction(command)
            "vibrate" -> {
                val args = parseIntegerFunctionArguments(command, min = 1, max = 16)
                EmscriptIrStatement.Vibrate(args)
            }
            else -> {
                val catalogEntry = VisualTaskerCommandCatalog.findByAcceptedName(command.lexeme)
                if (catalogEntry != null) {
                    val args = parseRawFunctionArguments(command)
                    validateCatalogArguments(command, catalogEntry, args.arguments)
                    return EmscriptIrStatement.CommandCall(catalogEntry.canonicalName, args.rendered)
                }
                throw ParseException(command.line, command.column, "Unbekanntes Kommando '${command.lexeme}'.")
            }
        }
    }

    private fun parseRawFunctionArguments(command: Token): RawFunctionArguments {
        consume(TokenType.LPAREN, "'(' nach ${command.lexeme} erwartet.")
        val args = mutableListOf<String>()
        val currentArg = mutableListOf<String>()
        var parenDepth = 0
        var bracketDepth = 0
        var braceDepth = 0
        while (!check(TokenType.EOF)) {
            if (check(TokenType.RPAREN) && parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) break
            val token = advance()
            when (token.type) {
                TokenType.LPAREN -> {
                    parenDepth += 1
                    currentArg += token.lexeme
                }
                TokenType.RPAREN -> {
                    parenDepth -= 1
                    currentArg += token.lexeme
                }
                TokenType.LBRACKET -> {
                    bracketDepth += 1
                    currentArg += token.lexeme
                }
                TokenType.RBRACKET -> {
                    bracketDepth -= 1
                    currentArg += token.lexeme
                }
                TokenType.LBRACE -> {
                    braceDepth += 1
                    currentArg += token.lexeme
                }
                TokenType.RBRACE -> {
                    braceDepth -= 1
                    currentArg += token.lexeme
                }
                TokenType.COMMA -> {
                    if (parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                        args += currentArg.joinToString(separator = "").trim()
                        currentArg.clear()
                    } else {
                        currentArg += token.lexeme
                    }
                }
                TokenType.STRING -> currentArg += "\"${token.lexeme.escapeEmscriptString()}\""
                TokenType.NEWLINE -> currentArg += " "
                else -> currentArg += token.lexeme
            }
            if (parenDepth < 0 || bracketDepth < 0 || braceDepth < 0) {
                throw ParseException(token.line, token.column, "Unausgeglichene Klammern in ${command.lexeme}-Parametern.")
            }
        }
        consume(TokenType.RPAREN, "')' nach ${command.lexeme}-Parametern erwartet.")
        val trailing = currentArg.joinToString(separator = "").trim()
        if (trailing.isNotEmpty() || args.isNotEmpty()) {
            args += trailing
        }
        return RawFunctionArguments(args.joinToString(","), args)
    }

    private fun validateCatalogArguments(command: Token, entry: CommandCatalogEntry, arguments: List<String>) {
        val specs = entry.arguments.filter { it.type != CommandArgumentType.STATEMENT_BODY }
        val required = specs.count { it.required }
        if (arguments.size < required) {
            throw ParseException(command.line, command.column, "${entry.canonicalName} erwartet mindestens $required Parameter.")
        }
        if (arguments.size > specs.size) {
            throw ParseException(command.line, command.column, "${entry.canonicalName} unterstützt maximal ${specs.size} Parameter.")
        }
        arguments.zip(specs).forEachIndexed { index, (raw, spec) ->
            validateRawArgument(command, entry, spec, index + 1, raw)
        }
    }

    private fun validateRawArgument(
        command: Token,
        entry: CommandCatalogEntry,
        spec: CommandArgument,
        position: Int,
        raw: String,
    ) {
        val trimmed = raw.trim()
        val valid = when (spec.type) {
            CommandArgumentType.ANY -> trimmed.isNotEmpty()
            CommandArgumentType.BOOLEAN -> trimmed.equals("true", ignoreCase = true) ||
                trimmed.equals("false", ignoreCase = true)
            CommandArgumentType.NUMBER,
            CommandArgumentType.DURATION_MS,
            CommandArgumentType.FREQUENCY_HZ,
            CommandArgumentType.PERCENT,
            -> trimmed.matches(numberArgumentRegex)
            CommandArgumentType.TEXT,
            CommandArgumentType.IMAGE_TEMPLATE,
            CommandArgumentType.REGION,
            -> trimmed.isQuotedString()
            CommandArgumentType.VARIABLE_REF -> trimmed.isIdentifierLike() || trimmed.isQuotedString()
            CommandArgumentType.STATEMENT_BODY -> true
        }
        if (!valid) {
            throw ParseException(
                command.line,
                command.column,
                "${entry.canonicalName} Parameter $position '${spec.name}' erwartet ${spec.type.name}, erhalten: $trimmed",
            )
        }
    }

    private fun parseWaitFunction(command: Token): EmscriptIrStatement.Wait {
        val args = parseIntegerFunctionArguments(command, min = 1, max = 1)
        return EmscriptIrStatement.Wait(EmscriptIrExpression.NumberLiteral(args.single().toDouble(), args.single().toString()))
    }

    private fun parseClickFunction(command: Token): EmscriptIrStatement.ClickText {
        consume(TokenType.LPAREN, "'(' nach ${command.lexeme} erwartet.")
        val text = consume(TokenType.STRING, "${command.lexeme} erwartet Text-String.").lexeme
        consume(TokenType.RPAREN, "')' nach ${command.lexeme}-Parametern erwartet.")
        return EmscriptIrStatement.ClickText(text)
    }

    private fun parseLogFunction(command: Token): EmscriptIrStatement.Output {
        consume(TokenType.LPAREN, "'(' nach ${command.lexeme} erwartet.")
        val value = parseExpression()
        consume(TokenType.RPAREN, "')' nach ${command.lexeme}-Parametern erwartet.")
        return EmscriptIrStatement.Output(value)
    }

    private fun parseBeepFunction(command: Token): EmscriptIrStatement.Beep {
        val args = parseIntegerFunctionArguments(command, min = 0, max = 3)
        return EmscriptIrStatement.Beep(
            frequency = args.getOrNull(0)?.toInt(),
            durationMs = args.getOrNull(1)?.toInt(),
            volume = args.getOrNull(2)?.toInt(),
        )
    }

    private fun parseLegacyBeep(): EmscriptIrStatement.Beep {
        val args = mutableListOf<Long>()
        while (!check(TokenType.NEWLINE) && !check(TokenType.EOF)) {
            val value = consume(TokenType.NUMBER, "BEEP erwartet numerische Parameter.").lexeme.toLongOrNull()
                ?: throw ParseException(previous().line, previous().column, "BEEP-Parameter muss eine Ganzzahl sein.")
            args += value
            if (args.size > 3) {
                val token = previous()
                throw ParseException(token.line, token.column, "BEEP unterstützt maximal drei Parameter.")
            }
        }
        return EmscriptIrStatement.Beep(
            frequency = args.getOrNull(0)?.toInt(),
            durationMs = args.getOrNull(1)?.toInt(),
            volume = args.getOrNull(2)?.toInt(),
        )
    }

    private fun parseIntegerFunctionArguments(command: Token, min: Int, max: Int): List<Long> {
        consume(TokenType.LPAREN, "'(' nach ${command.lexeme} erwartet.")
        val args = mutableListOf<Long>()
        if (!check(TokenType.RPAREN)) {
            do {
                val token = consume(TokenType.NUMBER, "${command.lexeme} erwartet numerische Parameter.")
                val value = token.lexeme.toLongOrNull()
                    ?: throw ParseException(token.line, token.column, "${command.lexeme}-Parameter muss eine Ganzzahl sein.")
                args += value
                if (args.size > max) {
                    throw ParseException(token.line, token.column, "${command.lexeme} unterstützt maximal $max Parameter.")
                }
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RPAREN, "')' nach ${command.lexeme}-Parametern erwartet.")
        if (args.size < min) {
            throw ParseException(command.line, command.column, "${command.lexeme} erwartet mindestens $min Parameter.")
        }
        return args
    }

    private fun parseExpression(): EmscriptIrExpression = parseLogicalOr()

    private fun parseLogicalOr(): EmscriptIrExpression {
        var expr = parseLogicalAnd()
        while (match(TokenType.OROR)) {
            val right = parseLogicalAnd()
            expr = EmscriptIrExpression.Binary(expr, EmscriptBinaryOp.OR, right)
        }
        return expr
    }

    private fun parseLogicalAnd(): EmscriptIrExpression {
        var expr = parseComparison()
        while (match(TokenType.ANDAND)) {
            val right = parseComparison()
            expr = EmscriptIrExpression.Binary(expr, EmscriptBinaryOp.AND, right)
        }
        return expr
    }

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
            match(TokenType.IDENT) -> {
                val identifier = parseQualifiedIdentifier(previous())
                if (check(TokenType.LPAREN)) {
                    parseExpressionFunction(identifier)
                } else {
                    EmscriptIrExpression.VariableRef(identifier.lexeme)
                }
            }
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

    private fun parseExpressionFunction(function: Token): EmscriptIrExpression.FunctionCall {
        val entry = VisualTaskerCommandCatalog.findByAcceptedName(function.lexeme)
            ?: throw ParseException(function.line, function.column, "Unbekannter Reporter '${function.lexeme}'.")
        if (entry.kind !in setOf(CommandCatalogKind.REPORTER, CommandCatalogKind.OPERATOR)) {
            throw ParseException(function.line, function.column, "'${function.lexeme}' ist kein Ausdruck.")
        }
        consume(TokenType.LPAREN, "'(' nach ${function.lexeme} erwartet.")
        val arguments = mutableListOf<EmscriptIrExpression>()
        if (!check(TokenType.RPAREN)) {
            do {
                arguments += parseExpression()
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RPAREN, "')' nach ${function.lexeme}-Parametern erwartet.")
        validateExpressionFunctionArguments(function, entry, arguments)
        return EmscriptIrExpression.FunctionCall(entry.canonicalName, arguments)
    }

    private fun validateExpressionFunctionArguments(
        function: Token,
        entry: CommandCatalogEntry,
        arguments: List<EmscriptIrExpression>,
    ) {
        val specs = entry.arguments.filter { it.type != CommandArgumentType.STATEMENT_BODY }
        val required = specs.count { it.required }
        if (arguments.size < required) {
            throw ParseException(function.line, function.column, "${entry.canonicalName} erwartet mindestens $required Parameter.")
        }
        if (arguments.size > specs.size) {
            throw ParseException(function.line, function.column, "${entry.canonicalName} unterstützt maximal ${specs.size} Parameter.")
        }
        arguments.zip(specs).forEachIndexed { index, (argument, spec) ->
            if (!argument.matchesArgumentType(spec.type)) {
                throw ParseException(
                    function.line,
                    function.column,
                    "${entry.canonicalName} Parameter ${index + 1} '${spec.name}' erwartet ${spec.type.name}.",
                )
            }
        }
    }

    private fun skipSeparators() {
        while (match(TokenType.NEWLINE) || match(TokenType.SEMICOLON)) {
            // no-op
        }
    }

    private fun isBlockBoundary(): Boolean =
        check(TokenType.END) || check(TokenType.ELSEIF) || check(TokenType.ELSE) || check(TokenType.RBRACE)

    private fun matchElseIf(): Boolean {
        if (match(TokenType.ELSEIF)) return true
        if (check(TokenType.ELSE) && tokens.getOrNull(current + 1)?.type == TokenType.IF) {
            advance()
            advance()
            return true
        }
        return false
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

    private fun consumeIdentifierPart(message: String): Token {
        val token = peek()
        if (token.type == TokenType.IDENT || token.type in identifierPartKeywords) {
            return advance()
        }
        throw ParseException(token.line, token.column, message)
    }

    private fun check(type: TokenType): Boolean =
        if (type == TokenType.EOF) {
            peek().type == TokenType.EOF
        } else {
            !isAtEnd() && peek().type == type
        }

    private fun advance(): Token {
        if (!isAtEnd()) current += 1
        return previous()
    }

    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF

    private fun peek(): Token = tokens[current]

    private fun previous(): Token = tokens[current - 1]

    private val identifierPartKeywords = setOf(
        TokenType.LET,
        TokenType.SET,
        TokenType.IF,
        TokenType.ELSEIF,
        TokenType.ELSE,
        TokenType.LOOP,
        TokenType.WHILE,
        TokenType.END,
        TokenType.TRUE,
        TokenType.FALSE,
    )
}

private data class RawFunctionArguments(
    val rendered: String,
    val arguments: List<String>,
)

private val numberArgumentRegex = Regex("-?\\d+(?:\\.\\d+)?")

private fun String.escapeEmscriptString(): String =
    replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

private fun String.isQuotedString(): Boolean =
    length >= 2 && startsWith("\"") && endsWith("\"")

private fun String.isIdentifierLike(): Boolean =
    matches(Regex("[A-Za-z_%][A-Za-z0-9_.%]*"))

private fun EmscriptIrExpression.matchesArgumentType(type: CommandArgumentType): Boolean =
    when (type) {
        CommandArgumentType.ANY -> true
        CommandArgumentType.BOOLEAN -> this is EmscriptIrExpression.BooleanLiteral ||
            this is EmscriptIrExpression.VariableRef ||
            this is EmscriptIrExpression.FunctionCall ||
            this is EmscriptIrExpression.Binary
        CommandArgumentType.NUMBER,
        CommandArgumentType.DURATION_MS,
        CommandArgumentType.FREQUENCY_HZ,
        CommandArgumentType.PERCENT,
        -> this is EmscriptIrExpression.NumberLiteral ||
            this is EmscriptIrExpression.VariableRef ||
            this is EmscriptIrExpression.FunctionCall ||
            this is EmscriptIrExpression.Binary
        CommandArgumentType.TEXT,
        CommandArgumentType.IMAGE_TEMPLATE,
        CommandArgumentType.REGION,
        -> this is EmscriptIrExpression.StringLiteral ||
            this is EmscriptIrExpression.VariableRef ||
            this is EmscriptIrExpression.FunctionCall
        CommandArgumentType.VARIABLE_REF -> this is EmscriptIrExpression.VariableRef ||
            this is EmscriptIrExpression.StringLiteral
        CommandArgumentType.STATEMENT_BODY -> true
    }

private class ParseException(
    val line: Int,
    val column: Int,
    override val message: String,
) : RuntimeException(message)
