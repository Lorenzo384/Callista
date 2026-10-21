package com.callista.controller;

import com.callista.model.CalculatorModel;
import com.callista.model.entity.CalculationResult;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.expression.Expression;
import com.callista.view.MainFrame;
import com.callista.view.panels.DerivativePanel;

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
        String pointText = parameters.getPointText();
        if (pointText.isBlank()) {
            return Optional.of(model.derive(function, order));
        }
        try {
            double point = model.interpretValue(pointText);
            return Optional.of(model.derive(function, order, point));
        } catch (InvalidExpressionException e) {
            parameters.showError(e.getMessage(), e.getPosition());
            view.setStatus("Revisa el punto de evaluación");
            return Optional.empty();
        }
    }
}
