package com.callista.view.components;

import com.callista.model.CalculatorModel;
import com.callista.model.calculus.numeric.NumericalMethod;
import com.callista.model.entity.Graph;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.view.ColorPalette;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprueba que la gráfica se dibuja sin errores. No abre ventanas: dibuja
 * sobre una imagen en memoria.
 */
class GraphPanelTest {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 220;

    private final CalculatorModel model = new CalculatorModel();

    private Graph graphOf(String function, double point) throws InvalidExpressionException {
        return model.derive(model.interpret(function), 1, point).getGraph().orElseThrow();
    }

    private static BufferedImage paint(GraphPanel panel) {
        panel.setSize(WIDTH, HEIGHT);
        panel.doLayout();
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        panel.paint(g);
        g.dispose();
        return image;
    }

    @ParameterizedTest
    @ValueSource(strings = {"x^3 - 3x", "tan(x)", "ln(x)", "1/x", "sen(x)*e^x", "5", "raiz(-1 - x^2)"})
    void drawsAnyFunction(String function) throws InvalidExpressionException {
        GraphPanel panel = new GraphPanel();
        panel.showGraph(graphOf(function, 1));
        paint(panel);
    }

    @Test
    void drawsTheAreaBetweenTwoCurves() throws Exception {
        GraphPanel panel = new GraphPanel();
        panel.showGraph(model.enclosedArea(model.interpret("x^3"), model.interpret("x")).getGraph().orElseThrow());
        paint(panel);
        // La región se interrumpe donde una curva no existe
        panel.showGraph(model.areaBetween(model.interpret("1/x"), model.interpret("0"),
                model.interpret("-1"), model.interpret("1")).getGraph().orElseThrow());
        paint(panel);
    }

    @Test
    void drawsInEveryPalette() throws InvalidExpressionException {
        GraphPanel panel = new GraphPanel();
        panel.showGraph(graphOf("x^2", 1));
        for (ColorPalette palette : ColorPalette.values()) {
            panel.setColors(palette.getGraphColors());
            paint(panel);
        }
    }

    @Test
    void theCurveChangesThePicture() throws InvalidExpressionException {
        GraphPanel empty = new GraphPanel();
        GraphPanel withGraph = new GraphPanel();
        withGraph.showGraph(graphOf("x^2", 1));

        BufferedImage before = paint(empty);
        BufferedImage after = paint(withGraph);
        int different = 0;
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (before.getRGB(x, y) != after.getRGB(x, y)) {
                    different++;
                }
            }
        }
        assertTrue(different > 1000, "la gráfica debería dibujar ejes y curvas: " + different + " píxeles distintos");

        withGraph.clear();
        paint(withGraph);
    }

    @Test
    void drawsIntegralAreasAndShapes() throws Exception {
        for (NumericalMethod method : NumericalMethod.values()) {
            GraphPanel panel = new GraphPanel();
            panel.showGraph(model.approximateIntegral(model.interpret("sen(x) + 1"), 0, 3, method, 6)
                    .getGraph().orElseThrow());
            paint(panel);
        }
    }
}
