package com.callista.model.exception;

/**
 * Excepción base de Callista. Representa un error que el usuario puede
 * corregir (una expresión mal escrita, una integral sin solución…) y cuyo
 * mensaje, en español, se le muestra directamente.
 *
 * <p>Es verificada (checked): quien llama al modelo debe capturarla y
 * mostrar el mensaje en la interfaz.</p>
 */
public class CallistaException extends Exception {

    public CallistaException(String message) {
        super(message);
    }

    public CallistaException(String message, Throwable cause) {
        super(message, cause);
    }
}
