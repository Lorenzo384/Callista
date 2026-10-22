package com.callista.model.calculus.numeric;

import com.callista.model.entity.GraphPolygon;
import com.callista.model.exception.ParameterException;
import com.callista.model.expression.Expression;

import java.util.List;

/**
 * Método numérico para aproximar una integral definida
 * {@code ∫[a, b] f(x) dx} dividiendo el intervalo en {@code n} subintervalos.
 */
public interface NumericalIntegrator {

    /**
     * Nombre completo, por ejemplo «Regla del trapecio».
     */
    String getName();

    /**
     * Nombre breve para comparaciones, por ejemplo «Trapecio».
     */
    String getShortName();

    /**
     * Fórmula general del método, para el procedimiento.
     */
    String getFormula();

    /**
     * Aproxima la integral.
     *
     * @throws ParameterException si {@code n} no es válido para este método
     */
    NumericalIntegration integrate(Expression f, double a, double b, int n) throws ParameterException;

    /**
     * Figuras con que el método aproxima el área (rectángulos, trapecios o
     * parábolas), para dibujarlas sobre la gráfica.
     */
    List<GraphPolygon> shapes(Expression f, double a, double b, int n);
}
