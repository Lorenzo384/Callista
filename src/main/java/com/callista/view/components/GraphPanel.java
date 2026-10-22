package com.callista.view.components;

import com.callista.model.entity.CurveStyle;
import com.callista.model.entity.Graph;
import com.callista.model.entity.GraphArea;
import com.callista.model.entity.GraphCurve;
import com.callista.model.entity.GraphPoint;
import com.callista.model.entity.GraphPolygon;
import com.callista.model.graph.FunctionSampler;
import com.callista.model.graph.FunctionSampler.Sample;
import com.callista.model.graph.NiceTicks;
import com.callista.model.graph.Range;
import com.callista.view.ColorPalette;
import com.callista.view.GraphColors;
import com.callista.view.ViewStyles;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;

/**
 * Pestaña «Gráfica»: dibuja las curvas, puntos, áreas y figuras de un
 * {@link Graph} con ejes, cuadrícula y leyenda.
 *
 * <ul>
 *     <li><b>Rueda del ratón</b>: acercar o alejar alrededor del cursor.</li>
 *     <li><b>Arrastrar</b>: mover la vista.</li>
 *     <li><b>Doble clic</b> o «Vista inicial»: volver a la vista calculada.</li>
 *     <li><b>«Ampliar»</b>: abre la misma gráfica en una ventana grande.</li>
 * </ul>
 *
 * <p>Es un componente propio (no un formulario) porque dibuja con
 * {@code paintComponent}; se coloca en {@code MainFrame.form} como componente
 * personalizado.</p>
 */
public class GraphPanel extends JPanel {

    private static final int MAX_X_TICKS = 8;
    private static final int MAX_Y_TICKS = 6;
    private static final int MARGIN_LEFT = 46;
    private static final int MARGIN_BOTTOM = 20;
    private static final int MARGIN_TOP = 8;
    private static final int MARGIN_RIGHT = 10;
    private static final double ZOOM_STEP = 1.15;
    private static final double MIN_WIDTH = 1e-6;
    private static final double MAX_WIDTH = 1e7;
    private static final int POINT_RADIUS = 4;
    /** Altura mínima del área de dibujo para escribir la ayuda en el pie. */
    private static final int MIN_HEIGHT_FOR_HINT = 220;

    private static final String EMPTY_MESSAGE = "Calcula para ver la gráfica.";
    private static final String HINT = "Rueda: acercar · Arrastrar: mover · Doble clic: vista inicial";

    private static final Stroke FUNCTION_STROKE = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Stroke DERIVATIVE_STROKE = new BasicStroke(1.8f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_ROUND, 10f, new float[]{7f, 4f}, 0f);
    private static final Stroke TANGENT_STROKE = new BasicStroke(1.5f, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND, 10f, new float[]{1f, 4f}, 0f);

    private final JButton resetButton = toolButton("Vista inicial", "Volver a la vista calculada (doble clic)");
    private final JButton enlargeButton = toolButton("Ampliar", "Abrir la gráfica en una ventana grande");
    private final boolean enlargeable;

    private Graph graph;
    private Range xView;
    private Range yView;
    private GraphColors colors = ColorPalette.DEFAULT.getGraphColors();

    private Point mouse;
    private Point dragStart;
    private Range dragX;
    private Range dragY;

    private JDialog enlargedDialog;
    private GraphPanel enlarged;

    public GraphPanel() {
        this(true);
    }

    private GraphPanel(boolean enlargeable) {
        super(null);
        this.enlargeable = enlargeable;
        setPreferredSize(new Dimension(380, 200));
        add(resetButton);
        resetButton.addActionListener(e -> resetView());
        if (enlargeable) {
            add(enlargeButton);
            enlargeButton.addActionListener(e -> openEnlarged());
        }
        setToolTipText(HINT);
        MouseHandler handler = new MouseHandler();
        addMouseListener(handler);
        addMouseMotionListener(handler);
        addMouseWheelListener(handler);
        updateButtons();
    }

    // ------------------------------------------------------------ API

    /**
     * Dibuja una gráfica nueva con su vista inicial.
     */
    public void showGraph(Graph graph) {
        this.graph = graph;
        resetView();
        updateButtons();
        if (enlarged != null) {
            enlarged.showGraph(graph);
        }
    }

    public void clear() {
        graph = null;
        updateButtons();
        repaint();
        if (enlarged != null) {
            enlarged.clear();
        }
    }

    public void setColors(GraphColors colors) {
        this.colors = colors;
        repaint();
        if (enlarged != null) {
            enlarged.setColors(colors);
        }
    }

    /**
     * Vista calculada: el intervalo de x que propone el modelo y una escala
     * vertical que muestra bien las curvas y los puntos.
     */
    private void resetView() {
        if (graph == null) {
            return;
        }
        xView = graph.xRange();
        // Los puntos marcados y, si hay áreas limitadas por el eje x, el eje deben quedar a la vista
        boolean showAxis = graph.areas().stream().anyMatch(GraphArea::touchesXAxis);
        double[] pointYs = DoubleStream.concat(
                graph.points().stream().mapToDouble(GraphPoint::y),
                showAxis ? DoubleStream.of(0) : DoubleStream.empty())
                .toArray();
        yView = FunctionSampler.suggestYRange(
                graph.curves().stream().map(GraphCurve::expression).toList(), xView, pointYs);
        repaint();
    }

    // ------------------------------------------------------------ dibujo

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Rectangle plot = plotArea();
            g.setColor(colors.background());
            g.fill(plot);

            if (graph == null) {
                drawCentered(g, EMPTY_MESSAGE, plot, ViewStyles.secondaryTextColor());
                return;
            }
            drawGrid(g, plot);
            Graphics2D clipped = (Graphics2D) g.create();
            clipped.clip(plot);
            for (GraphArea area : graph.areas()) {
                drawArea(clipped, plot, area);
            }
            for (GraphPolygon polygon : graph.polygons()) {
                drawPolygon(clipped, plot, polygon);
            }
            for (GraphCurve curve : graph.curves()) {
                drawCurve(clipped, plot, curve);
            }
            drawPoints(clipped, plot);
            clipped.dispose();
            g.setColor(colors.grid().darker());
            g.draw(plot);
            drawLegend(g, plot);
            drawFooter(g, plot);
        } finally {
            g.dispose();
        }
    }

    private Rectangle plotArea() {
        return new Rectangle(MARGIN_LEFT, MARGIN_TOP,
                Math.max(1, getWidth() - MARGIN_LEFT - MARGIN_RIGHT),
                Math.max(1, getHeight() - MARGIN_TOP - MARGIN_BOTTOM));
    }

    private void drawGrid(Graphics2D g, Rectangle plot) {
        g.setFont(smallFont(10));
        FontMetrics metrics = g.getFontMetrics();

        double xStep = NiceTicks.step(xView, MAX_X_TICKS);
        for (double x : NiceTicks.ticks(xView, MAX_X_TICKS)) {
            int px = (int) Math.round(toPixelX(x, plot));
            g.setColor(colors.grid());
            g.drawLine(px, plot.y, px, plot.y + plot.height);
            String label = NiceTicks.format(x, xStep);
            g.setColor(colors.axis());
            g.drawString(label, px - metrics.stringWidth(label) / 2, plot.y + plot.height + metrics.getAscent() + 3);
        }
        double yStep = NiceTicks.step(yView, MAX_Y_TICKS);
        for (double y : NiceTicks.ticks(yView, MAX_Y_TICKS)) {
            int py = (int) Math.round(toPixelY(y, plot));
            g.setColor(colors.grid());
            g.drawLine(plot.x, py, plot.x + plot.width, py);
            String label = NiceTicks.format(y, yStep);
            g.setColor(colors.axis());
            g.drawString(label, plot.x - metrics.stringWidth(label) - 5, py + metrics.getAscent() / 2 - 1);
        }

        // Ejes x = 0 y y = 0, si están a la vista
        g.setColor(colors.axis());
        g.setStroke(new BasicStroke(1.2f));
        if (xView.contains(0)) {
            int px = (int) Math.round(toPixelX(0, plot));
            g.drawLine(px, plot.y, px, plot.y + plot.height);
        }
        if (yView.contains(0)) {
            int py = (int) Math.round(toPixelY(0, plot));
            g.drawLine(plot.x, py, plot.x + plot.width, py);
        }
    }

    /**
     * Dibuja la curva con un punto por píxel. Se corta donde la función no
     * existe o donde salta más que la altura visible (una asíntota).
     */
    private void drawCurve(Graphics2D g, Rectangle plot, GraphCurve curve) {
        List<Sample> samples = FunctionSampler.sample(curve.expression(), xView, Math.max(2, plot.width));
        Path2D path = new Path2D.Double();
        for (List<Sample> segment : FunctionSampler.segments(samples, yView.width())) {
            boolean first = true;
            for (Sample s : segment) {
                double px = toPixelX(s.x(), plot);
                double py = clampPixel(toPixelY(s.y(), plot), plot);
                if (first) {
                    path.moveTo(px, py);
                    first = false;
                } else {
                    path.lineTo(px, py);
                }
            }
        }
        g.setColor(colorOf(curve.style()));
        g.setStroke(strokeOf(curve.style()));
        g.draw(path);
    }

    /**
     * Sombrea la región entre las dos curvas en [from, to]: avanza por la
     * curva de arriba y regresa por la de abajo. Donde alguna de las dos no
     * existe, la región se interrumpe.
     */
    private void drawArea(Graphics2D g, Rectangle plot, GraphArea area) {
        double from = Math.max(area.from(), xView.min());
        double to = Math.min(area.to(), xView.max());
        if (from >= to) {
            return;
        }
        int count = Math.max(2, (int) Math.ceil((to - from) / xView.width() * plot.width));
        Range range = new Range(from, to);
        List<Sample> upper = FunctionSampler.sample(area.upper(), range, count);
        List<Sample> lower = FunctionSampler.sample(area.lower(), range, count);
        g.setColor(colors.area());
        Path2D region = new Path2D.Double();
        List<Sample> back = new ArrayList<>();
        for (int i = 0; i < upper.size(); i++) {
            Sample top = upper.get(i);
            Sample bottom = lower.get(i);
            if (!Double.isFinite(top.y()) || !Double.isFinite(bottom.y())) {
                closeRegion(g, plot, region, back);
                region = new Path2D.Double();
                continue;
            }
            double px = toPixelX(top.x(), plot);
            double py = clampPixel(toPixelY(top.y(), plot), plot);
            if (back.isEmpty()) {
                region.moveTo(px, py);
            } else {
                region.lineTo(px, py);
            }
            back.add(bottom);
        }
        closeRegion(g, plot, region, back);
    }

    /** Termina un tramo de región regresando por la curva de abajo y lo rellena. */
    private void closeRegion(Graphics2D g, Rectangle plot, Path2D region, List<Sample> back) {
        if (back.isEmpty()) {
            return;
        }
        for (Sample s : back.reversed()) {
            region.lineTo(toPixelX(s.x(), plot), clampPixel(toPixelY(s.y(), plot), plot));
        }
        region.closePath();
        g.fill(region);
        back.clear();
    }

    /**
     * Figura de un método numérico (rectángulo, trapecio o franja bajo una
     * parábola): borde visible y relleno muy suave.
     */
    private void drawPolygon(Graphics2D g, Rectangle plot, GraphPolygon polygon) {
        Path2D path = new Path2D.Double();
        boolean first = true;
        for (GraphPolygon.Vertex v : polygon.vertices()) {
            double px = toPixelX(v.x(), plot);
            double py = clampPixel(toPixelY(v.y(), plot), plot);
            if (first) {
                path.moveTo(px, py);
                first = false;
            } else {
                path.lineTo(px, py);
            }
        }
        path.closePath();
        Color outline = colors.shape();
        g.setColor(new Color(outline.getRed(), outline.getGreen(), outline.getBlue(), 28));
        g.fill(path);
        g.setColor(outline);
        g.setStroke(new BasicStroke(1f));
        g.draw(path);
    }

    /**
     * Marca los puntos con su etiqueta arriba a la derecha; si chocaría con
     * otra etiqueta, la pone debajo.
     */
    private void drawPoints(Graphics2D g, Rectangle plot) {
        g.setFont(smallFont(10));
        FontMetrics metrics = g.getFontMetrics();
        List<Rectangle> labels = new ArrayList<>();
        for (GraphPoint point : graph.points()) {
            int px = (int) Math.round(toPixelX(point.x(), plot));
            int py = (int) Math.round(toPixelY(point.y(), plot));
            g.setColor(colors.background());
            g.fillOval(px - POINT_RADIUS - 1, py - POINT_RADIUS - 1, 2 * POINT_RADIUS + 2, 2 * POINT_RADIUS + 2);
            g.setColor(colors.point());
            g.fillOval(px - POINT_RADIUS, py - POINT_RADIUS, 2 * POINT_RADIUS, 2 * POINT_RADIUS);

            int width = metrics.stringWidth(point.label());
            Rectangle above = new Rectangle(px + POINT_RADIUS + 3, py - POINT_RADIUS - 2 - metrics.getAscent(),
                    width, metrics.getHeight());
            Rectangle below = new Rectangle(above.x, py + POINT_RADIUS + 2, width, metrics.getHeight());
            Rectangle label = labels.stream().anyMatch(above::intersects) ? below : above;
            labels.add(label);
            g.drawString(point.label(), label.x, label.y + metrics.getAscent());
        }
    }

    /**
     * Leyenda en la esquina superior izquierda: una muestra del trazo y el
     * nombre de cada curva.
     */
    private void drawLegend(Graphics2D g, Rectangle plot) {
        g.setFont(smallFont(11));
        FontMetrics metrics = g.getFontMetrics();
        int sample = 22;
        int lineHeight = metrics.getHeight();
        int maxTextWidth = plot.width - sample - 24;
        int width = 0;
        for (GraphCurve curve : graph.curves()) {
            width = Math.max(width, Math.min(maxTextWidth, metrics.stringWidth(curve.label())));
        }
        List<String> areaLabels = graph.areas().stream().map(GraphArea::label).distinct().toList();
        for (String label : areaLabels) {
            width = Math.max(width, Math.min(maxTextWidth, metrics.stringWidth(label)));
        }
        int x = plot.x + 6;
        int y = plot.y + 6;
        int boxWidth = sample + 10 + width + 8;
        int boxHeight = lineHeight * (graph.curves().size() + areaLabels.size()) + 6;
        g.setColor(new Color(255, 255, 255, 225));
        g.fillRoundRect(x, y, boxWidth, boxHeight, 6, 6);
        g.setColor(colors.grid().darker());
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x, y, boxWidth, boxHeight, 6, 6);

        int baseline = y + 3 + metrics.getAscent();
        for (GraphCurve curve : graph.curves()) {
            int middle = baseline - metrics.getAscent() / 2 + 1;
            g.setColor(colorOf(curve.style()));
            g.setStroke(strokeOf(curve.style()));
            g.drawLine(x + 5, middle, x + 5 + sample, middle);
            g.setColor(colors.axis().darker());
            g.drawString(fit(curve.label(), metrics, maxTextWidth), x + sample + 12, baseline);
            baseline += lineHeight;
        }
        for (String label : areaLabels) {
            int top = baseline - metrics.getAscent() + 2;
            g.setColor(colors.area());
            g.fillRect(x + 5, top, sample, metrics.getAscent() - 3);
            g.setColor(colors.axis().darker());
            g.drawString(fit(label, metrics, maxTextWidth), x + sample + 12, baseline);
            baseline += lineHeight;
        }
    }

    /**
     * Abajo: las coordenadas del cursor si está sobre la gráfica o, si hay
     * espacio, una ayuda de cómo usarla (en la gráfica pequeña la ayuda queda
     * en el tooltip).
     */
    private void drawFooter(Graphics2D g, Rectangle plot) {
        g.setFont(smallFont(10));
        FontMetrics metrics = g.getFontMetrics();
        String text;
        if (mouse != null && plot.contains(mouse)) {
            double x = fromPixelX(mouse.x, plot);
            double y = fromPixelY(mouse.y, plot);
            text = "x = " + NiceTicks.format(x, NiceTicks.step(xView, 100))
                    + "   y = " + NiceTicks.format(y, NiceTicks.step(yView, 100));
        } else if (plot.height >= MIN_HEIGHT_FOR_HINT) {
            text = HINT;
        } else {
            return;
        }
        text = fit(text, metrics, plot.width - 8);
        int x = plot.x + plot.width - metrics.stringWidth(text) - 5;
        int y = plot.y + plot.height - 5;
        g.setColor(new Color(255, 255, 255, 200));
        g.fillRect(x - 3, y - metrics.getAscent(), metrics.stringWidth(text) + 6, metrics.getHeight());
        g.setColor(colors.axis());
        g.drawString(text, x, y);
    }

    private static void drawCentered(Graphics2D g, String text, Rectangle area, Color color) {
        g.setFont(smallFont(12));
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(color);
        g.drawString(text, area.x + (area.width - metrics.stringWidth(text)) / 2,
                area.y + (area.height + metrics.getAscent()) / 2);
    }

    // ------------------------------------------------------------ escalas

    private double toPixelX(double x, Rectangle plot) {
        return plot.x + (x - xView.min()) / xView.width() * plot.width;
    }

    private double toPixelY(double y, Rectangle plot) {
        return plot.y + plot.height - (y - yView.min()) / yView.width() * plot.height;
    }

    private double fromPixelX(double px, Rectangle plot) {
        return xView.min() + (px - plot.x) / plot.width * xView.width();
    }

    private double fromPixelY(double py, Rectangle plot) {
        return yView.min() + (plot.y + plot.height - py) / plot.height * yView.width();
    }

    /** Evita coordenadas enormes fuera de la vista, que Java2D dibuja mal. */
    private static double clampPixel(double py, Rectangle plot) {
        double limit = plot.height * 10.0;
        return Math.max(plot.y - limit, Math.min(plot.y + plot.height + limit, py));
    }

    // ------------------------------------------------------------ estilo

    private Color colorOf(CurveStyle style) {
        return switch (style) {
            case FUNCTION -> colors.function();
            case DERIVATIVE, ANTIDERIVATIVE, SECOND_FUNCTION -> colors.derivative();
            case TANGENT -> colors.tangent();
        };
    }

    private static Stroke strokeOf(CurveStyle style) {
        return switch (style) {
            case FUNCTION -> FUNCTION_STROKE;
            case DERIVATIVE, ANTIDERIVATIVE, SECOND_FUNCTION -> DERIVATIVE_STROKE;
            case TANGENT -> TANGENT_STROKE;
        };
    }

    private static Font smallFont(int size) {
        return new Font(Font.DIALOG, Font.PLAIN, size);
    }

    /** Recorta un texto con «…» para que quepa en {@code width} píxeles. */
    private static String fit(String text, FontMetrics metrics, int width) {
        if (metrics.stringWidth(text) <= width) {
            return text;
        }
        String result = text;
        while (result.length() > 1 && metrics.stringWidth(result + "…") > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "…";
    }

    // ------------------------------------------------------------ botones

    private static JButton toolButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setFont(smallFont(10));
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setMargin(new Insets(1, 6, 1, 6));
        button.putClientProperty("JButton.buttonType", "roundRect");
        return button;
    }

    private void updateButtons() {
        resetButton.setVisible(graph != null);
        enlargeButton.setVisible(graph != null && enlargeable);
    }

    /** Coloca los botones en la esquina superior derecha del área de dibujo. */
    @Override
    public void doLayout() {
        Rectangle plot = plotArea();
        int x = plot.x + plot.width - 6;
        int y = plot.y + 6;
        for (JButton button : new JButton[]{enlargeButton, resetButton}) {
            if (button.getParent() == this) {
                Dimension size = button.getPreferredSize();
                x -= size.width;
                button.setBounds(x, y, size.width, size.height);
                x -= 4;
            }
        }
    }

    private void openEnlarged() {
        if (enlargedDialog == null) {
            enlarged = new GraphPanel(false);
            enlarged.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            enlargedDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Gráfica — Callista",
                    Dialog.ModalityType.MODELESS);
            enlargedDialog.setContentPane(enlarged);
            enlargedDialog.setSize(780, 560);
            enlargedDialog.setLocationRelativeTo(this);
        }
        enlarged.setColors(colors);
        enlarged.showGraph(graph);
        enlargedDialog.setVisible(true);
    }

    // ------------------------------------------------------------ ratón

    private final class MouseHandler extends MouseAdapter {

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (graph == null) {
                return;
            }
            Rectangle plot = plotArea();
            double factor = Math.pow(ZOOM_STEP, e.getPreciseWheelRotation());
            double newWidth = xView.width() * factor;
            if (newWidth < MIN_WIDTH || newWidth > MAX_WIDTH) {
                return;
            }
            xView = xView.scale(factor, fromPixelX(e.getX(), plot));
            yView = yView.scale(factor, fromPixelY(e.getY(), plot));
            repaint();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            dragStart = e.getPoint();
            dragX = xView;
            dragY = yView;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (graph == null || dragStart == null) {
                return;
            }
            Rectangle plot = plotArea();
            double dx = (e.getX() - dragStart.x) / (double) plot.width * dragX.width();
            double dy = (e.getY() - dragStart.y) / (double) plot.height * dragY.width();
            xView = dragX.shift(-dx);
            yView = dragY.shift(dy);
            mouse = e.getPoint();
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragStart = null;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            mouse = e.getPoint();
            repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            mouse = null;
            repaint();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                resetView();
            }
        }
    }
}
