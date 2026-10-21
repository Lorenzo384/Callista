package com.callista;

import com.callista.controller.CalculatorController;
import com.callista.model.CalculatorModel;
import com.callista.view.MainFrame;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame view = new MainFrame();
            CalculatorModel model = new CalculatorModel();

            // Unir la vista y el modelo mediante el controlador
            new CalculatorController(view, model);

            // Mostrar la ventana
            view.setVisible(true);
        });
    }
}