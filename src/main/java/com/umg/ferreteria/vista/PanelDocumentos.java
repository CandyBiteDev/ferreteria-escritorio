package com.umg.ferreteria.vista;

import com.umg.ferreteria.modelo.DetalleDocumento;
import com.umg.ferreteria.modelo.Documento;
import com.umg.ferreteria.modelo.Usuario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Pantalla maestro-detalle genérica: arriba los documentos, abajo el detalle del seleccionado.
 * Compras, Ventas y Transferencias heredan de aquí.
 */
public abstract class PanelDocumentos<D extends Documento<? extends DetalleDocumento>> extends JPanel {

    protected final AppEscritorio app;
    protected final Usuario usuario;
    protected final DefaultTableModel modelo;
    protected final JTable tabla;
    protected final DefaultTableModel modeloDetalle;
    protected final JTable tablaDetalle;
    private final JComboBox<String> cbEstado;
    private final JTextField txtBuscar = new JTextField(18);
    private final JLabel lblDetalle = new JLabel("Seleccione un documento para ver su detalle");
    protected List<D> datos = new ArrayList<>();

    protected PanelDocumentos(AppEscritorio app, Usuario usuario, String titulo, String subtitulo,
                              String[] columnas, String[] columnasDetalle, String... estados) {
        super(new BorderLayout(8, 8));
        this.app = app;
        this.usuario = usuario;
        setBorder(new EmptyBorder(12, 12, 12, 12));
        modelo = Ui.modelo(columnas);
        tabla = Ui.tabla(modelo);
        modeloDetalle = Ui.modelo(columnasDetalle);
        tablaDetalle = Ui.tabla(modeloDetalle);
        String[] opciones = new String[estados.length + 1];
        opciones[0] = "Todos";
        System.arraycopy(estados, 0, opciones, 1, estados.length);
        cbEstado = new JComboBox<>(opciones);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.add(new JLabel("Estado:"));
        filtros.add(cbEstado);
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar...");
        filtros.add(txtBuscar);
        JButton btnRefrescar = Ui.boton("Refrescar");
        btnRefrescar.addActionListener(e -> refrescar());
        filtros.add(btnRefrescar);
        filtros.add(new JSeparator(SwingConstants.VERTICAL));
        botones(filtros);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(Ui.encabezado(titulo, subtitulo), BorderLayout.NORTH);
        norte.add(filtros, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);

        JPanel det = new JPanel(new BorderLayout(0, 4));
        lblDetalle.setFont(lblDetalle.getFont().deriveFont(Font.BOLD));
        det.add(lblDetalle, BorderLayout.NORTH);
        det.add(new JScrollPane(tablaDetalle), BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tabla), det);
        split.setResizeWeight(0.6);
        add(split, BorderLayout.CENTER);

        cbEstado.addActionListener(e -> refrescar());
        txtBuscar.addActionListener(e -> filtrarTexto());
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrarTexto(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrarTexto(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrarTexto(); }
        });
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) Ui.accion(this, this::mostrarDetalle);
        });
        refrescar();
    }

    // ---------- Puntos de extensión ----------
    protected abstract List<D> cargar();

    protected abstract Object[] fila(D d);

    /** Recarga el documento con sus detalles. */
    protected abstract D completo(D d);

    protected abstract Object[] filaDetalle(DetalleDocumento d);

    protected abstract void botones(JPanel barra);

    protected String resumen(D d) {
        return d.getTipoDocumento() + " #" + d.getId() + " — " + d.getEstado();
    }

    // ---------- Comunes ----------
    protected D seleccionado() {
        int i = Ui.filaSeleccionada(tabla);
        return i < 0 ? null : datos.get(i);
    }

    /** Devuelve el seleccionado o muestra aviso si no hay. */
    protected D requerirSeleccion() {
        D d = seleccionado();
        if (d == null) Ui.info(this, "Seleccione un documento de la lista.");
        return d;
    }

    public void refrescar() {
        Ui.accion(this, () -> {
            String estado = (String) cbEstado.getSelectedItem();
            datos = new ArrayList<>(cargar().stream().filter(d -> "Todos".equals(estado) || d.getEstado().equals(estado)).toList());
            modelo.setRowCount(0);
            for (D d : datos) modelo.addRow(fila(d));
            modeloDetalle.setRowCount(0);
            lblDetalle.setText("Seleccione un documento para ver su detalle");
            filtrarTexto();
        });
    }

    private void filtrarTexto() {
        @SuppressWarnings("unchecked")
        TableRowSorter<DefaultTableModel> s = (TableRowSorter<DefaultTableModel>) tabla.getRowSorter();
        String t = txtBuscar.getText().trim();
        s.setRowFilter(t.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(t)));
    }

    private void mostrarDetalle() {
        D d = seleccionado();
        modeloDetalle.setRowCount(0);
        if (d == null) return;
        D c = completo(d);
        lblDetalle.setText(resumen(c));
        for (DetalleDocumento det : c.getDetalles()) modeloDetalle.addRow(filaDetalle(det));
    }
}
