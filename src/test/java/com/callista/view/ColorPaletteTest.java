package com.callista.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorPaletteTest {

    /** Nodo de preferencias temporal para no tocar las del usuario. */
    private Preferences preferences;

    @BeforeEach
    void setUp() {
        preferences = Preferences.userRoot().node("callista-test-" + System.nanoTime());
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        preferences.removeNode();
    }

    @Test
    void pastelPurpleIsTheDefault() {
        assertEquals(ColorPalette.PASTEL_PURPLE, ColorPalette.DEFAULT);
        assertEquals(ColorPalette.DEFAULT, ColorPalette.load(preferences));
    }

    @Test
    void remembersTheChosenPalette() {
        ColorPalette.FORMAL_GRAY.save(preferences);
        assertEquals(ColorPalette.FORMAL_GRAY, ColorPalette.load(preferences));
    }

    @Test
    void unknownSavedPaletteFallsBackToTheDefault() {
        preferences.put("palette", "NEON");
        assertEquals(ColorPalette.DEFAULT, ColorPalette.load(preferences));
    }

    @Test
    void everyPaletteDefinesItsAccentWithValidColors() {
        for (ColorPalette palette : ColorPalette.values()) {
            assertNotNull(palette.getLookAndFeelDefaults().get("@accentColor"), palette.name());
            palette.getLookAndFeelDefaults().forEach((key, value) ->
                    assertTrue(value.matches("#[0-9A-Fa-f]{6}"), palette.name() + " " + key + " = " + value));
        }
    }
}
