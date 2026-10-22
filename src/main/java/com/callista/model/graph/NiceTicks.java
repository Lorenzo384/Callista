package com.callista.model.graph;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Marcas «redondas» para los ejes de una gráfica: valores múltiplos de
 * 1, 2 o 5 por una potencia de 10 ({@code -10, -5, 0, 5, 10} o
 * {@code 0.2, 0.4, 0.6}), como en un papel milimetrado.
 */
public final class NiceTicks {

    private static final double[] NICE_STEPS = {1, 2, 5, 10};

    private NiceTicks() {
    }

    /**
     * Distancia entre marcas: el menor valor «redondo» que deja como máximo
     * {@code maxTicks} marcas en el intervalo.
     */
    public static double step(Range range, int maxTicks) {
        double rough = range.width() / Math.max(1, maxTicks);
        double magnitude = Math.pow(10, Math.floor(Math.log10(rough)));
        for (double nice : NICE_STEPS) {
            if (nice * magnitude >= rough) {
                return nice * magnitude;
            }
        }
        return 10 * magnitude;
    }

    /**
     * Valores de las marcas dentro del intervalo, en orden.
     */
    public static List<Double> ticks(Range range, int maxTicks) {
        double step = step(range, maxTicks);
        List<Double> ticks = new ArrayList<>();
        long first = (long) Math.ceil(range.min() / step);
        long last = (long) Math.floor(range.max() / step);
        for (long i = first; i <= last; i++) {
            double value = i * step;
            ticks.add(value == 0 ? 0 : value); // evita «-0»
        }
        return ticks;
    }

    /**
     * Texto de una marca con los decimales justos para el paso:
     * {@code 0.2} y no {@code 0.20000000000000004}.
     */
    public static String format(double value, double step) {
        int decimals = Math.max(0, (int) -Math.floor(Math.log10(step)));
        return new BigDecimal(value).setScale(decimals, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }
}
