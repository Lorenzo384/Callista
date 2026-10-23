import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera los íconos de cada sistema a partir del diseño cuadrado
 * {@code src/main/resources/icon/callista.png}:
 *
 * <ul>
 *   <li>{@code src/main/resources/icon/callista-macos.png}: versión con la
 *       forma de macOS (cuadro redondeado de 824 px centrado en 1024 px con
 *       margen transparente), para el Dock cuando se corre desde IntelliJ.</li>
 *   <li>{@code packaging/macos/Callista.icns}: la misma versión en todos los
 *       tamaños, armada con {@code iconutil}.</li>
 *   <li>{@code packaging/windows/Callista.ico}: el diseño cuadrado en
 *       16–256 px (entradas PNG).</li>
 * </ul>
 *
 * Uso (desde la raíz del proyecto, solo en macOS por {@code iconutil}):
 * {@code java packaging/GenIcons.java}
 */
public class GenIcons {

    private static final Path SOURCE = Path.of("src/main/resources/icon/callista.png");
    private static final Path MAC_PNG = Path.of("src/main/resources/icon/callista-macos.png");
    private static final Path ICNS = Path.of("packaging/macos/Callista.icns");
    private static final Path ICO = Path.of("packaging/windows/Callista.ico");

    // Retícula de íconos de macOS (Big Sur en adelante)
    private static final int CANVAS = 1024;
    private static final int TILE = 824;
    private static final int CORNER_RADIUS = 185;

    public static void main(String[] args) throws Exception {
        BufferedImage source = ImageIO.read(SOURCE.toFile());
        if (source == null || source.getWidth() != source.getHeight()) {
            throw new IllegalArgumentException("El icono debe ser un PNG cuadrado: " + SOURCE);
        }

        BufferedImage mac = macShape(source);
        ImageIO.write(mac, "png", MAC_PNG.toFile());
        writeIcns(mac);
        writeIco(source);
        System.out.println("Listo: " + MAC_PNG + ", " + ICNS + ", " + ICO);
    }

    /** Recorta el diseño en un cuadro redondeado con margen transparente. */
    private static BufferedImage macShape(BufferedImage source) {
        BufferedImage result = new BufferedImage(CANVAS, CANVAS, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = quality(result.createGraphics());
        int offset = (CANVAS - TILE) / 2;
        // Primero la forma (con antialias) y luego el diseño solo dentro de ella
        g.fill(new RoundRectangle2D.Double(offset, offset, TILE, TILE,
                2 * CORNER_RADIUS, 2 * CORNER_RADIUS));
        g.setComposite(AlphaComposite.SrcIn);
        g.drawImage(source, offset, offset, TILE, TILE, null);
        g.dispose();
        return result;
    }

    private static void writeIcns(BufferedImage mac) throws IOException, InterruptedException {
        Path iconset = Files.createTempDirectory("callista").resolve("Callista.iconset");
        Files.createDirectories(iconset);
        for (int size : new int[]{16, 32, 128, 256, 512}) {
            ImageIO.write(scale(mac, size), "png", iconset.resolve("icon_" + size + "x" + size + ".png").toFile());
            ImageIO.write(scale(mac, 2 * size), "png", iconset.resolve("icon_" + size + "x" + size + "@2x.png").toFile());
        }
        Files.createDirectories(ICNS.getParent());
        Process process = new ProcessBuilder("iconutil", "-c", "icns", iconset.toString(), "-o", ICNS.toString())
                .inheritIO().start();
        if (process.waitFor() != 0) {
            throw new IOException("iconutil falló");
        }
    }

    /** Formato ICO con entradas PNG (Windows Vista en adelante). */
    private static void writeIco(BufferedImage source) throws IOException {
        int[] sizes = {16, 24, 32, 48, 64, 128, 256};
        List<byte[]> images = new ArrayList<>();
        for (int size : sizes) {
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(scale(source, size), "png", png);
            images.add(png.toByteArray());
        }

        ByteBuffer header = ByteBuffer.allocate(6 + 16 * sizes.length).order(ByteOrder.LITTLE_ENDIAN);
        header.putShort((short) 0).putShort((short) 1).putShort((short) sizes.length);
        int offset = header.capacity();
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i] == 256 ? 0 : sizes[i]; // 0 significa 256
            header.put((byte) size).put((byte) size).put((byte) 0).put((byte) 0);
            header.putShort((short) 1).putShort((short) 32);
            header.putInt(images.get(i).length).putInt(offset);
            offset += images.get(i).length;
        }

        Files.createDirectories(ICO.getParent());
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(ICO.toFile()))) {
            out.write(header.array());
            for (byte[] image : images) {
                out.write(image);
            }
        }
    }

    /** Reduce a la mitad sucesivamente para que los tamaños pequeños no se vean dentados. */
    private static BufferedImage scale(BufferedImage image, int size) {
        BufferedImage current = image;
        while (current.getWidth() / 2 >= size) {
            current = resize(current, current.getWidth() / 2);
        }
        return current.getWidth() == size ? current : resize(current, size);
    }

    private static BufferedImage resize(BufferedImage image, int size) {
        BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = quality(result.createGraphics());
        g.drawImage(image, 0, 0, size, size, null);
        g.dispose();
        return result;
    }

    private static Graphics2D quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }
}
