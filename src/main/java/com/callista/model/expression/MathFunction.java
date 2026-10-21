package com.callista.model.expression;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;

/**
 * Funciones elementales que pueden aparecer en una expresión.
 *
 * <p>Cada constante sabe su nombre, los alias que acepta al escribirse
 * (por ejemplo {@code sen} para el seno) y cómo calcular su valor.
 * Fuera del dominio de la función el resultado es {@code NaN} o
 * {@code Infinity}, igual que en el resto del árbol.</p>
 */
public enum MathFunction {

    // Trigonométricas
    SIN("sin", Math::sin, "sen"),
    COS("cos", Math::cos),
    TAN("tan", Math::tan, "tg"),
    COT("cot", v -> 1 / Math.tan(v), "ctg", "cotg"),
    SEC("sec", v -> 1 / Math.cos(v)),
    CSC("csc", v -> 1 / Math.sin(v), "cosec"),

    // Trigonométricas inversas
    ARCSIN("arcsin", Math::asin, "asin", "arcsen", "asen"),
    ARCCOS("arccos", Math::acos, "acos"),
    ARCTAN("arctan", Math::atan, "atan", "arctg"),

    // Hiperbólicas
    SINH("sinh", Math::sinh, "senh"),
    COSH("cosh", Math::cosh),
    TANH("tanh", Math::tanh, "tgh"),

    // Exponencial y logarítmicas
    EXP("exp", Math::exp),
    LN("ln", Math::log),
    LOG("log", Math::log10),

    // Otras
    SQRT("sqrt", Math::sqrt, "raiz"),
    ABS("abs", Math::abs);

    private final String name;
    private final DoubleUnaryOperator operation;
    private final List<String> aliases;

    MathFunction(String name, DoubleUnaryOperator operation, String... aliases) {
        this.name = name;
        this.operation = operation;
        this.aliases = List.of(aliases);
    }

    /**
     * Nombre con el que se escribe la función, por ejemplo {@code "sin"}.
     */
    public String getName() {
        return name;
    }

    /**
     * Otros nombres aceptados al escribir la función, por ejemplo {@code "sen"}.
     */
    public List<String> getAliases() {
        return aliases;
    }

    /**
     * Aplica la función a un valor.
     *
     * @param value argumento de la función
     * @return el resultado, o {@code NaN}/{@code Infinity} fuera del dominio
     */
    public double apply(double value) {
        return operation.applyAsDouble(value);
    }

    /**
     * Busca una función por su nombre o alias, sin distinguir mayúsculas.
     *
     * @param text nombre escrito por el usuario, por ejemplo {@code "sen"}
     * @return la función, o vacío si el nombre no corresponde a ninguna
     */
    public static Optional<MathFunction> fromName(String text) {
        String key = text.toLowerCase(Locale.ROOT);
        for (MathFunction function : values()) {
            if (function.name.equals(key) || function.aliases.contains(key)) {
                return Optional.of(function);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return name;
    }
}
