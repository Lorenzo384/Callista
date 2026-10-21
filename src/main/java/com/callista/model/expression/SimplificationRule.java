package com.callista.model.expression;

/**
 * Tipos de simplificación que aplica {@link Simplifier#simplify}. Se informan
 * al estudiante para que sepa qué se hizo, por ejemplo: «Simplificar
 * (agrupar términos semejantes y cancelar factores comunes)».
 */
public enum SimplificationRule {

    NUMBERS("operar números"),
    LIKE_TERMS("agrupar términos semejantes"),
    EXPONENT_LAWS("aplicar leyes de los exponentes"),
    CANCELLATION("cancelar factores comunes"),
    KNOWN_VALUES("sustituir valores conocidos");

    private final String description;

    SimplificationRule(String description) {
        this.description = description;
    }

    /**
     * Descripción en minúsculas, lista para usarse dentro de una frase.
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
