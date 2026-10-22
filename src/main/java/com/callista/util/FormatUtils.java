package com.callista.util;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Formato de valores para mostrarlos al usuario.
 */
public final class FormatUtils {

    /** Cifras significativas con que se muestran los resultados. */
    public static final int SIGNIFICANT_DIGITS = 10;

    /**
     * Valores más pequeños que este se muestran como 0. Absorbe el error de
     * redondeo de {@code double}: sen(π) da 1.2e-16 y no exactamente 0.
     */
    private static final double ZERO_THRESHOLD = 1e-12;

    private FormatUtils() {
    }

    /** Cifras significativas en textos breves, como las etiquetas de la gráfica. */
    public static final int SHORT_SIGNIFICANT_DIGITS = 4;

    /**
     * Como {@link #formatNumber(double)} pero con
     * {@value #SHORT_SIGNIFICANT_DIGITS} cifras: {@code 1.047} en vez de
     * {@code 1.047197551}.
     */
    public static String formatShort(double value) {
        return format(value, SHORT_SIGNIFICANT_DIGITS);
    }

    /**
     * Convierte un número a texto legible: sin ceros sobrantes ({@code 2} y no
     * {@code 2.0}), redondeado a {@value #SIGNIFICANT_DIGITS} cifras
     * significativas y en notación científica solo si es muy grande o muy
     * pequeño ({@code 1.5E15}).
     *
     * @return el número formateado, {@code "∞"}/{@code "-∞"} o {@code "indefinido"}
     */
    public static String formatNumber(double value) {
        return format(value, SIGNIFICANT_DIGITS);
    }

    private static String format(double value, int significantDigits) {
        if (Double.isNaN(value)) {
            return "indefinido";
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? "∞" : "-∞";
        }
        double magnitude = Math.abs(value);
        if (magnitude < ZERO_THRESHOLD) {
            return "0";
        }
        BigDecimal rounded = new BigDecimal(value)
                .round(new MathContext(significantDigits))
                .stripTrailingZeros();
        if (magnitude >= 1e-6 && magnitude < 1e12) {
            return rounded.toPlainString();
        }
        return rounded.toString().replace("E+", "E");
    }

    private static final String SUBSCRIPT_DIGITS = "₀₁₂₃₄₅₆₇₈₉";

    /**
     * Índice en subíndice para escribir {@code x₀}, {@code x₁₂}.
     */
    public static String subscript(int index) {
        StringBuilder result = new StringBuilder();
        for (char digit : Integer.toString(index).toCharArray()) {
            result.append(digit == '-' ? '₋' : SUBSCRIPT_DIGITS.charAt(digit - '0'));
        }
        return result.toString();
    }
}
