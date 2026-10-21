package com.callista;

import com.callista.controller.CalculatorController;
import com.callista.model.CalculatorModel;
import com.callista.view.MainFrame;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        installLookAndFeel();

        SwingUtilities.invokeLater(() -> {
            MainFrame view = new MainFrame();
            CalculatorModel model = new CalculatorModel();

            // Unir la vista y el modelo mediante el controlador
            new CalculatorController(view, model);

            // Mostrar la ventana
            view.setVisible(true);
        });
    }

    /**
     * Apariencia FlatLaf Light con acento gris en lugar del azul por defecto,
     * para un aspecto sobrio de calculadora.
     */
    private static void installLookAndFeel() {
        FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", "#5A5A5A"));
        FlatLightLaf.setup();
    }
}
