package com.callista.view.components;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Comprueba cómo el teclado de funciones edita la expresión.
 * No abre ninguna ventana: solo usa el panel y su campo de texto.
 */
class InputPanelTest {

    private InputPanel panel;
    private JTextField field;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        panel = new InputPanel();
        Field expressionField = InputPanel.class.getDeclaredField("expressionField");
        expressionField.setAccessible(true);
        field = (JTextField) expressionField.get(panel);
    }

    @Test
    void insertsTextAtTheCaret() {
        field.setText("3+1");
        field.setCaretPosition(1);
        panel.insert("x");
        assertEquals("3x+1", panel.getText());
        assertEquals(2, field.getCaretPosition());
    }

    @Test
    void functionsCloseTheParenthesisAndLeaveTheCaretInside() {
        panel.insert("sen(");
        assertEquals("sen()", panel.getText());
        assertEquals(4, field.getCaretPosition());
        panel.insert("x");
        assertEquals("sen(x)", panel.getText());
    }

    @Test
    void functionsWrapTheSelectedText() {
        field.setText("x^2 + 1");
        field.select(0, 3);
        panel.insert("raiz(");
        assertEquals("raiz(x^2) + 1", panel.getText());
        assertEquals(8, field.getCaretPosition());
    }

    @Test
    void backspaceDeletesTheCharacterBeforeTheCaretOrTheSelection() {
        field.setText("3x+1");
        field.setCaretPosition(2);
        panel.deleteBeforeCaret();
        assertEquals("3+1", panel.getText());

        field.select(1, 3);
        panel.deleteBeforeCaret();
        assertEquals("3", panel.getText());

        field.setCaretPosition(0);
        panel.deleteBeforeCaret();
        assertEquals("3", panel.getText());
    }
}
