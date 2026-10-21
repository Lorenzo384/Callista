package com.callista.model.parser;

import com.callista.model.exception.InvalidExpressionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Análisis léxico: divide el texto de una expresión en tokens.
 *
 * <p>Ejemplo: {@code "3x^2 + sen(x)"} produce
 * {@code [3] [x] [^] [2] [+] [sen] [(] [x] [)] [FIN]}.
 * Los espacios se ignoran. Además de los operadores ASCII acepta
 * {@code ·} y {@code ×} para multiplicar, {@code ÷} para dividir y
 * {@code −} (signo menos tipográfico) para restar.</p>
 */
public class Lexer {

    private final String text;
    private int index;

    public Lexer(String text) {
        this.text = Objects.requireNonNull(text, "text");
    }

    /**
     * Recorre el texto completo y devuelve sus tokens. El último siempre es
     * de tipo {@link TokenType#END}.
     *
     * @throws InvalidExpressionException si hay un carácter no válido o un
     *                                    número mal escrito
     */
    public List<Token> tokenize() throws InvalidExpressionException {
        List<Token> tokens = new ArrayList<>();
        index = 0;
        while (index < text.length()) {
            char current = text.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
            } else if (Character.isDigit(current) || current == '.') {
                tokens.add(readNumber());
            } else if (Character.isLetter(current)) {
                tokens.add(readIdentifier());
            } else {
                tokens.add(readSymbol(current));
            }
        }
        tokens.add(new Token(TokenType.END, "", text.length() + 1));
        return tokens;
    }

    private Token readNumber() throws InvalidExpressionException {
        int start = index;
        boolean hasDot = false;
        while (index < text.length()) {
            char current = text.charAt(index);
            if (current == '.') {
                if (hasDot) {
                    throw new InvalidExpressionException("Número con más de un punto decimal", index + 1);
                }
                hasDot = true;
            } else if (!Character.isDigit(current)) {
                break;
            }
            index++;
        }
        String number = text.substring(start, index);
        if (number.equals(".") || number.endsWith(".")) {
            throw new InvalidExpressionException(
                    "Número incompleto «" + number + "»: faltan dígitos después del punto", start + 1);
        }
        return new Token(TokenType.NUMBER, number, start + 1);
    }

    private Token readIdentifier() {
        int start = index;
        while (index < text.length() && Character.isLetter(text.charAt(index))) {
            index++;
        }
        return new Token(TokenType.IDENTIFIER, text.substring(start, index), start + 1);
    }

    private Token readSymbol(char symbol) throws InvalidExpressionException {
        TokenType type = switch (symbol) {
            case '+' -> TokenType.PLUS;
            case '-', '−' -> TokenType.MINUS;
            case '*', '·', '×' -> TokenType.STAR;
            case '/', '÷' -> TokenType.SLASH;
            case '^' -> TokenType.CARET;
            case '(' -> TokenType.LEFT_PAREN;
            case ')' -> TokenType.RIGHT_PAREN;
            default -> null;
        };
        if (type == null) {
            String hint = symbol == ',' ? ": usa punto para los decimales, por ejemplo 0.5" : "";
            throw new InvalidExpressionException("Carácter no válido «" + symbol + "»" + hint, index + 1);
        }
        index++;
        return new Token(type, String.valueOf(symbol), index);
    }
}
