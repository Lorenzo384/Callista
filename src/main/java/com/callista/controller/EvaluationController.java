package com.callista.controller;

import com.callista.model.CalculatorModel;
import com.callista.model.entity.CalculationResult;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.expression.Expression;
import com.callista.view.MainFrame;
import com.callista.view.panels.EvaluationPanel;

import java.util.Optional;

/**
 * Operación «Evaluar función»: calcula f(x) en el punto indicado.
 */
public class EvaluationController implements OperationController {

    private final MainFrame view;
    private final CalculatorModel model;

    public EvaluationController(MainFrame view, CalculatorModel model) {
        this.view = view;
        this.model = model;
    }

    @Override
    public Optional<CalculationResult> calculate(Expression function) {
        EvaluationPanel parameters = view.getEvaluationPanel();
        try {
            double x = model.interpretValue(parameters.getValueText());
            return Optional.of(model.evaluate(function, x));
        } catch (InvalidExpressionException e) {
            parameters.showError(e.getMessage(), e.getPosition());
            view.setStatus("Revisa el valor de x");
            return Optional.empty();
        }
    }
}
