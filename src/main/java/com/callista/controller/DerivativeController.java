package com.callista.controller;

import com.callista.model.CalculatorModel;
import com.callista.model.entity.CalculationResult;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.expression.Expression;
import com.callista.view.MainFrame;
import com.callista.view.panels.DerivativePanel;
import com.callista.model.expression.Variable;

import java.util.Optional;

/**
 * Operación «Derivada»: deriva la función en el orden elegido y, si se
 * indicó un punto, evalúa la derivada en él.
 */
public class DerivativeController implements OperationController {

    private final MainFrame view;
    private final CalculatorModel model;

    public DerivativeController(MainFrame view, CalculatorModel model) {
        this.view = view;
        this.model = model;
    }

    @Override
    public Optional<CalculationResult> calculate(Expression function) {
        DerivativePanel parameters = view.getDerivativePanel();

        int order = parameters.getOrder();
        String variableName = parameters.getVariableName();
        String pointText = parameters.getPointText();

        // Sin punto de evaluación, calcular la derivada simbólica.
        if (pointText.isBlank()) {
            return Optional.of(
                    model.derive(function, order, variableName));
        }

        /*
         * El campo actual solo permite introducir una coordenada.
         * Una función multivariable necesita los valores de x e y
         * para evaluar la derivada en un punto.
         */
        if (!"x".equals(variableName)
                || function.contains(new Variable("y"))) {

            parameters.showError(
                    "Para evaluar una derivada en un punto, "
                            + "se necesita una función de una variable x. "
                            + "Deja vacío el campo para obtener la derivada simbólica.",
                    1);

            view.setStatus(
                    "Revisa el punto de evaluación");

            return Optional.empty();
        }

        try {
            double point = model.interpretValue(pointText);

            return Optional.of(
                    model.derive(function, order, point));

        } catch (InvalidExpressionException e) {
            parameters.showError(e.getMessage(), e.getPosition());
            view.setStatus("Revisa el punto de evaluación");
            return Optional.empty();
        }
    }
}
