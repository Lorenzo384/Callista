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
 * Operación «Integral definida»: lee los límites (números o ±∞), el método y
 * el número de subintervalos, y calcula la integral de forma exacta (también
 * impropia) o numérica (o compara los tres métodos numéricos).
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
            a = model.interpretLimit(parameters.getLowerText());
        } catch (InvalidExpressionException e) {
            return invalid(parameters, Field.LOWER, "Límite inferior: " + e.getMessage(), e.getPosition());
        }
        try {
            b = model.interpretLimit(parameters.getUpperText());
        } catch (InvalidExpressionException e) {
            return invalid(parameters, Field.UPPER, "Límite superior: " + e.getMessage(), e.getPosition());
        }
        String variableName = parameters.getVariableName();

        if (parameters.isExactSelected()) {
            return Optional.of(
                    model.exactIntegral(function, a, b, variableName));
        }
        String otherVariable =
                "x".equals(variableName) ? "y" : "x";

        if (!"x".equals(variableName)
                || function.dependsOn(otherVariable)) {

            parameters.showGeneralError(
                    "Los métodos numéricos actuales están preparados para "
                            + "integrales respecto de x con funciones de una "
                            + "sola variable. Para integrar respecto de y o "
                            + "utilizar expresiones con x e y, selecciona "
                            + "«Exacto».");

            view.setStatus(
                    "Selecciona Exacto para esta integral");

            return Optional.empty();
        }

        double av = a.evaluate(0);
        double bv = b.evaluate(0);
        if (Double.isInfinite(av) || Double.isInfinite(bv)) {
            Field field = Double.isInfinite(av) ? Field.LOWER : Field.UPPER;
            return invalid(parameters, field, "Los métodos numéricos necesitan límites finitos: elige «Exacto» "
                    + "para la integral impropia.", 1);
        }
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
