package com.callista.model.calculus.integral.technique;

import com.callista.model.exception.UnsupportedIntegralException;
import com.callista.model.expression.Expression;
import com.callista.model.expression.Monomial;

import java.util.Optional;

/**
 * Una técnica de integración (patrón Strategy). El integrador las prueba en
 * orden hasta que una funciona.
 *
 * <p>Recibe solo la parte que depende de la variable de un término, con
 * coeficiente 1: las constantes y las sumas ya las separó el integrador
 * (múltiplo constante y linealidad).</p>
 */
public interface IntegrationTechnique {

    /**
     * Intenta integrar.
     *
     * @return la antiderivada, o vacío si la técnica no aplica a esta forma
     * @throws UnsupportedIntegralException si la técnica aplica pero alguna
     *                                      integral intermedia no se puede resolver
     */
    Optional<Expression> integrate(Monomial integrand, IntegrationContext context, int level)
            throws UnsupportedIntegralException;
}
