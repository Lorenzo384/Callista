package com.callista.controller;

import com.callista.model.CalculatorModel;
import com.callista.model.entity.CalculationResult;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.exception.ParameterException;
import com.callista.model.expression.Expression;
import com.callista.view.MainFrame;
import com.callista.view.panels.IntegralPanel;
import com.callista.view.panels.IntegralPanel.Field;

import java.util.Optional;

/**
 * Operación «Integral definida»: lee los límites, el método y el número de
 * subintervalos, y calcula la integral de forma exacta o numérica (o compara
 * los tres métodos numéricos).
 */
public class IntegralController implements OperationController {

    private final MainFrame view;
    private final CalculatorModel model;

    public IntegralController(MainFrame view, CalculatorModel model) {
        this.view = view;
        this.model = model;
    }

    @Override
    public Optional<CalculationResult> calculate(Expression function) {
        IntegralPanel parameters = view.getIntegralPanel();
        Expression a;
        Expression b;
        try {
            a = model.interpretConstant(parameters.getLowerText());
        } catch (InvalidExpressionException e) {
            return invalid(parameters, Field.LOWER, "Límite inferior: " + e.getMessage(), e.getPosition());
        }
        try {
            b = model.interpretConstant(parameters.getUpperText());
        } catch (InvalidExpressionException e) {
            return invalid(parameters, Field.UPPER, "Límite superior: " + e.getMessage(), e.getPosition());
        }
        if (parameters.isExactSelected()) {
            return Optional.of(model.exactIntegral(function, a, b));
        }

        double av = a.evaluate(0);
        double bv = b.evaluate(0);
        int n = parameters.getSubintervals();
        try {
            return Optional.of(parameters.isCompareSelected()
                    ? model.compareIntegrationMethods(function, av, bv, n)
                    : model.approximateIntegral(function, av, bv, parameters.getMethod(), n));
        } catch (ParameterException e) {
            return invalid(parameters, Field.SUBINTERVALS, e.getMessage(), 1);
        }
    }

    private Optional<CalculationResult> invalid(IntegralPanel parameters, Field field, String message, int position) {
        parameters.showError(field, message, position);
        view.setStatus("Revisa los parámetros de la integral");
        return Optional.empty();
    }
}
