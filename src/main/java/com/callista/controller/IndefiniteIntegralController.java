package com.callista.controller;

import com.callista.model.CalculatorModel;
import com.callista.model.entity.CalculationResult;
import com.callista.model.expression.Expression;

import java.util.Optional;

/**
 * Operación «Integral indefinida»: no tiene parámetros; calcula
 * {@code ∫f(x) dx = F(x) + C}.
 */
public class IndefiniteIntegralController implements OperationController {

    private final CalculatorModel model;

    public IndefiniteIntegralController(CalculatorModel model) {
        this.model = model;
    }

    @Override
    public Optional<CalculationResult> calculate(Expression function) {
        return Optional.of(model.integrate(function));
    }
}
