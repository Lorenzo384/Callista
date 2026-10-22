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
 * subintervalos, y aproxima la integral (o compara los tres métodos).
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
        double a;
        double b;
        try {
            a = model.interpretValue(parameters.getLowerText());
        } catch (InvalidExpressionException e) {
            return invalid(parameters, Field.LOWER, "Límite inferior: " + e.getMessage(), e.getPosition());
        }
        try {
            b = model.interpretValue(parameters.getUpperText());
        } catch (InvalidExpressionException e) {
            return invalid(parameters, Field.UPPER, "Límite superior: " + e.getMessage(), e.getPosition());
        }

        int n = parameters.getSubintervals();
        try {
            return Optional.of(parameters.isCompareSelected()
                    ? model.compareIntegrationMethods(function, a, b, n)
                    : model.approximateIntegral(function, a, b, parameters.getMethod(), n));
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
