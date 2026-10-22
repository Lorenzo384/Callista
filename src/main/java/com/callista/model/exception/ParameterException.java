package com.callista.model.exception;

/**
 * Un parámetro de la operación no es válido, por ejemplo un número impar de
 * subintervalos en la regla de Simpson. El mensaje explica cómo corregirlo.
 */
public class ParameterException extends CallistaException {

    public ParameterException(String message) {
        super(message);
    }
}
