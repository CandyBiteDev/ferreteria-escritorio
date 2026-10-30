package com.umg.ferreteria.vista;

import com.umg.ferreteria.dao.Dao;
import com.umg.ferreteria.modelo.EntidadBase;
import com.umg.ferreteria.modelo.Usuario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Pantalla genérica de ABC (altas, bajas y cambios) con búsqueda.
 * Cada subclase define columnas, formulario y DAO (método plantilla + polimorfismo).
 * Las subclases deben llamar inicializar() al final de su constructor.
 */
public abstract class PanelCrud<T extends EntidadBase> extends JPanel {

    protected final AppEscritorio app;
    protected final Usuario usuario;
    protected final boolean editable;
    protected final DefaultTableModel modelo;
    protected final JTable tabla;
    protected final JTextField txtBuscar = new JTextField(22);
    protected final JPanel barraFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    protected List<T> datos = new ArrayList<>();
    protected T seleccionado;
    private final String titulo;
    private final String subtitulo;

    protected PanelCrud(AppEscritorio app, Usuario usuario, String titulo, String subtitulo, boolean editable, String... columnas) {
        super(new BorderLayout(12, 8));
        this.app = app;
        this.usuario = usuario;
        this.editable = editable;
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.modelo = Ui.modelo(columnas);
        this.tabla = Ui.tabla(modelo);
        setBorder(new EmptyBorder(12, 12, 12, 12));
    }

    // ---------- Puntos de extensión ----------
    protected abstract Dao<T> dao();

    protected abstract Object[] fila(T e);

    protected abstract void construirFormulario(Ui.Formulario f);

    /** Muestra la entidad en el formulario; null = limpiar. */
    protected abstract void llenarFormulario(T e);

    /** Crea una entidad nueva con lo escrito en el formulario (sin id). */
    protected abstract T leerFormulario();

    protected List<T> cargar() {
        return app.catalogo().listar(dao());
    }

    protected void guardar(T e) {
        app.catalogo().guardar(dao(), e, usuario);
    }

    protected void eliminar(T e) {
        app.catalogo().eliminar(dao(), e, usuario);
    }

    /** Hook para agregar botones propios en la barra superior. */
    protected void accionesExtra(JPanel barra) {
    }

    /** Filtro adicional a la búsqueda de texto (por defecto ninguno). */
    protected boolean pasaFiltro(T e) {
        return true;
    }

    // ---------- Construcción ----------
    protected final void inicializar() {
        JPanel norte = new JPanel(new BorderLayout());
        norte.add(Ui.encabezado(titulo, subtitulo), BorderLayout.NORTH);
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar...");
        txtBuscar.putClientProperty("JTextField.showClearButton", true);
        barraFiltros.add(new JLabel("Buscar:"));
        barraFiltros.add(txtBuscar);
        JButton btnRefrescar = Ui.boton("Refrescar");
        btnRefrescar.addActionListener(e -> refrescar());
        barraFiltros.add(btnRefrescar);
        accionesExtra(barraFiltros);
        norte.add(barraFiltros, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);

        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel lateral = new JPanel(new BorderLayout(0, 10));
        lateral.setPreferredSize(new Dimension(340, 0));
        lateral.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(editable ? "Detalle / edición" : "Detalle (solo lectura)"),
                new EmptyBorder(6, 8, 8, 8)));
        Ui.Formulario f = new Ui.Formulario();
        construirFormulario(f);
        f.relleno();
        lateral.add(new JScrollPane(f, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER) {{ setBorder(null); }}, BorderLayout.CENTER);

        if (editable) {
            JPanel botones = new JPanel(new GridLayout(1, 3, 6, 0));
            JButton btnNuevo = Ui.boton("Nuevo");
            JButton btnGuardar = Ui.boton("Guardar", Ui.EXITO);
            JButton btnEliminar = Ui.boton("Eliminar", Ui.PELIGRO);
            btnNuevo.addActionListener(e -> limpiar());
            btnGuardar.addActionListener(e -> Ui.accion(this, this::accionGuardar));
            btnEliminar.addActionListener(e -> Ui.accion(this, this::accionEliminar));
            botones.add(btnNuevo);
            botones.add(btnGuardar);
            botones.add(btnEliminar);
            lateral.add(botones, BorderLayout.SOUTH);
        } else {
            habilitarFormulario(f, false);
        }
        add(lateral, BorderLayout.EAST);

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int i = Ui.filaSeleccionada(tabla);
            seleccionado = i < 0 ? null : datos.get(i);
            if (seleccionado != null) llenarFormulario(seleccionado);
        });
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrar(); }
            public void removeUpdate(DocumentEvent e) { filtrar(); }
            public void changedUpdate(DocumentEvent e) { filtrar(); }
        });
        Ui.accion(this, this::refrescar);
    }

    private static void habilitarFormulario(Container c, boolean on) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JTextField || comp instanceof JComboBox || comp instanceof JCheckBox || comp instanceof JTextArea) {
                comp.setEnabled(on);
            }
            if (comp instanceof Container hijo) habilitarFormulario(hijo, on);
        }
    }

    // ---------- Acciones ----------
    public void refrescar() {
        datos = new ArrayList<>(cargar().stream().filter(this::pasaFiltro).toList());
        modelo.setRowCount(0);
        for (T e : datos) modelo.addRow(fila(e));
        seleccionado = null;
        filtrar();
    }

    protected void filtrar() {
        @SuppressWarnings("unchecked")
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) tabla.getRowSorter();
        String t = txtBuscar.getText().trim();
        sorter.setRowFilter(t.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(t)));
    }

    protected void limpiar() {
        tabla.clearSelection();
        seleccionado = null;
        llenarFormulario(null);
    }

    private void accionGuardar() {
        T e = leerFormulario();
        boolean nuevo = seleccionado == null;
        if (!nuevo) e.setId(seleccionado.getId());
        guardar(e);
        refrescar();
        limpiar();
        Ui.info(this, nuevo ? "Registro creado correctamente." : "Cambios guardados correctamente.");
    }

    private void accionEliminar() {
        if (seleccionado == null) {
            Ui.info(this, "Seleccione un registro de la tabla.");
            return;
        }
        if (!Ui.confirmar(this, "¿Eliminar \"" + seleccionado + "\"?")) return;
        eliminar(seleccionado);
        refrescar();
        limpiar();
    }
}
