package com.callista.view.components;

import com.callista.model.entity.Solid;
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
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Vista 3D de un sólido de revolución (componente propio, sin bibliotecas).
 *
 * <p>Gira el borde de cada tramo de la región alrededor del eje y forma una
 * malla de cuadriláteros. Cada cuadrilátero se sombrea según cuánto mira hacia
 * la luz y se dibujan de atrás hacia adelante (algoritmo del pintor), así el
 * sólido se ve opaco sin necesidad de un motor 3D.</p>
 *
 * <ul>
 *     <li><b>Arrastrar</b>: girar el sólido.</li>
 *     <li><b>Rueda</b>: acercar o alejar.</li>
 *     <li><b>Doble clic</b> o «Vista inicial»: volver al ángulo de inicio.</li>
 *     <li><b>«Ampliar»</b>: abrir la vista en una ventana grande.</li>
 * </ul>
 */
public class SolidPanel extends JPanel {

    /** Pasos alrededor del eje (cada uno de 7.5°). */
    private static final int ANGLE_STEPS = 48;
    /** Puntos de cada curva del borde en un tramo. */
    private static final int PROFILE_SAMPLES = 48;
    /** Divisiones de cada lado vertical del borde. */
    private static final int SIDE_SAMPLES = 12;
    private static final double INITIAL_YAW = -0.65;
    private static final double INITIAL_PITCH = 0.42;
    private static final double INITIAL_ZOOM = 0.8;
    private static final double MAX_PITCH = 1.5;
    /** Distancia de la cámara, en tamaños del sólido: más chica, más perspectiva. */
    private static final double CAMERA_DISTANCE = 4;
    private static final double[] LIGHT = normalize(new double[]{-0.4, 0.6, 0.7});

    private static final String EMPTY_MESSAGE = "Calcula un volumen de revolución para ver el sólido.";
    private static final String HINT = "Arrastrar: girar · Rueda: acercar · Doble clic: vista inicial";

    private final JButton resetButton = toolButton("Vista inicial", "Volver al ángulo de inicio (doble clic)");
    private final JButton enlargeButton = toolButton("Ampliar", "Abrir el sólido en una ventana grande");
    private final boolean enlargeable;

    private Solid solid;
    private List<Quad> mesh = List.of();
    private double[] center = {0, 0, 0};
    private double size = 1;
    private double yaw = INITIAL_YAW;
    private double pitch = INITIAL_PITCH;
    private double zoom = INITIAL_ZOOM;
    private GraphColors colors = ColorPalette.DEFAULT.getGraphColors();

    private Point dragStart;
    private double dragYaw;
    private double dragPitch;

    private JDialog enlargedDialog;
    private SolidPanel enlarged;

    public SolidPanel() {
        this(true);
    }

    private SolidPanel(boolean enlargeable) {
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
     * Muestra un sólido nuevo desde el ángulo de inicio.
     */
    public void showSolid(Solid solid) {
        this.solid = solid;
        buildMesh();
        resetView();
        updateButtons();
        if (enlarged != null) {
            enlarged.showSolid(solid);
        }
    }

    public void clear() {
        solid = null;
        mesh = List.of();
        updateButtons();
        repaint();
        if (enlarged != null) {
            enlarged.clear();
        }
    }

    public boolean hasSolid() {
        return solid != null;
    }

    public void setColors(GraphColors colors) {
        this.colors = colors;
        repaint();
        if (enlarged != null) {
            enlarged.setColors(colors);
        }
    }

    private void resetView() {
        yaw = INITIAL_YAW;
        pitch = INITIAL_PITCH;
        zoom = INITIAL_ZOOM;
        repaint();
    }

    // ------------------------------------------------------------ malla

    /** Un cuadrilátero de la superficie, con sus cuatro vértices en 3D. */
    private record Quad(double[][] vertices) {
    }

    /**
     * Gira el borde de cada tramo (curva de arriba, lado derecho, curva de
     * abajo de regreso y lado izquierdo) alrededor del eje.
     */
    private void buildMesh() {
        List<Quad> quads = new ArrayList<>();
        double[] min = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double[] max = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (Solid.Piece piece : solid.pieces()) {
            List<double[]> border = border(piece);
            for (int i = 0; i + 1 < border.size(); i++) {
                for (int j = 0; j < ANGLE_STEPS; j++) {
                    double a1 = 2 * Math.PI * j / ANGLE_STEPS;
                    double a2 = 2 * Math.PI * (j + 1) / ANGLE_STEPS;
                    double[][] v = {rotate(border.get(i), a1), rotate(border.get(i + 1), a1),
                            rotate(border.get(i + 1), a2), rotate(border.get(i), a2)};
                    if (allFinite(v)) {
                        quads.add(new Quad(v));
                        for (double[] p : v) {
                            for (int c = 0; c < 3; c++) {
                                min[c] = Math.min(min[c], p[c]);
                                max[c] = Math.max(max[c], p[c]);
                            }
                        }
                    }
                }
            }
        }
        mesh = quads;
        if (quads.isEmpty()) {
            return;
        }
        center = new double[]{(min[0] + max[0]) / 2, (min[1] + max[1]) / 2, (min[2] + max[2]) / 2};
        size = Math.max(1e-9, Math.max(max[0] - min[0], Math.max(max[1] - min[1], max[2] - min[2])));
    }

    /** El borde de un tramo como polilínea cerrada en el plano (x, y). */
    private static List<double[]> border(Solid.Piece piece) {
        List<double[]> points = new ArrayList<>();
        for (int i = 0; i <= PROFILE_SAMPLES; i++) {
            double x = piece.from() + (piece.to() - piece.from()) * i / PROFILE_SAMPLES;
            points.add(new double[]{x, piece.upper().evaluate(x)});
        }
        addSide(points, piece.to(), piece.upper().evaluate(piece.to()), piece.lower().evaluate(piece.to()));
        for (int i = PROFILE_SAMPLES; i >= 0; i--) {
            double x = piece.from() + (piece.to() - piece.from()) * i / PROFILE_SAMPLES;
            points.add(new double[]{x, piece.lower().evaluate(x)});
        }
        addSide(points, piece.from(), piece.lower().evaluate(piece.from()), piece.upper().evaluate(piece.from()));
        points.add(points.getFirst());
        return points;
    }

    /**
     * Puntos intermedios de un lado vertical del borde: con un solo
     * cuadrilátero alto, el orden de dibujo falla y se ven huecos.
     */
    private static void addSide(List<double[]> points, double x, double from, double to) {
        for (int i = 1; i < SIDE_SAMPLES; i++) {
            points.add(new double[]{x, from + (to - from) * i / SIDE_SAMPLES});
        }
    }

    /**
     * Gira el punto (x, y) un ángulo alrededor del eje. Con eje horizontal
     * y = k, x no cambia; con eje vertical x = k, y no cambia.
     */
    private double[] rotate(double[] point, double angle) {
        double k = solid.axisValue();
        if (solid.verticalAxis()) {
            double r = point[0] - k;
            return new double[]{k + r * Math.cos(angle), point[1], r * Math.sin(angle)};
        }
        double r = point[1] - k;
        return new double[]{point[0], k + r * Math.cos(angle), r * Math.sin(angle)};
    }

    private static boolean allFinite(double[][] vertices) {
        for (double[] v : vertices) {
            for (double c : v) {
                if (!Double.isFinite(c)) {
                    return false;
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------ proyección

    /** Coordenadas de vista: el punto girado según la cámara, centrado en el sólido. */
    private double[] view(double[] p) {
        double x = p[0] - center[0];
        double y = p[1] - center[1];
        double z = p[2] - center[2];
        double cosY = Math.cos(yaw);
        double sinY = Math.sin(yaw);
        double x1 = x * cosY + z * sinY;
        double z1 = -x * sinY + z * cosY;
        double cosP = Math.cos(pitch);
        double sinP = Math.sin(pitch);
        double y2 = y * cosP - z1 * sinP;
        double z2 = y * sinP + z1 * cosP;
        return new double[]{x1, y2, z2};
    }

    /** De coordenadas de vista a píxeles, con una perspectiva suave. */
    private double[] project(double[] v) {
        double scale = zoom * Math.min(getWidth(), getHeight()) / size;
        double perspective = CAMERA_DISTANCE * size / (CAMERA_DISTANCE * size - v[2]);
        return new double[]{getWidth() / 2.0 + v[0] * scale * perspective,
                getHeight() / 2.0 - v[1] * scale * perspective};
    }

    // ------------------------------------------------------------ dibujo

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(colors.background());
            g.fillRect(0, 0, getWidth(), getHeight());
            if (solid == null || mesh.isEmpty()) {
                drawCentered(g, EMPTY_MESSAGE);
                return;
            }
            drawMesh(g);
            drawAxis(g);
            drawLabels(g);
        } finally {
            g.dispose();
        }
    }

    /** Los cuadriláteros del más lejano al más cercano, sombreados según la luz. */
    private void drawMesh(Graphics2D g) {
        record Projected(Path2D shape, double depth, double light) {
        }
        List<Projected> faces = new ArrayList<>(mesh.size());
        for (Quad quad : mesh) {
            double[][] v = new double[4][];
            double depth = 0;
            Path2D path = new Path2D.Double();
            for (int i = 0; i < 4; i++) {
                v[i] = view(quad.vertices()[i]);
                depth += v[i][2];
                double[] p = project(v[i]);
                if (i == 0) {
                    path.moveTo(p[0], p[1]);
                } else {
                    path.lineTo(p[0], p[1]);
                }
            }
            path.closePath();
            double[] normal = normalize(cross(subtract(v[2], v[0]), subtract(v[3], v[1])));
            double light = Math.abs(dot(normal, LIGHT));
            faces.add(new Projected(path, depth / 4, light));
        }
        faces.sort(Comparator.comparingDouble(Projected::depth));

        // El color de la función, aclarado para que el sombreado se distinga
        Color base = mix(colors.function(), Color.WHITE, 0.35);
        Color edge = new Color(base.getRed(), base.getGreen(), base.getBlue(), 40);
        g.setStroke(new BasicStroke(0.6f));
        for (Projected face : faces) {
            double intensity = 0.55 + 0.6 * face.light();
            g.setColor(shade(base, intensity));
            g.fill(face.shape());
            g.setColor(edge);
            g.draw(face.shape());
        }
    }

    /** El eje de giro, un poco más largo que el sólido, punteado. */
    private void drawAxis(Graphics2D g) {
        double k = solid.axisValue();
        double extra = size * 0.15;
        double[] from;
        double[] to;
        if (solid.verticalAxis()) {
            double low = center[1] - size / 2 - extra;
            double high = center[1] + size / 2 + extra;
            from = new double[]{k, low, 0};
            to = new double[]{k, high, 0};
        } else {
            double left = center[0] - size / 2 - extra;
            double right = center[0] + size / 2 + extra;
            from = new double[]{left, k, 0};
            to = new double[]{right, k, 0};
        }
        double[] a = project(view(from));
        double[] b = project(view(to));
        g.setColor(colors.tangent());
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                new float[]{6f, 4f}, 0f));
        g.draw(new Line2D.Double(a[0], a[1], b[0], b[1]));
    }

    private void drawLabels(Graphics2D g) {
        g.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        FontMetrics metrics = g.getFontMetrics();
        String axis = "Eje de giro: " + (solid.verticalAxis() ? "x = " : "y = ") + format(solid.axisValue());
        g.setColor(colors.axis().darker());
        g.drawString(solid.label(), 8, 6 + metrics.getAscent());
        g.setColor(colors.tangent());
        g.drawString(axis, 8, 6 + metrics.getAscent() + metrics.getHeight());
        if (getHeight() >= 220) {
            g.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
            FontMetrics small = g.getFontMetrics();
            g.setColor(colors.axis());
            g.drawString(HINT, getWidth() - small.stringWidth(HINT) - 6, getHeight() - 6);
        }
    }

    private void drawCentered(Graphics2D g, String text) {
        g.setColor(ViewStyles.secondaryTextColor());
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2,
                (getHeight() + metrics.getAscent()) / 2);
    }

    // ------------------------------------------------------------ apoyo

    private static Color shade(Color base, double intensity) {
        return new Color(clamp(base.getRed() * intensity), clamp(base.getGreen() * intensity),
                clamp(base.getBlue() * intensity), 245);
    }

    private static Color mix(Color a, Color b, double amount) {
        return new Color(clamp(a.getRed() + (b.getRed() - a.getRed()) * amount),
                clamp(a.getGreen() + (b.getGreen() - a.getGreen()) * amount),
                clamp(a.getBlue() + (b.getBlue() - a.getBlue()) * amount));
    }

    private static int clamp(double value) {
        return (int) Math.max(0, Math.min(255, Math.round(value)));
    }

    private static double[] subtract(double[] a, double[] b) {
        return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] normalize(double[] v) {
        double length = Math.sqrt(dot(v, v));
        return length == 0 ? new double[]{0, 0, 0} : new double[]{v[0] / length, v[1] / length, v[2] / length};
    }

    private static String format(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : String.format("%.4g", value);
    }

    private static JButton toolButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setMargin(new Insets(1, 6, 1, 6));
        button.putClientProperty("JButton.buttonType", "roundRect");
        return button;
    }

    private void updateButtons() {
        resetButton.setVisible(solid != null);
        enlargeButton.setVisible(solid != null && enlargeable);
    }

    /** Coloca los botones en la esquina superior derecha. */
    @Override
    public void doLayout() {
        int x = getWidth() - 6;
        for (JButton button : new JButton[]{enlargeButton, resetButton}) {
            if (button.getParent() == this) {
                Dimension preferred = button.getPreferredSize();
                x -= preferred.width;
                button.setBounds(x, 6, preferred.width, preferred.height);
                x -= 4;
            }
        }
    }

    private void openEnlarged() {
        if (enlargedDialog == null) {
            enlarged = new SolidPanel(false);
            enlarged.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            enlargedDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Sólido 3D — Callista",
                    Dialog.ModalityType.MODELESS);
            enlargedDialog.setContentPane(enlarged);
            enlargedDialog.setSize(780, 560);
            enlargedDialog.setLocationRelativeTo(this);
        }
        enlarged.setColors(colors);
        enlarged.showSolid(solid);
        enlargedDialog.setVisible(true);
    }

    // ------------------------------------------------------------ ratón

    private final class MouseHandler extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            dragStart = e.getPoint();
            dragYaw = yaw;
            dragPitch = pitch;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (solid == null || dragStart == null) {
                return;
            }
            yaw = dragYaw + (e.getX() - dragStart.x) * 0.01;
            pitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, dragPitch + (e.getY() - dragStart.y) * 0.01));
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragStart = null;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                resetView();
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (solid == null) {
                return;
            }
            zoom = Math.max(0.2, Math.min(5, zoom * Math.pow(0.9, e.getPreciseWheelRotation())));
            repaint();
        }
    }
}
