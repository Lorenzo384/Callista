package com.callista.model.parser;

import com.callista.model.exception.InvalidExpressionException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LexerTest {

    private static List<Token> tokenize(String text) throws InvalidExpressionException {
        return new Lexer(text).tokenize();
    }

    private static List<String> texts(List<Token> tokens) {
        return tokens.stream().map(Token::getText).toList();
    }

    @Test
    void splitsExpressionIntoTokens() throws InvalidExpressionException {
        List<Token> tokens = tokenize("3x^2 + sen(x)");
        assertEquals(List.of("3", "x", "^", "2", "+", "sen", "(", "x", ")", ""), texts(tokens));
        assertEquals(List.of(TokenType.NUMBER, TokenType.IDENTIFIER, TokenType.CARET, TokenType.NUMBER,
                        TokenType.PLUS, TokenType.IDENTIFIER, TokenType.LEFT_PAREN, TokenType.IDENTIFIER,
                        TokenType.RIGHT_PAREN, TokenType.END),
                tokens.stream().map(Token::getType).toList());
    }

    @Test
    void recordsPositionsStartingAtOne() throws InvalidExpressionException {
        List<Token> tokens = tokenize("3x^2 + sen(x)");
        assertEquals(List.of(1, 2, 3, 4, 6, 8, 11, 12, 13, 14),
                tokens.stream().map(Token::getPosition).toList());
    }

    @Test
    void readsDecimalNumbers() throws InvalidExpressionException {
        assertEquals(List.of("0.5", "*", ".25", ""), texts(tokenize("0.5*.25")));
    }

    @Test
    void acceptsTypographicOperators() throws InvalidExpressionException {
        List<TokenType> types = tokenize("2·x × 3 ÷ 4 − 1").stream().map(Token::getType).toList();
        assertEquals(List.of(TokenType.NUMBER, TokenType.STAR, TokenType.IDENTIFIER, TokenType.STAR,
                TokenType.NUMBER, TokenType.SLASH, TokenType.NUMBER, TokenType.MINUS, TokenType.NUMBER,
                TokenType.END), types);
    }

    @Test
    void readsPiAsIdentifier() throws InvalidExpressionException {
        assertEquals(List.of("2", "π", ""), texts(tokenize("2π")));
    }

    @Test
    void rejectsInvalidCharactersAndNumbers() {
        InvalidExpressionException invalid = assertThrows(InvalidExpressionException.class, () -> tokenize("2 $ x"));
        assertEquals(3, invalid.getPosition());
        assertTrue(invalid.getMessage().contains("Carácter no válido «$»"));

        InvalidExpressionException comma = assertThrows(InvalidExpressionException.class, () -> tokenize("0,5"));
        assertEquals(2, comma.getPosition());
        assertTrue(comma.getMessage().contains("usa punto"));

        InvalidExpressionException twoDots = assertThrows(InvalidExpressionException.class, () -> tokenize("1.2.3"));
        assertEquals(4, twoDots.getPosition());

        assertThrows(InvalidExpressionException.class, () -> tokenize("3."));
    }
}
