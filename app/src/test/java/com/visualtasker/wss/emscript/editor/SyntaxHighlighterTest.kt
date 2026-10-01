package com.visualtasker.wss.emscript.editor

import de.visualtasker.emscript.contract.EmscriptV1LanguageCore
import org.junit.Assert.assertEquals
import org.junit.Test

class SyntaxHighlighterTest {
    @Test
    fun `keywords are recognized case insensitively from language core`() {
        listOf("IF", "if", "If", "LET", "let", "FUNCTION", "function", "BREAK", "CONTINUE")
            .forEach { token ->
                assertEquals(token, EmscriptWordKind.CANONICAL_KEYWORD, SyntaxHighlighter.wordKind(token))
            }
    }

    @Test
    fun `legacy aliases remain distinct from canonical keywords`() {
        assertEquals(EmscriptWordKind.LEGACY_KEYWORD_ALIAS, SyntaxHighlighter.wordKind("LOOP"))
        assertEquals(EmscriptWordKind.LEGACY_KEYWORD_ALIAS, SyntaxHighlighter.wordKind("loop"))
        assertEquals(EmscriptWordKind.LEGACY_KEYWORD_ALIAS, SyntaxHighlighter.wordKind("ELSE IF"))
    }

    @Test
    fun `identifiers and commands are not classified as keywords`() {
        listOf("normalIdentifier", "§variable").forEach { token ->
            assertEquals(token, EmscriptWordKind.IDENTIFIER, SyntaxHighlighter.wordKind(token))
        }
        assertEquals(EmscriptWordKind.COMMAND, SyntaxHighlighter.wordKind("click"))
        assertEquals(EmscriptWordKind.IDENTIFIER, SyntaxHighlighter.wordKind("clickText"))
    }

    @Test
    fun `every canonical language core keyword is recognized`() {
        EmscriptV1LanguageCore.definition.keywords.forEach { definition ->
            assertEquals(
                definition.canonical,
                EmscriptWordKind.CANONICAL_KEYWORD,
                SyntaxHighlighter.wordKind(definition.canonical),
            )
        }
    }
}
