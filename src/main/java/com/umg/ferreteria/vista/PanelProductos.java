package com.umg.ferreteria.vista;

import com.umg.ferreteria.dao.Dao;
import com.umg.ferreteria.modelo.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PanelProductos extends PanelCrud<Producto> {

    private final JTextField txtCodigo = new JTextField();
    private final JTextField txtNombre = new JTextField();
    private final JTextField txtDescripcion = new JTextField();
    private final JComboBox<Categoria> cbCategoria = new JComboBox<>();
    private final JComboBox<Marca> cbMarca = new JComboBox<>();
    private final JComboBox<UnidadMedida> cbUnidad = new JComboBox<>();
    private final JTextField txtCosto = new JTextField();
    private final JTextField txtPrecio = new JTextField();
    private final JTextField txtMinimo = new JTextField("0");
    private final JCheckBox chkActivo = new JCheckBox("Activo", true);
    private JComboBox<Object> cbFiltroCategoria;

    public PanelProductos(AppEscritorio app, Usuario u, boolean editable) {
        super(app, u, "Productos", "Catálogo de artículos con precios, unidad base y stock mínimo", editable,
                "ID", "Código", "Producto", "Categoría", "Marca", "Unidad", "Costo", "Precio", "Margen %", "Mínimo", "Activo");
        inicializar();
    }

    @Override
    protected Dao<Producto> dao() { return app.catalogo().productos(); }

    @Override
    protected void accionesExtra(JPanel barra) {
        cbFiltroCategoria = new JComboBox<>();
        barra.add(new JLabel("Categoría:"));
        barra.add(cbFiltroCategoria);
        cbFiltroCategoria.addActionListener(e -> {
            if (cbFiltroCategoria.isShowing()) Ui.accion(this, this::refrescar);
        });
        JButton btnConv = Ui.boton("Conversiones de unidad", Ui.INFO);
        btnConv.addActionListener(e -> Ui.accion(this, this::abrirConversiones));
        barra.add(btnConv);
    }

    @Override
    public void refrescar() {
        List<Categoria> cats = app.catalogo().listar(app.catalogo().categorias());
        Ui.recargar(cbCategoria, cats);
        Ui.recargar(cbMarca, app.catalogo().listar(app.catalogo().marcas()));
        Ui.recargar(cbUnidad, app.catalogo().listar(app.catalogo().unidades()));
        if (cbFiltroCategoria.getItemCount() == 0) {
            cbFiltroCategoria.addItem("Todas");
            cats.forEach(cbFiltroCategoria::addItem);
        }
        super.refrescar();
    }

    @Override
    protected boolean pasaFiltro(Producto p) {
        Object sel = cbFiltroCategoria.getSelectedItem();
        return !(sel instanceof Categoria c) || c.getId().equals(p.getIdCategoria());
    }

    @Override
    protected Object[] fila(Producto p) {
        return new Object[]{p.getId(), p.getCodigo(), p.getNombre(), p.getCategoria(), p.getMarca(), p.getUnidad(),
                Ui.moneda(p.getPrecioCosto()), Ui.moneda(p.getPrecioVenta()), p.getMargen(), p.getStockMinimo(), p.isActivo() ? "Sí" : "No"};
    }

    @Override
    protected void construirFormulario(Ui.Formulario f) {
        f.campo("Código", txtCodigo).campo("Nombre", txtNombre).campo("Descripción", txtDescripcion)
                .campo("Categoría", cbCategoria).campo("Marca", cbMarca).campo("Unidad base", cbUnidad)
                .campo("Costo (Q)", txtCosto).campo("Precio venta (Q)", txtPrecio).campo("Stock mínimo", txtMinimo)
                .campo("", chkActivo);
    }

    @Override
    protected void llenarFormulario(Producto p) {
        txtCodigo.setText(p == null ? "" : p.getCodigo());
        txtNombre.setText(p == null ? "" : p.getNombre());
        txtDescripcion.setText(p == null ? "" : p.getDescripcion());
        Ui.seleccionar(cbCategoria, p == null ? null : p.getIdCategoria());
        Ui.seleccionar(cbMarca, p == null ? null : p.getIdMarca());
        Ui.seleccionar(cbUnidad, p == null ? null : p.getIdUnidad());
        txtCosto.setText(p == null ? "" : p.getPrecioCosto().toPlainString());
        txtPrecio.setText(p == null ? "" : p.getPrecioVenta().toPlainString());
        txtMinimo.setText(p == null ? "0" : String.valueOf(p.getStockMinimo()));
        chkActivo.setSelected(p == null || p.isActivo());
    }

    @Override
    protected Producto leerFormulario() {
        Producto p = new Producto();
        p.setCodigo(txtCodigo.getText());
        p.setNombre(txtNombre.getText().trim());
        p.setDescripcion(txtDescripcion.getText().trim());
        p.setIdCategoria(Ui.idDe(cbCategoria));
        p.setIdMarca(Ui.idDe(cbMarca));
        p.setIdUnidad(Ui.idDe(cbUnidad));
        p.setPrecioCosto(Ui.decimal(txtCosto, "el costo"));
        p.setPrecioVenta(Ui.decimal(txtPrecio, "el precio de venta"));
        p.setStockMinimo(Ui.entero(txtMinimo, "el stock mínimo"));
        p.setActivo(chkActivo.isSelected());
        return p;
    }

    /** Diálogo para definir unidades equivalentes (regla: solo se vende en unidades con conversión). */
    private void abrirConversiones() {
        if (seleccionado == null) {
            Ui.info(this, "Seleccione un producto de la tabla.");
            return;
        }
        Producto p = seleccionado;
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), "Conversiones — " + p, Dialog.ModalityType.APPLICATION_MODAL);
        DefaultTableModel m = Ui.modelo("ID", "Unidad", "Equivale a", "Unidad base");
        JTable t = Ui.tabla(m);
        Runnable cargar = () -> {
            m.setRowCount(0);
            for (ConversionUnidad c : app.catalogo().conversionesDe(p.getId())) {
                m.addRow(new Object[]{c.getId(), "1 " + c.getUnidad(), Ui.cantidad(c.getFactor()), p.getUnidad()});
            }
        };
        cargar.run();

        JComboBox<UnidadMedida> cb = Ui.combo(app.catalogo().listar(app.catalogo().unidades()));
        JTextField txtFactor = new JTextField(8);
        JButton btnAgregar = Ui.boton("Agregar", Ui.EXITO);
        JButton btnQuitar = Ui.boton("Quitar seleccionada", Ui.PELIGRO);
        btnAgregar.setEnabled(editable);
        btnQuitar.setEnabled(editable);
        btnAgregar.addActionListener(e -> Ui.accion(d, () -> {
            ConversionUnidad c = new ConversionUnidad();
            c.setIdProducto(p.getId());
            c.setIdUnidad(Ui.idDe(cb));
            c.setFactor(Ui.decimal(txtFactor, "el factor"));
            app.catalogo().guardarConversion(c, usuario);
            txtFactor.setText("");
            cargar.run();
        }));
        btnQuitar.addActionListener(e -> Ui.accion(d, () -> {
            int i = Ui.filaSeleccionada(t);
            if (i < 0) return;
            ConversionUnidad c = new ConversionUnidad();
            c.setId((Integer) m.getValueAt(i, 0));
            app.catalogo().eliminar(app.catalogo().conversiones(), c, usuario);
            cargar.run();
        }));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("1"));
        form.add(cb);
        form.add(new JLabel("="));
        form.add(txtFactor);
        form.add(new JLabel(p.getUnidad()));
        form.add(btnAgregar);
        form.add(btnQuitar);

        JPanel raiz = new JPanel(new BorderLayout(0, 8));
        raiz.setBorder(new EmptyBorder(12, 12, 12, 12));
        raiz.add(new JLabel("Unidad base: " + p.getUnidad() + ". Ej.: 1 Caja = 100 Unidad."), BorderLayout.NORTH);
        raiz.add(new JScrollPane(t), BorderLayout.CENTER);
        raiz.add(form, BorderLayout.SOUTH);
        d.setContentPane(raiz);
        d.setSize(640, 360);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }
}
