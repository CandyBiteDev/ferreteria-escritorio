package com.umg.ferreteria.vista;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * FlowLayout que pasa los componentes a la siguiente línea cuando no caben
 * y calcula su alto real (FlowLayout normal los deja cortados fuera de la ventana).
 */
public class WrapLayout extends FlowLayout {

    public WrapLayout(int alineacion, int hgap, int vgap) {
        super(alineacion, hgap, vgap);
    }

    /** Panel-barra con este layout que se reacomoda cuando cambia el ancho de la ventana. */
    public static JPanel barra() {
        JPanel p = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        p.addComponentListener(new ComponentAdapter() {
            private int anchoAnterior = -1;

            @Override
            public void componentResized(ComponentEvent e) {
                if (p.getWidth() != anchoAnterior) {
                    anchoAnterior = p.getWidth();
                    SwingUtilities.invokeLater(p::revalidate);
                }
            }
        });
        return p;
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return medir(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension d = medir(target, false);
        d.width -= getHgap() + 1;
        return d;
    }

    private Dimension medir(Container target, boolean preferido) {
        synchronized (target.getTreeLock()) {
            // Ancho disponible: el del panel, o el del primer contenedor padre que ya tenga tamaño
            Container c = target;
            while (c.getWidth() == 0 && c.getParent() != null) c = c.getParent();
            int anchoDisponible = c.getWidth() == 0 ? Integer.MAX_VALUE : c.getWidth();

            Insets in = target.getInsets();
            int margenes = in.left + in.right + getHgap() * 2;
            int maximo = anchoDisponible - margenes;

            Dimension total = new Dimension(0, 0);
            int anchoFila = 0;
            int altoFila = 0;
            for (Component comp : target.getComponents()) {
                if (!comp.isVisible()) continue;
                Dimension d = preferido ? comp.getPreferredSize() : comp.getMinimumSize();
                if (anchoFila > 0 && anchoFila + getHgap() + d.width > maximo) {
                    agregarFila(total, anchoFila, altoFila);
                    anchoFila = 0;
                    altoFila = 0;
                }
                if (anchoFila > 0) anchoFila += getHgap();
                anchoFila += d.width;
                altoFila = Math.max(altoFila, d.height);
            }
            agregarFila(total, anchoFila, altoFila);
            total.width += margenes;
            total.height += in.top + in.bottom + getVgap() * 2;
            return total;
        }
    }

    private void agregarFila(Dimension total, int anchoFila, int altoFila) {
        total.width = Math.max(total.width, anchoFila);
        if (total.height > 0) total.height += getVgap();
        total.height += altoFila;
    }
}
