package com.callista.view;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

/**
 * Fuentes y colores comunes de la interfaz. La apariencia general la da
 * FlatLaf Light; aquí solo se definen los detalles propios de la calculadora.
 */
public final class ViewStyles {

    /** Fuente de la «pantalla» donde se escribe la función. */
    public static final Font MATH_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 15);
    /** Fuente de la interpretación y del procedimiento. */
    public static final Font MATH_SMALL_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 13);
    /** Fuente del resultado. */
    public static final Font RESULT_FONT = new Font(Font.MONOSPACED, Font.BOLD, 22);
    /** Fuente del teclado de funciones. */
    public static final Font KEY_FONT = new Font(Font.DIALOG, Font.PLAIN, 14);

    /** Único color de acento: los mensajes de error. */
    public static final Color ERROR_COLOR = new Color(0xA3, 0x1F, 0x1F);

    private ViewStyles() {
    }

    /**
     * Gris de los textos secundarios (ayudas y barra de estado).
     *
     * <p>Se devuelve una copia que no es {@code UIResource}: así el color se
     * conserva cuando se cambia la paleta y Swing reinstala los colores del tema.</p>
     */
    public static Color secondaryTextColor() {
        Color color = UIManager.getColor("Label.disabledForeground");
        return color != null ? new Color(color.getRGB()) : Color.GRAY;
    }
}
