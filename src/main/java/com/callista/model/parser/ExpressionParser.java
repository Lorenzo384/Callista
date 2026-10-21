package com.callista.model.parser;

import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.expression.Constant;
import com.callista.model.expression.Difference;
import com.callista.model.expression.Expression;
import com.callista.model.expression.FunctionCall;
import com.callista.model.expression.MathFunction;
import com.callista.model.expression.Negation;
import com.callista.model.expression.Power;
import com.callista.model.expression.Product;
import com.callista.model.expression.Quotient;
import com.callista.model.expression.Sum;
import com.callista.model.expression.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Convierte el texto que escribe el usuario en un árbol de {@link Expression}.
 *
 * <p>Usa la técnica de <b>descenso recursivo</b>: un método por cada nivel de
 * precedencia, de la más débil a la más fuerte. Cada método reconoce su nivel
 * y delega el resto al siguiente:</p>
 *
 * <pre>
 * expresión → término  ( (+ | -) término )*
 * término   → unario   ( (* | / | multiplicación implícita) unario )*
 * unario    → (- | +) unario | potencia
 * potencia  → primario ( ^ unario )?
 * primario  → número | π | e | variable | función( expresión ) | ( expresión )
 * </pre>
 *
 * <p>La multiplicación implícita se acepta antes de un nombre o de un
 * paréntesis: {@code 3x}, {@code 2sen(x)}, {@code (x+1)(x-1)}.</p>
 */
public class ExpressionParser {

    /** Nombre de la variable cuando no se indica otro. */
    public static final String DEFAULT_VARIABLE = "x";

    private final String variableName;
    private List<Token> tokens;
    private int current;

    public ExpressionParser() {
        this(DEFAULT_VARIABLE);
    }

    /**
     * @param variableName nombre de la variable de las expresiones, por ejemplo
     *                     {@code "x"} o {@code "t"}; debe estar formado solo
     *                     por letras y no coincidir con una constante o función
     */
    public ExpressionParser(String variableName) {
        Objects.requireNonNull(variableName, "variableName");
        if (variableName.isEmpty() || !variableName.chars().allMatch(Character::isLetter)) {
            throw new IllegalArgumentException("La variable debe estar formada solo por letras: " + variableName);
        }
        if (isConstantName(variableName) || MathFunction.fromName(variableName).isPresent()) {
            throw new IllegalArgumentException("«" + variableName + "» es una constante o función reservada");
        }
        this.variableName = variableName;
    }

    /**
     * Interpreta una expresión.
     *
     * @param text expresión escrita por el usuario, por ejemplo {@code "3x^2 + sen(x)"}
     * @return el árbol que representa la expresión
     * @throws InvalidExpressionException si el texto no es una expresión válida;
     *                                    el mensaje explica qué falló y dónde
     */
    public Expression parse(String text) throws InvalidExpressionException {
        tokens = new Lexer(text).tokenize();
        current = 0;
        if (check(TokenType.END)) {
            throw new InvalidExpressionException("La expresión está vacía", 1);
        }
        Expression expression = parseExpression();
        if (!check(TokenType.END)) {
            throw unexpected(peek());
        }
        return expression;
    }

    // expresión → término ( (+ | -) término )*
    private Expression parseExpression() throws InvalidExpressionException {
        Expression left = parseTerm();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            Token operator = advance();
            Expression right = parseTerm();
            left = operator.getType() == TokenType.PLUS ? new Sum(left, right) : new Difference(left, right);
        }
        return left;
    }

    // término → unario ( (* | / | multiplicación implícita) unario )*
    private Expression parseTerm() throws InvalidExpressionException {
        Expression left = parseUnary();
        while (true) {
            if (check(TokenType.STAR) || check(TokenType.SLASH)) {
                Token operator = advance();
                Expression right = parseUnary();
                left = operator.getType() == TokenType.STAR ? new Product(left, right) : new Quotient(left, right);
            } else if (check(TokenType.IDENTIFIER) || check(TokenType.LEFT_PAREN)) {
                left = new Product(left, parsePower());
            } else {
                return left;
            }
        }
    }

    // unario → (- | +) unario | potencia
    private Expression parseUnary() throws InvalidExpressionException {
        if (check(TokenType.MINUS)) {
            advance();
            return new Negation(parseUnary());
        }
        if (check(TokenType.PLUS)) {
            advance();
            return parseUnary();
        }
        return parsePower();
    }

    // potencia → primario ( ^ unario )?
    // El exponente vuelve a pasar por unario, lo que permite x^-1 y hace que
    // 2^3^2 se agrupe como 2^(3^2).
    private Expression parsePower() throws InvalidExpressionException {
        Expression base = parsePrimary();
        if (check(TokenType.CARET)) {
            advance();
            return new Power(base, parseUnary());
        }
        return base;
    }

    // primario → número | nombre | ( expresión )
    private Expression parsePrimary() throws InvalidExpressionException {
        Token token = peek();
        switch (token.getType()) {
            case NUMBER -> {
                advance();
                return new Constant(Double.parseDouble(token.getText()));
            }
            case IDENTIFIER -> {
                advance();
                return parseIdentifier(token);
            }
            case LEFT_PAREN -> {
                advance();
                Expression inner = parseExpression();
                expectClosingParenthesis(token);
                return inner;
            }
            default -> throw missingTerm(token);
        }
    }

    private Expression parseIdentifier(Token token) throws InvalidExpressionException {
        String name = token.getText();
        if (name.equals(variableName)) {
            return new Variable(name);
        }
        if (name.equalsIgnoreCase("pi") || name.equals("π")) {
            return Constant.PI;
        }
        if (name.equals("e")) {
            return Constant.E;
        }
        Optional<MathFunction> function = MathFunction.fromName(name);
        if (function.isPresent()) {
            return parseFunctionCall(function.get(), token);
        }
        throw unknownSymbol(token);
    }

    private Expression parseFunctionCall(MathFunction function, Token name) throws InvalidExpressionException {
        if (check(TokenType.LEFT_PAREN)) {
            Token open = advance();
            Expression argument = parseExpression();
            expectClosingParenthesis(open);
            return new FunctionCall(function, argument);
        }
        String written = name.getText();
        if (check(TokenType.CARET)) {
            throw new InvalidExpressionException("Para elevar una función escribe " + written
                    + "(x)^2, no " + written + "^2(x)", name.getPosition());
        }
        throw new InvalidExpressionException("La función «" + written + "» necesita paréntesis: "
                + written + "(x)", name.getPosition());
    }

    private void expectClosingParenthesis(Token open) throws InvalidExpressionException {
        if (check(TokenType.RIGHT_PAREN)) {
            advance();
        } else if (check(TokenType.END)) {
            throw new InvalidExpressionException("Falta cerrar el paréntesis abierto", open.getPosition());
        } else {
            throw unexpected(peek());
        }
    }

    // ---------------------------------------------------------------- errores

    /**
     * Un token que sobra después de una expresión completa.
     */
    private InvalidExpressionException unexpected(Token token) {
        String detail = switch (token.getType()) {
            case RIGHT_PAREN -> "Paréntesis «)» sin abrir";
            case NUMBER -> "Falta un operador antes de «" + token.getText() + "»";
            default -> "Símbolo inesperado «" + token.getText() + "»";
        };
        return new InvalidExpressionException(detail, token.getPosition());
    }

    /**
     * Se esperaba un número, nombre o paréntesis y llegó otra cosa.
     */
    private InvalidExpressionException missingTerm(Token token) {
        Token previous = current > 0 ? tokens.get(current - 1) : null;
        if (previous == null) {
            return new InvalidExpressionException(
                    "La expresión no puede empezar con «" + token.getText() + "»", token.getPosition());
        }
        if (token.getType() == TokenType.END) {
            return new InvalidExpressionException(
                    "Falta un término después de «" + previous.getText() + "»", previous.getPosition());
        }
        if (previous.getType() == TokenType.LEFT_PAREN && token.getType() == TokenType.RIGHT_PAREN) {
            return new InvalidExpressionException(
                    "Paréntesis vacíos: falta una expresión dentro", previous.getPosition());
        }
        return new InvalidExpressionException("Falta un término entre «" + previous.getText()
                + "» y «" + token.getText() + "»", token.getPosition());
    }

    /**
     * Un nombre que no es la variable, ni una constante, ni una función.
     * Si está formado por nombres conocidos pegados ({@code xsen}), sugiere
     * separarlos con {@code *}.
     */
    private InvalidExpressionException unknownSymbol(Token token) {
        String name = token.getText();
        Optional<List<String>> parts = splitIntoKnownWords(name);
        String detail;
        if (parts.isPresent()) {
            detail = "Símbolo desconocido «" + name + "»: ¿quisiste decir " + String.join("*", parts.get()) + "?";
        } else if (name.length() == 1) {
            detail = "Variable desconocida «" + name + "»: la variable de la función es «" + variableName + "»";
        } else {
            detail = "Símbolo desconocido «" + name + "»";
        }
        return new InvalidExpressionException(detail, token.getPosition());
    }

    /**
     * Intenta dividir un nombre en palabras conocidas, probando primero la
     * palabra más larga: {@code "xsen"} → {@code [x, sen]}.
     */
    private Optional<List<String>> splitIntoKnownWords(String name) {
        if (name.isEmpty()) {
            return Optional.of(new ArrayList<>());
        }
        for (int end = name.length(); end > 0; end--) {
            String prefix = name.substring(0, end);
            if (isKnownWord(prefix)) {
                Optional<List<String>> rest = splitIntoKnownWords(name.substring(end));
                if (rest.isPresent()) {
                    rest.get().addFirst(prefix);
                    return rest;
                }
            }
        }
        return Optional.empty();
    }

    private boolean isKnownWord(String word) {
        return word.equals(variableName) || isConstantName(word) || MathFunction.fromName(word).isPresent();
    }

    private static boolean isConstantName(String name) {
        return name.equalsIgnoreCase("pi") || name.equals("π") || name.equals("e");
    }

    // ---------------------------------------------------------- navegación

    private Token peek() {
        return tokens.get(current);
    }

    private boolean check(TokenType type) {
        return peek().getType() == type;
    }

    private Token advance() {
        return tokens.get(current++);
    }
}
