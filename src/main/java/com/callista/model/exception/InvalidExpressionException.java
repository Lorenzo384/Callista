package com.callista.model.exception;

/**
 * La expresión escrita por el usuario no se puede interpretar.
 *
 * <p>Indica qué falló y en qué posición del texto (contando desde 1),
 * por ejemplo: «Falta un término después de «+» (posición 6)».</p>
 */
public class InvalidExpressionException extends CallistaException {

    private final int position;

    /**
     * @param detail   descripción del error, sin la posición
     * @param position posición del error en el texto, contando desde 1
     */
    public InvalidExpressionException(String detail, int position) {
        super(detail + " (posición " + position + ")");
        this.position = position;
    }

    /**
     * Posición del error en el texto, contando desde 1. La vista puede usarla
     * para colocar el cursor donde está el problema.
     */
    public int getPosition() {
        return position;
    }
}
