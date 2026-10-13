package view;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    // Declarar componentes principales como atributos
    public JTextField txtExpresion = new JTextField(15);
    public JButton btnCalcular = new JButton("Calcular");
    public JLabel lblResultado = new JLabel("Resultado: ");

    public MainFrame() {
        setTitle("Calculadora de Derivadas e Integrales");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        // Agregar los componentes a la ventana
        add(new JLabel("Expresión:"));
        add(txtExpresion);
        add(btnCalcular);
        add(lblResultado);

        setLocationRelativeTo(null); // Centrar en pantalla
    }
}