package com.callista.view;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import java.awt.Color;
import java.util.Map;
import java.util.prefs.Preferences;

import static java.util.Map.entry;

/**
 * Paletas de colores predefinidas de la interfaz.
 *
 * <p>Cada paleta indica a FlatLaf qué colores usar ({@link #install()}) y
 * define los colores propios de la sección «Resultado». Los colores suaves se
 * usan en fondos y rellenos; los textos de acento llevan un tono oscuro del
 * mismo color para que se lean bien.</p>
 *
 * <p>Para agregar una paleta basta con una nueva constante.</p>
 */
public enum ColorPalette {

    PASTEL_PURPLE("Morado y magenta (pastel)", Map.ofEntries(
            entry("@background", "#F6F2FA"),
            entry("@componentBackground", "#FFFFFF"),
            entry("@accentColor", "#9C7BCB"),
            entry("TitledBorder.titleColor", "#5E4386"),
            entry("TabbedPane.underlineColor", "#C2689E"),
            entry("Button.background", "#EFE7F8"),
            entry("Button.hoverBackground", "#E6DAF4"),
            entry("Button.pressedBackground", "#F4CFE3"),
            entry("Button.borderColor", "#D8CAEB"),
            entry("Button.default.background", "#D8C4F0"),
            entry("Button.default.foreground", "#3B2758"),
            entry("Button.default.borderColor", "#B79BDB"),
            entry("Button.default.hoverBackground", "#CDB6EB"),
            entry("Button.default.pressedBackground", "#EDB8D6")),
            new Color(0xFBEAF3), new Color(0x8A2F66),
            new GraphColors(new Color(0x5E4386), new Color(0xC2689E), new Color(0x9C7BCB), new Color(0x8A2F66),
                    new Color(0xECE6F4), new Color(0x6E6680), Color.WHITE,
                    new Color(156, 123, 203, 60), new Color(0xC2689E))),

    FORMAL_GRAY("Gris formal", Map.of(
            "@accentColor", "#5A5A5A"),
            null, null,
            new GraphColors(new Color(0x222222), new Color(0x6B6B6B), new Color(0x8C8C8C), new Color(0x222222),
                    new Color(0xE6E6E6), new Color(0x555555), Color.WHITE,
                    new Color(90, 90, 90, 45), new Color(0x6B6B6B)));

    /** Paleta con la que se abre la aplicación la primera vez. */
    public static final ColorPalette DEFAULT = PASTEL_PURPLE;

    private static final String PREFERENCE_KEY = "palette";

    private final String displayName;
    private final Map<String, String> lookAndFeelDefaults;
    private final Color resultBackground;
    private final Color resultForeground;
    private final GraphColors graphColors;

    ColorPalette(String displayName, Map<String, String> lookAndFeelDefaults,
                 Color resultBackground, Color resultForeground, GraphColors graphColors) {
        this.displayName = displayName;
        this.lookAndFeelDefaults = lookAndFeelDefaults;
        this.resultBackground = resultBackground;
        this.resultForeground = resultForeground;
        this.graphColors = graphColors;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Colores que se pasan a FlatLaf. Las claves con {@code @} son variables
     * del tema (de ellas se derivan muchos colores); el resto son colores de
     * componentes concretos.
     */
    public Map<String, String> getLookAndFeelDefaults() {
        return lookAndFeelDefaults;
    }

    /**
     * Fondo de la sección «Resultado», o {@code null} para usar el del tema.
     */
    public Color getResultBackground() {
        return resultBackground;
    }

    /**
     * Color del texto del resultado, o {@code null} para usar el del tema.
     */
    public Color getResultForeground() {
        return resultForeground;
    }

    /**
     * Colores de la gráfica.
     */
    public GraphColors getGraphColors() {
        return graphColors;
    }

    /**
     * Activa FlatLaf Light con los colores de esta paleta. Si ya hay ventanas
     * abiertas, después hay que llamar a {@link FlatLaf#updateUI()}.
     */
    public void install() {
        FlatLaf.setGlobalExtraDefaults(lookAndFeelDefaults);
        FlatLightLaf.setup();
    }

    /**
     * Paleta guardada por el usuario, o {@link #DEFAULT} si no hay ninguna
     * (o si se guardó una que ya no existe).
     */
    public static ColorPalette load(Preferences preferences) {
        String name = preferences.get(PREFERENCE_KEY, DEFAULT.name());
        for (ColorPalette palette : values()) {
            if (palette.name().equals(name)) {
                return palette;
            }
        }
        return DEFAULT;
    }

    /**
     * Recuerda esta paleta para la próxima vez que se abra la aplicación.
     */
    public void save(Preferences preferences) {
        preferences.put(PREFERENCE_KEY, name());
    }

    /**
     * Preferencias del usuario para Callista (las guarda el sistema operativo).
     */
    public static Preferences userPreferences() {
        return Preferences.userNodeForPackage(ColorPalette.class);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
