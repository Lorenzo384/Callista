package com.callista.controller;

import com.callista.model.CalculatorModel;
import com.callista.model.entity.CalculationResult;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.exception.ParameterException;
import com.callista.model.expression.Expression;
import com.callista.view.MainFrame;
import com.callista.view.panels.AreaPanel;
import com.callista.view.panels.AreaPanel.Field;

import java.util.Optional;

/**
 * Operación «Área entre curvas»: lee la segunda curva g(x) (vacía es el eje
 * x) y los límites. Con a y b vacíos calcula la región que encierran f y g;
 * con ambos, el área en [a, b].
 */
public class AreaController implements OperationController {

    private final MainFrame view;
    private final CalculatorModel model;

    public AreaController(MainFrame view, CalculatorModel model) {
        this.view = view;
        this.model = model;
    }

    @Override
    public Optional<CalculationResult> calculate(Expression f) {
        AreaPanel parameters = view.getAreaPanel();
        Expression g;
        try {
            String text = parameters.getSecondFunctionText();
            g = text.isBlank() ? model.xAxis() : model.interpret(text);
        } catch (InvalidExpressionException e) {
            return invalid(parameters, Field.SECOND_FUNCTION, "g(x): " + e.getMessage(), e.getPosition());
        }

        String lowerText = parameters.getLowerText();
        String upperText = parameters.getUpperText();
        try {
            if (lowerText.isBlank() && upperText.isBlank()) {
                return Optional.of(model.enclosedArea(f, g));
            }
            if (lowerText.isBlank() || upperText.isBlank()) {
                Field empty = lowerText.isBlank() ? Field.LOWER : Field.UPPER;
                return invalid(parameters, empty, "Escribe a y b, o deja los dos vacíos para usar los cortes.", 1);
            }
        } catch (ParameterException e) {
            return invalid(parameters, Field.LOWER, e.getMessage(), 1);
        }

        Expression a;
        Expression b;
        try {
            a = model.interpretConstant(lowerText);
        } catch (InvalidExpressionException e) {
            return invalid(parameters, Field.LOWER, "Límite inferior: " + e.getMessage(), e.getPosition());
        }
        try {
            b = model.interpretConstant(upperText);
        } catch (InvalidExpressionException e) {
            return invalid(parameters, Field.UPPER, "Límite superior: " + e.getMessage(), e.getPosition());
        }
        try {
            return Optional.of(model.areaBetween(f, g, a, b));
        } catch (ParameterException e) {
            return invalid(parameters, Field.LOWER, e.getMessage(), 1);
        }
    }

    private Optional<CalculationResult> invalid(AreaPanel parameters, Field field, String message, int position) {
        parameters.showError(field, message, position);
        view.setStatus("Revisa los parámetros del área");
        return Optional.empty();
    }
}
