package model;

public class CalculatorModel {

    // Método simulado temporal
    public String procesarCalculo(String expresion) {
        if (expresion.isEmpty()) {
            return "Por favor ingresa una expresión.";
        }
        return "Procesado correctamente: " + expresion;
    }
}