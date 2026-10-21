package com.callista.model.parser;

/**
 * Tipos de pieza (token) en que el {@link Lexer} divide una expresión.
 */
public enum TokenType {

    /** Número, por ejemplo {@code 3} o {@code 0.5}. */
    NUMBER,
    /** Nombre: la variable, una constante ({@code pi}, {@code e}) o una función ({@code sen}). */
    IDENTIFIER,
    PLUS,
    MINUS,
    STAR,
    SLASH,
    CARET,
    LEFT_PAREN,
    RIGHT_PAREN,
    /** Marca el final del texto. */
    END
}
