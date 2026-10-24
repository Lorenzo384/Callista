package com.callista.controller;

import com.callista.model.CalculatorModel;
import com.callista.model.entity.CalculationResult;
import com.callista.model.expression.Expression;
import com.callista.view.MainFrame;

import java.util.Optional;

/**
 * Operación «Integral indefinida».
 */
public class IndefiniteIntegralController
        implements OperationController {

    private final MainFrame view;
    private final CalculatorModel model;

    public IndefiniteIntegralController(
            MainFrame view, CalculatorModel model) {

        this.view = view;
        this.model = model;
    }

    @Override
    public Optional<CalculationResult> calculate(
            Expression function) {

        String variableName =
                view.getIndefiniteIntegralVariableName();

        return Optional.of(
                model.integrate(function, variableName));
    }
}
