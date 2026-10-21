package com.callista.controller;

import com.callista.model.CalculatorModel;
import com.callista.model.entity.CalculationResult;
import com.callista.model.entity.OperationType;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.expression.Expression;
import com.callista.view.MainFrame;

import java.util.Map;
import java.util.Optional;

/**
 * Controlador principal: conecta los eventos de la ventana con el modelo.
 *
 * <p>Se encarga de lo común a todas las operaciones (vista previa de la
 * expresión, interpretarla al calcular, limpiar, cambiar de modo) y delega
 * los parámetros y el cálculo de cada operación en su
 * {@link OperationController}.</p>
 */
public class CalculatorController {

    private final MainFrame view;
    private final CalculatorModel model;
    private final Map<OperationType, OperationController> operations;

    public CalculatorController(MainFrame view, CalculatorModel model) {
        this.view = view;
        this.model = model;
        this.operations = Map.of(
                OperationType.EVALUATE, new EvaluationController(view, model),
                OperationType.DERIVATIVE, new DerivativeController(view, model));

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
        OperationType operation = view.getSelectedOperation();
        if (!isSupported(operation)) {
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

        Optional<CalculationResult> result = operations.get(operation).calculate(function);
        if (result.isPresent()) {
            view.showResult(result.get().getSummary(), result.get().getSteps());
            view.setStatus("Listo");
        } else {
            view.clearResult();
        }
    }

    private void clear() {
        view.clearAll();
        view.setStatus("Listo");
    }

    private boolean isSupported(OperationType operation) {
        return model.getSupportedOperations().contains(operation) && operations.containsKey(operation);
    }
}
