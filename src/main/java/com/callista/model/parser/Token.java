package com.callista.model.parser;

import java.util.Objects;

/**
 * Pieza mínima con significado dentro de una expresión: un número,
 * un nombre, un operador o un paréntesis.
 */
public final class Token {

    private final TokenType type;
    private final String text;
    private final int position;

    /**
     * @param type     tipo de token
     * @param text     texto tal como aparece en la expresión
     * @param position posición del primer carácter, contando desde 1
     */
    public Token(TokenType type, String text, int position) {
        this.type = Objects.requireNonNull(type, "type");
        this.text = Objects.requireNonNull(text, "text");
        this.position = position;
    }

    public TokenType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return type + "(" + text + ")@" + position;
    }
}
