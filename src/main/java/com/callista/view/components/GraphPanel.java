package com.callista.view.components;

import com.callista.view.ViewStyles;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;

/**
 * Pestaña «Gráfica». Por ahora solo muestra un aviso; el dibujo de la
 * función se implementará en un bocado posterior.
 *
 * <p>Será un componente propio (no un formulario) porque dibuja con
 * {@code paintComponent}; se coloca en {@code MainFrame.form} como
 * componente personalizado.</p>
 */
public class GraphPanel extends JPanel {

    public GraphPanel() {
        super(new BorderLayout());
        JLabel notice = new JLabel("La gráfica estará disponible próximamente.", SwingConstants.CENTER);
        notice.setForeground(ViewStyles.secondaryTextColor());
        add(notice, BorderLayout.CENTER);
    }
}
