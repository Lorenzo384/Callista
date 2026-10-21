package com.callista.view.components;

import com.callista.model.entity.SolutionStep;
import com.callista.view.ViewStyles;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.util.List;

/**
 * Pestaña «Procedimiento»: los pasos numerados del cálculo. Los pasos que
 * detallan a otro (por ejemplo, la derivada de cada término de una suma)
 * aparecen con sangría.
 *
 * <p>Es un componente propio (no un formulario) porque solo envuelve un área
 * de texto; se coloca en {@code MainFrame.form} como componente personalizado.</p>
 */
public class StepsPanel extends JPanel {

    private static final String INDENT = "   ";
    private static final String PLACEHOLDER = "Aquí aparecerá el procedimiento del cálculo.";

    private final JTextArea stepsArea = new JTextArea();

    public StepsPanel() {
        super(new BorderLayout());
        stepsArea.setEditable(false);
        stepsArea.setLineWrap(true);
        stepsArea.setWrapStyleWord(true);
        stepsArea.setFont(ViewStyles.MATH_SMALL_FONT);
        stepsArea.setMargin(new Insets(6, 8, 6, 8));
        add(new JScrollPane(stepsArea), BorderLayout.CENTER);
        clear();
    }

    public void showSteps(List<SolutionStep> steps) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            SolutionStep step = steps.get(i);
            if (i > 0) {
                text.append('\n');
            }
            text.append(INDENT.repeat(step.getLevel()))
                    .append(i + 1).append(". ").append(step.getDescription());
        }
        stepsArea.setText(text.toString());
        stepsArea.setCaretPosition(0);
    }

    public void clear() {
        stepsArea.setText(PLACEHOLDER);
    }
}
