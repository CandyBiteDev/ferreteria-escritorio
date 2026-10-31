package com.umg.ferreteria.memoria;

import com.umg.ferreteria.excepcion.ValidacionException;
import com.umg.ferreteria.modelo.*;
import com.umg.ferreteria.vista.Ui;
import com.umg.ferreteria.vista.WrapLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ENTREGA 2: interfaz Swing que trabaja solo con listas en memoria (sin MySQL).
 * Navegación por pestañas, formularios y operaciones funcionales con las reglas de negocio.
 */
public class VentanaMemoria extends JFrame {

    private final InventarioMemoria inv;

    // Productos
    private final DefaultTableModel mProductos = Ui.modelo("ID", "Código", "Producto", "Categoría", "Marca", "Unidad", "Costo", "Precio", "Mínimo");
    private final JTable tProductos = Ui.tabla(mProductos);
    private final JTextField txtBuscar = new JTextField(20);
    private final JTextField txtCodigo = new JTextField();
    private final JTextField txtNombre = new JTextField();
    private final JComboBox<Categoria> cbCategoria = new JComboBox<>();
    private final JComboBox<Marca> cbMarca = new JComboBox<>();
    private final JComboBox<UnidadMedida> cbUnidad = new JComboBox<>();
    private final JTextField txtCosto = new JTextField();
    private final JTextField txtPrecio = new JTextField();
    private final JTextField txtMinimo = new JTextField("0");
    private Producto seleccionado;

    // Existencias
    private final JComboBox<Object> cbBodegaStock = new JComboBox<>();
    private final DefaultTableModel mExistencias = Ui.modelo("Bodega", "Código", "Producto", "Existencia", "Mínimo", "Estado");

    // Ventas
    private final JComboBox<Cliente> cbCliente = new JComboBox<>();
    private final JComboBox<Bodega> cbBodegaVenta = new JComboBox<>();
    private final JComboBox<Producto> cbProdVenta = new JComboBox<>();
    private final JTextField txtCantVenta = new JTextField(6);
    private final DefaultTableModel mLineas = Ui.modelo("Código", "Producto", "Cantidad");
    private final List<DetalleVenta> lineas = new ArrayList<>();
    private final DefaultTableModel mVentas = Ui.modelo("No.", "Fecha", "Cliente", "Bodega", "Productos", "Total");

    // Transferencias
    private final JComboBox<Bodega> cbOrigen = new JComboBox<>();
    private final JComboBox<Bodega> cbDestino = new JComboBox<>();
    private final JComboBox<Producto> cbProdTrf = new JComboBox<>();
    private final JTextField txtCantTrf = new JTextField(6);
    private final DefaultTableModel mTransferencias = Ui.modelo("No.", "Enviada", "Origen", "Destino", "Productos", "Estado");
    private final JTable tTransferencias = Ui.tabla(mTransferencias);

    // Terceros (polimorfismo)
    private final DefaultTableModel mTerceros = Ui.modelo("Tipo (getTipoPersona)", "Nombre", "NIT", "Clase Java");

    public VentanaMemoria(InventarioMemoria inv) {
        super("Ferretería Multisucursal — Modo en memoria (Entrega 2)");
        this.inv = inv;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 720);
        setLocationRelativeTo(null);

        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Ui.INFO);
        barra.setBorder(new EmptyBorder(10, 16, 10, 16));
        JLabel titulo = new JLabel("Ferretería Multisucursal — versión con listas de objetos (ArrayList)");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 17f));
        JLabel aviso = new JLabel("Sin base de datos: los datos se pierden al cerrar");
        aviso.setForeground(Color.WHITE);
        barra.add(titulo, BorderLayout.WEST);
        barra.add(aviso, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Productos", panelProductos());
        tabs.addTab("Existencias", panelExistencias());
        tabs.addTab("Ventas", panelVentas());
        tabs.addTab("Transferencias", panelTransferencias());
        tabs.addTab("Clientes y proveedores", panelTerceros());
        tabs.addChangeListener(e -> refrescarTodo());

        JPanel contenido = new JPanel(new BorderLayout());
        contenido.setBorder(new EmptyBorder(8, 8, 8, 8));
        contenido.add(tabs);
        getContentPane().add(barra, BorderLayout.NORTH);
        getContentPane().add(contenido, BorderLayout.CENTER);
        refrescarTodo();
    }

    private static JPanel pagina(String titulo, String subtitulo) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(new EmptyBorder(12, 12, 12, 12));
        p.add(Ui.encabezado(titulo, subtitulo), BorderLayout.NORTH);
        return p;
    }

    // ---------- Productos (altas, bajas, cambios y búsqueda) ----------
    private JPanel panelProductos() {
        JPanel p = pagina("Productos", "Se guardan en un RepositorioMemoria<Producto> (ArrayList). Código único y validaciones del modelo.");
        JPanel filtros = WrapLayout.barra();
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar...");
        filtros.add(new JLabel("Buscar:"));
        filtros.add(txtBuscar);
        txtBuscar.getDocument().addDocumentListener((SimpleDoc) () -> {
            @SuppressWarnings("unchecked")
            TableRowSorter<DefaultTableModel> s = (TableRowSorter<DefaultTableModel>) tProductos.getRowSorter();
            String t = txtBuscar.getText().trim();
            s.setRowFilter(t.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(t)));
        });

        Ui.Formulario f = new Ui.Formulario().campo("Código", txtCodigo).campo("Nombre", txtNombre)
                .campo("Categoría", cbCategoria).campo("Marca", cbMarca).campo("Unidad", cbUnidad)
                .campo("Costo (Q)", txtCosto).campo("Precio venta (Q)", txtPrecio).campo("Stock mínimo", txtMinimo).relleno();
        JButton nuevo = Ui.boton("Nuevo");
        JButton guardar = Ui.boton("Guardar", Ui.EXITO);
        JButton eliminar = Ui.boton("Eliminar", Ui.PELIGRO);
        nuevo.addActionListener(e -> limpiarProducto());
        guardar.addActionListener(e -> Ui.accion(this, this::guardarProducto));
        eliminar.addActionListener(e -> Ui.accion(this, () -> {
            if (seleccionado == null) throw new ValidacionException("Seleccione un producto de la tabla.");
            if (!Ui.confirmar(this, "¿Eliminar " + seleccionado + "?")) return;
            inv.eliminarProducto(seleccionado.getId());
            limpiarProducto();
            refrescarTodo();
        }));
        JPanel botones = new JPanel(new GridLayout(1, 3, 6, 0));
        botones.add(nuevo);
        botones.add(guardar);
        botones.add(eliminar);
        JPanel lateral = new JPanel(new BorderLayout(0, 10));
        lateral.setPreferredSize(new Dimension(330, 0));
        lateral.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Detalle / edición"),
                new EmptyBorder(6, 8, 8, 8)));
        lateral.add(f, BorderLayout.CENTER);
        lateral.add(botones, BorderLayout.SOUTH);

        tProductos.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int i = Ui.filaSeleccionada(tProductos);
            if (i < 0) return;
            int id = (Integer) mProductos.getValueAt(i, 0);
            seleccionado = inv.productos().stream().filter(x -> x.getId() == id).findFirst().orElse(null);
            if (seleccionado == null) return;
            txtCodigo.setText(seleccionado.getCodigo());
            txtNombre.setText(seleccionado.getNombre());
            Ui.seleccionar(cbCategoria, seleccionado.getIdCategoria());
            Ui.seleccionar(cbMarca, seleccionado.getIdMarca());
            Ui.seleccionar(cbUnidad, seleccionado.getIdUnidad());
            txtCosto.setText(seleccionado.getPrecioCosto().toPlainString());
            txtPrecio.setText(seleccionado.getPrecioVenta().toPlainString());
            txtMinimo.setText(String.valueOf(seleccionado.getStockMinimo()));
        });

        JPanel centro = new JPanel(new BorderLayout(12, 8));
        centro.add(filtros, BorderLayout.NORTH);
        centro.add(new JScrollPane(tProductos), BorderLayout.CENTER);
        centro.add(lateral, BorderLayout.EAST);
        p.add(centro, BorderLayout.CENTER);
        return p;
    }

    private void guardarProducto() {
        Producto pr = new Producto();
        if (seleccionado != null) pr.setId(seleccionado.getId());
        pr.setCodigo(txtCodigo.getText());
        pr.setNombre(txtNombre.getText().trim());
        pr.setIdCategoria(Ui.idDe(cbCategoria));
        pr.setIdMarca(Ui.idDe(cbMarca));
        pr.setIdUnidad(Ui.idDe(cbUnidad));
        pr.setPrecioCosto(Ui.decimal(txtCosto, "el costo"));
        pr.setPrecioVenta(Ui.decimal(txtPrecio, "el precio de venta"));
        pr.setStockMinimo(Ui.entero(txtMinimo, "el stock mínimo"));
        boolean nuevo = seleccionado == null;
        inv.guardarProducto(pr);
        limpiarProducto();
        refrescarTodo();
        Ui.info(this, nuevo ? "Producto agregado a la lista." : "Producto actualizado.");
    }

    private void limpiarProducto() {
        tProductos.clearSelection();
        seleccionado = null;
        txtCodigo.setText("");
        txtNombre.setText("");
        txtCosto.setText("");
        txtPrecio.setText("");
        txtMinimo.setText("0");
    }

    // ---------- Existencias ----------
    private JPanel panelExistencias() {
        JPanel p = pagina("Existencias por bodega", "RepositorioMemoria<Existencia>. Estado BAJO MÍNIMO cuando la existencia no alcanza el mínimo.");
        JPanel filtros = WrapLayout.barra();
        filtros.add(new JLabel("Bodega:"));
        filtros.add(cbBodegaStock);
        cbBodegaStock.addActionListener(e -> refrescarExistencias());
        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(filtros, BorderLayout.NORTH);
        JTable t = Ui.tabla(mExistencias);
        centro.add(new JScrollPane(t), BorderLayout.CENTER);
        p.add(centro, BorderLayout.CENTER);
        return p;
    }

    // ---------- Ventas ----------
    private JPanel panelVentas() {
        JPanel p = pagina("Ventas", "Regla: no se vende más de lo que hay. La venta se aplica completa o no se aplica.");
        JPanel form = WrapLayout.barra();
        form.add(new JLabel("Cliente:"));
        form.add(cbCliente);
        form.add(new JLabel("Bodega:"));
        form.add(cbBodegaVenta);
        form.add(new JLabel("Producto:"));
        form.add(cbProdVenta);
        form.add(new JLabel("Cantidad:"));
        form.add(txtCantVenta);
        JButton agregar = Ui.boton("Agregar línea", Ui.INFO);
        JButton quitar = Ui.boton("Limpiar líneas");
        JButton confirmar = Ui.boton("Confirmar venta", Ui.EXITO);
        form.add(agregar);
        form.add(quitar);
        form.add(confirmar);
        agregar.addActionListener(e -> Ui.accion(this, () -> {
            Producto pr = (Producto) cbProdVenta.getSelectedItem();
            if (pr == null) throw new ValidacionException("Seleccione un producto.");
            DetalleVenta d = new DetalleVenta();
            d.setIdProducto(pr.getId());
            d.setCodigo(pr.getCodigo());
            d.setProducto(pr.getNombre());
            d.setCantidad(Ui.decimal(txtCantVenta, "la cantidad"));
            lineas.add(d);
            mLineas.addRow(new Object[]{pr.getCodigo(), pr.getNombre(), Ui.cantidad(d.getCantidad())});
            txtCantVenta.setText("");
        }));
        quitar.addActionListener(e -> limpiarLineas());
        confirmar.addActionListener(e -> Ui.accion(this, () -> {
            Venta v = new Venta();
            v.setIdCliente(Ui.idDe(cbCliente));
            v.setIdBodega(Ui.idDe(cbBodegaVenta));
            lineas.forEach(v::agregarDetalle);
            Venta guardada = inv.vender(v);
            limpiarLineas();
            refrescarTodo();
            Ui.info(this, "Venta #" + guardada.getId() + " registrada. Total: " + Ui.moneda(guardada.getTotal()));
        }));

        JPanel lineasPanel = new JPanel(new BorderLayout());
        lineasPanel.setBorder(BorderFactory.createTitledBorder("Líneas de la venta actual (composición: la venta es dueña de sus detalles)"));
        lineasPanel.add(new JScrollPane(Ui.tabla(mLineas)));
        JPanel ventasPanel = new JPanel(new BorderLayout());
        ventasPanel.setBorder(BorderFactory.createTitledBorder("Ventas registradas"));
        ventasPanel.add(new JScrollPane(Ui.tabla(mVentas)));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, lineasPanel, ventasPanel);
        split.setResizeWeight(0.4);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(form, BorderLayout.NORTH);
        centro.add(split, BorderLayout.CENTER);
        p.add(centro, BorderLayout.CENTER);
        return p;
    }

    private void limpiarLineas() {
        lineas.clear();
        mLineas.setRowCount(0);
    }

    // ---------- Transferencias ----------
    private JPanel panelTransferencias() {
        JPanel p = pagina("Transferencias", "Regla: la salida (EN_TRANSITO) y la recepción (RECIBIDA) son pasos separados.");
        JPanel form = WrapLayout.barra();
        form.add(new JLabel("Origen:"));
        form.add(cbOrigen);
        form.add(new JLabel("Destino:"));
        form.add(cbDestino);
        form.add(new JLabel("Producto:"));
        form.add(cbProdTrf);
        form.add(new JLabel("Cantidad:"));
        form.add(txtCantTrf);
        JButton enviar = Ui.boton("Enviar", Ui.PRIMARIO);
        JButton recibir = Ui.boton("Recibir seleccionada", Ui.EXITO);
        form.add(enviar);
        form.add(recibir);
        enviar.addActionListener(e -> Ui.accion(this, () -> {
            Producto pr = (Producto) cbProdTrf.getSelectedItem();
            if (pr == null) throw new ValidacionException("Seleccione un producto.");
            Transferencia t = new Transferencia();
            t.setIdBodegaOrigen(Ui.idDe(cbOrigen));
            t.setIdBodegaDestino(Ui.idDe(cbDestino));
            DetalleTransferencia d = new DetalleTransferencia();
            d.setIdProducto(pr.getId());
            d.setCantidad(Ui.decimal(txtCantTrf, "la cantidad"));
            t.agregarDetalle(d);
            Transferencia enviada = inv.enviar(t);
            txtCantTrf.setText("");
            refrescarTodo();
            Ui.info(this, "Transferencia #" + enviada.getId() + " EN TRÁNSITO: ya salió de " + enviada.getBodegaOrigen() + ".");
        }));
        recibir.addActionListener(e -> Ui.accion(this, () -> {
            int i = Ui.filaSeleccionada(tTransferencias);
            if (i < 0) throw new ValidacionException("Seleccione una transferencia de la tabla.");
            Transferencia t = inv.recibir((Integer) mTransferencias.getValueAt(i, 0));
            refrescarTodo();
            Ui.info(this, "Transferencia #" + t.getId() + " RECIBIDA en " + t.getBodegaDestino() + ".");
        }));
        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(form, BorderLayout.NORTH);
        centro.add(new JScrollPane(tTransferencias), BorderLayout.CENTER);
        p.add(centro, BorderLayout.CENTER);
        return p;
    }

    // ---------- Polimorfismo ----------
    private JPanel panelTerceros() {
        JPanel p = pagina("Clientes y proveedores",
                "Una sola List<Persona> con objetos Cliente y Proveedor: cada uno responde getTipoPersona() a su manera (herencia + polimorfismo).");
        p.add(new JScrollPane(Ui.tabla(mTerceros)), BorderLayout.CENTER);
        return p;
    }

    // ---------- Refresco ----------
    private void refrescarTodo() {
        mProductos.setRowCount(0);
        for (Producto pr : inv.productos()) {
            mProductos.addRow(new Object[]{pr.getId(), pr.getCodigo(), pr.getNombre(), pr.getCategoria(), pr.getMarca(), pr.getUnidad(),
                    Ui.moneda(pr.getPrecioCosto()), Ui.moneda(pr.getPrecioVenta()), pr.getStockMinimo()});
        }
        recargarSiCambia(cbCategoria, inv.categorias());
        recargarSiCambia(cbMarca, inv.marcas());
        recargarSiCambia(cbUnidad, inv.unidades());
        recargarSiCambia(cbCliente, inv.clientes());
        recargarSiCambia(cbBodegaVenta, inv.bodegas());
        recargarSiCambia(cbOrigen, inv.bodegas());
        recargarSiCambia(cbDestino, inv.bodegas());
        if (cbDestino.getItemCount() > 1 && cbDestino.getSelectedIndex() == cbOrigen.getSelectedIndex()) cbDestino.setSelectedIndex(1);
        Ui.recargar(cbProdVenta, inv.productos());
        Ui.recargar(cbProdTrf, inv.productos());
        if (cbBodegaStock.getItemCount() == 0) {
            cbBodegaStock.addItem("Todas las bodegas");
            inv.bodegas().forEach(cbBodegaStock::addItem);
        }
        refrescarExistencias();

        mVentas.setRowCount(0);
        for (Venta v : inv.ventas()) {
            mVentas.addRow(new Object[]{v.getId(), Ui.fecha(v.getFecha()), v.getCliente(), v.getBodega(),
                    v.getDetalles().stream().map(d -> Ui.cantidad(d.getCantidad()) + " " + d.getProducto()).toList(), Ui.moneda(v.getTotal())});
        }
        mTransferencias.setRowCount(0);
        for (Transferencia t : inv.transferencias()) {
            mTransferencias.addRow(new Object[]{t.getId(), Ui.fecha(t.getFecha()), t.getBodegaOrigen(), t.getBodegaDestino(),
                    t.getDetalles().stream().map(d -> Ui.cantidad(d.getCantidad()) + " " + d.getProducto()).toList(), t.getEstado()});
        }
        mTerceros.setRowCount(0);
        for (Persona pe : inv.terceros()) {
            mTerceros.addRow(new Object[]{pe.getTipoPersona(), pe.getNombre(), pe.getNit(), pe.getClass().getSimpleName()});
        }
    }

    private void refrescarExistencias() {
        Integer idBodega = cbBodegaStock.getSelectedItem() instanceof Bodega b ? b.getId() : null;
        mExistencias.setRowCount(0);
        for (Existencia e : inv.existencias(idBodega)) {
            mExistencias.addRow(new Object[]{e.getBodega(), e.getCodigo(), e.getProducto(), Ui.cantidad(e.getCantidad()),
                    e.getStockMinimo(), e.isBajoMinimo() ? "BAJO MÍNIMO" : "OK"});
        }
    }

    /** Recarga un combo solo si cambió la lista, para no perder la selección del usuario. */
    private static <T> void recargarSiCambia(JComboBox<T> combo, List<T> items) {
        if (combo.getItemCount() == items.size()) return;
        Object sel = combo.getSelectedItem();
        Ui.recargar(combo, items);
        if (sel != null) combo.setSelectedItem(sel);
    }

    /** DocumentListener de un solo método para no repetir los tres eventos. */
    @FunctionalInterface
    private interface SimpleDoc extends javax.swing.event.DocumentListener {
        void cambio();

        default void insertUpdate(javax.swing.event.DocumentEvent e) { cambio(); }
        default void removeUpdate(javax.swing.event.DocumentEvent e) { cambio(); }
        default void changedUpdate(javax.swing.event.DocumentEvent e) { cambio(); }
    }
}
