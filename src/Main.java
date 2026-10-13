import controller.CalculatorController;
import model.CalculatorModel;
import view.MainFrame;

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