package com.callista.controller;

import com.callista.model.CalculatorModel;
import com.callista.model.calculus.revolution.RotationAxis;
import com.callista.model.entity.CalculationResult;
import com.callista.model.entity.OperationType;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.exception.ParameterException;
import com.callista.model.expression.Expression;
import com.callista.view.MainFrame;
import com.callista.view.panels.RevolutionPanel;
import com.callista.view.panels.RevolutionPanel.Field;

import java.util.Optional;

/**
 * Operación «Volumen de sólido de revolución»: lee la región (g(x), vacía es
 * el eje x, y los límites opcionales) y el eje de giro, y calcula el volumen.
 */
public class RevolutionController implements OperationController {

    private final MainFrame view;
    private final CalculatorModel model;

    public RevolutionController(MainFrame view, CalculatorModel model) {
        this.view = view;
        this.model = model;
    }

    @Override
    public Optional<CalculationResult> calculate(Expression f) {
        RevolutionPanel parameters = view.getRevolutionPanel();
        Expression g;
        try {
            String text = parameters.getSecondFunctionText();
            g = text.isBlank() ? model.xAxis() : model.interpret(text, OperationType.SOLID_OF_REVOLUTION);
        } catch (InvalidExpressionException e) {
            return invalid(parameters, Field.SECOND_FUNCTION, "g(x): " + e.getMessage(), e.getPosition());
        }

        RotationAxis axis;
        RevolutionPanel.Axis choice = parameters.getAxis();
        if (choice.needsValue()) {
            try {
                Expression k = model.interpretConstant(parameters.getAxisValueText());
                axis = choice == RevolutionPanel.Axis.HORIZONTAL_LINE
                        ? RotationAxis.horizontalLine(k) : RotationAxis.verticalLine(k);
            } catch (InvalidExpressionException e) {
                return invalid(parameters, Field.AXIS_VALUE, "k: " + e.getMessage(), e.getPosition());
            }
        } else {
            axis = choice == RevolutionPanel.Axis.X_AXIS ? RotationAxis.xAxis() : RotationAxis.yAxis();
        }

        String lowerText = parameters.getLowerText();
        String upperText = parameters.getUpperText();
        if (lowerText.isBlank() != upperText.isBlank()) {
            Field empty = lowerText.isBlank() ? Field.LOWER : Field.UPPER;
            return invalid(parameters, empty, "Escribe a y b, o deja los dos vacíos para usar los cortes.", 1);
        }
        try {
            if (lowerText.isBlank()) {
                return Optional.of(model.enclosedVolume(f, g, axis));
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
            return Optional.of(model.volumeBetween(f, g, a, b, axis));
        } catch (ParameterException e) {
            Field field = e.getMessage().startsWith("El eje") ? Field.AXIS_VALUE : Field.LOWER;
            return invalid(parameters, field, e.getMessage(), 1);
        }
    }

    private Optional<CalculationResult> invalid(RevolutionPanel parameters, Field field, String message,
                                                int position) {
        parameters.showError(field, message, position);
        view.setStatus("Revisa los parámetros del volumen");
        return Optional.empty();
    }
}
