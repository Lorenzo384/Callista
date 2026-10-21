package com.callista.controller;

import com.callista.model.entity.CalculationResult;
import com.callista.model.expression.Expression;

import java.util.Optional;

/**
 * Controlador de una operación concreta (evaluar, derivar…).
 *
 * <p>{@link CalculatorController} ya interpretó la función; cada operación
 * solo lee sus propios parámetros, pide el cálculo al modelo y devuelve el
 * resultado. Agregar una operación nueva es agregar una implementación.</p>
 */
public interface OperationController {

    /**
     * Calcula la operación sobre la función ya interpretada.
     *
     * @return el resultado, o vacío si algún parámetro es inválido (en ese
     *         caso el error ya se mostró junto al parámetro)
     */
    Optional<CalculationResult> calculate(Expression function);
}
