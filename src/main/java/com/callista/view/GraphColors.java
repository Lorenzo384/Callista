package com.callista.view;

import java.awt.Color;

/**
 * Colores de la gráfica para una paleta.
 *
 * @param function   curva de la función
 * @param derivative curva de la derivada
 * @param tangent    rectas auxiliares, como la tangente
 * @param point      puntos marcados
 * @param grid       cuadrícula
 * @param axis       ejes y textos de las marcas
 * @param background fondo del área de dibujo
 */
public record GraphColors(Color function, Color derivative, Color tangent, Color point,
                          Color grid, Color axis, Color background) {
}
