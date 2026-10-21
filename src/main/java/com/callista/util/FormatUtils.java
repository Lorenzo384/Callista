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

    /**
     * Convierte un número a texto legible: sin ceros sobrantes ({@code 2} y no
     * {@code 2.0}), redondeado a {@value #SIGNIFICANT_DIGITS} cifras
     * significativas y en notación científica solo si es muy grande o muy
     * pequeño ({@code 1.5E15}).
     *
     * @return el número formateado, {@code "∞"}/{@code "-∞"} o {@code "indefinido"}
     */
    public static String formatNumber(double value) {
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
                .round(new MathContext(SIGNIFICANT_DIGITS))
                .stripTrailingZeros();
        if (magnitude >= 1e-6 && magnitude < 1e12) {
            return rounded.toPlainString();
        }
        return rounded.toString().replace("E+", "E");
    }
}
