package com.callista.model.exception;

/**
 * Ninguna de las técnicas disponibles encontró una antiderivada. Puede que
 * la función no tenga antiderivada elemental (como {@code e^(-x^2)}) o que
 * requiera una técnica que la calculadora todavía no aplica.
 */
public class UnsupportedIntegralException extends CallistaException {

    public UnsupportedIntegralException(String message) {
        super(message);
    }
}
