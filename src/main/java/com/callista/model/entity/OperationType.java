package com.callista.model.entity;

/**
 * Operaciones que ofrece la calculadora. Cada una corresponde a un modo
 * del selector «Operación» de la ventana.
 */
public enum OperationType {

    EVALUATE("Evaluar función"),
    DERIVATIVE("Derivada"),
    INDEFINITE_INTEGRAL("Integral indefinida"),
    DEFINITE_INTEGRAL("Integral definida"),
    AREA_BETWEEN_CURVES("Área entre curvas"),
    SOLID_OF_REVOLUTION("Volumen de sólido de revolución");

    private final String displayName;

    OperationType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Nombre que ve el usuario, por ejemplo «Evaluar función».
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
