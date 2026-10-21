package com.callista.controller;

import com.callista.model.CalculatorModel;
import com.callista.model.entity.CalculationResult;
import com.callista.model.entity.OperationType;
import com.callista.model.entity.SolutionStep;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.expression.Expression;
import com.callista.view.MainFrame;

/**
 * Controlador principal: conecta los eventos de la ventana con el modelo.
 *
 * <p>Lee lo que escribió el usuario, pide al modelo que lo interprete o lo
 * calcule y le devuelve a la vista el resultado o el mensaje de error.</p>
 */
public class CalculatorController {

    private final MainFrame view;
    private final CalculatorModel model;

    public CalculatorController(MainFrame view, CalculatorModel model) {
        this.view = view;
        this.model = model;

        view.setOperations(OperationType.values(), model.getSupportedOperations());
        view.onOperationChanged(this::changeOperation);
        view.onExpressionChanged(this::updateInterpretation);
        view.onCalculate(this::calculate);
        view.onClear(this::clear);

        changeOperation(view.getSelectedOperation());
    }

    private void changeOperation(OperationType operation) {
        view.showOperation(operation);
        view.clearResult();
        view.setStatus(isSupported(operation) ? "Listo" : "Operación en desarrollo");
    }

    /**
     * Vista previa en vivo: cada vez que cambia el texto se muestra cómo se
     * interpretó o qué error tiene. No mueve el cursor para no estorbar
     * mientras se escribe.
     */
    private void updateInterpretation() {
        String text = view.getExpressionText();
        if (text.isBlank()) {
            view.clearInterpretation();
            return;
        }
        try {
            view.showInterpretation(model.interpret(text).toString());
        } catch (InvalidExpressionException e) {
            view.showExpressionError(e.getMessage());
        }
    }

    private void calculate() {
        if (!isSupported(view.getSelectedOperation())) {
            return;
        }

        Expression function;
        try {
            function = model.interpret(view.getExpressionText());
        } catch (InvalidExpressionException e) {
            view.showExpressionError(e.getMessage());
            view.focusExpressionAt(e.getPosition());
            view.clearResult();
            view.setStatus("Revisa la expresión");
            return;
        }

        double x;
        try {
            x = model.interpretValue(view.getValueText());
        } catch (InvalidExpressionException e) {
            view.showValueError(e.getMessage(), e.getPosition());
            view.clearResult();
            view.setStatus("Revisa el valor de x");
            return;
        }

        CalculationResult result = model.evaluate(function, x);
        view.showResult(result.getSummary(),
                result.getSteps().stream().map(SolutionStep::getDescription).toList());
        view.setStatus("Listo");
    }

    private void clear() {
        view.clearAll();
        view.setStatus("Listo");
    }

    private boolean isSupported(OperationType operation) {
        return model.getSupportedOperations().contains(operation);
    }
}
