package controller;

import model.CalculatorModel;
import view.MainFrame;

public class CalculatorController {
    private MainFrame view;
    private CalculatorModel model;

    public CalculatorController(MainFrame view, CalculatorModel model) {
        this.view = view;
        this.model = model;

        // Escuchar cuando el usuario hace clic en el botón
        this.view.btnCalcular.addActionListener(e -> ejecutarCalculo());
    }

    private void ejecutarCalculo() {
        // 1. Obtener texto de la vista
        String textoIngresado = view.txtExpresion.getText();

        // 2. Procesar en el modelo
        String resultado = model.procesarCalculo(textoIngresado);

        // 3. Actualizar la vista con la respuesta
        view.lblResultado.setText("Resultado: " + resultado);
    }
}