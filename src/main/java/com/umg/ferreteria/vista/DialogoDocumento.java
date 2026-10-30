package com.umg.ferreteria.vista;

import com.umg.ferreteria.excepcion.ValidacionException;
import com.umg.ferreteria.modelo.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Captura de documentos encabezado-detalle: compra, venta, cotización o transferencia.
 * La vista solo arma el objeto; las reglas y la transacción las aplica el servicio.
 */
public class DialogoDocumento extends JDialog {

    public enum Tipo { COMPRA, VENTA, COTIZACION, TRANSFERENCIA }

    private record Linea(Producto producto, ConversionUnidad unidad, BigDecimal cantidad, BigDecimal precio) {
        BigDecimal subtotal() {
            return precio == null ? BigDecimal.ZERO : cantidad.multiply(precio);
        }
    }

    private final AppEscritorio app;
    private final Usuario usuario;
    private final Tipo tipo;
    private final List<Linea> lineas = new ArrayList<>();
    private EntidadBase resultado;

    // Encabezado
    private final JComboBox<Proveedor> cbProveedor = new JComboBox<>();
    private final JComboBox<Cliente> cbCliente = new JComboBox<>();
    private final JComboBox<Bodega> cbBodega = new JComboBox<>();
    private final JComboBox<Bodega> cbDestino = new JComboBox<>();
    private final JTextField txtObservacion = new JTextField();

    // Línea
    private final JComboBox<Producto> cbProducto = new JComboBox<>();
    private final JComboBox<ConversionUnidad> cbUnidad = new JComboBox<>();
    private final JTextField txtCantidad = new JTextField(6);
    private final JTextField txtPrecio = new JTextField(8);
    private final JLabel lblDisponible = new JLabel(" ");

    private final DefaultTableModel modelo = Ui.modelo("Código", "Producto", "Unidad", "Cantidad", "Precio/Costo", "Subtotal");
    private final JTable tabla = Ui.tabla(modelo);
    private final JLabel lblTotal = new JLabel("Total: Q 0.00");

    public DialogoDocumento(Component padre, AppEscritorio app, Usuario usuario, Tipo tipo) {
        super(SwingUtilities.getWindowAncestor(padre), titulo(tipo), ModalityType.APPLICATION_MODAL);
        this.app = app;
        this.usuario = usuario;
        this.tipo = tipo;

        List<Bodega> bodegas = app.catalogo().listar(app.catalogo().bodegas()).stream().filter(Bodega::isActivo).toList();
        Ui.recargar(cbBodega, bodegas);
        Ui.recargar(cbDestino, bodegas);
        if (usuario.getIdBodega() != null) Ui.seleccionar(cbBodega, usuario.getIdBodega());
        Ui.recargar(cbProducto, app.catalogo().productosActivos());

        Ui.Formulario cab = new Ui.Formulario();
        switch (tipo) {
            case COMPRA -> {
                Ui.recargar(cbProveedor, app.catalogo().listar(app.catalogo().proveedores()));
                cab.campo("Proveedor", cbProveedor).campo("Bodega que recibe", cbBodega);
            }
            case VENTA, COTIZACION -> {
                Ui.recargar(cbCliente, app.catalogo().listar(app.catalogo().clientes()));
                cab.campo("Cliente", cbCliente).campo("Bodega de despacho", cbBodega);
            }
            case TRANSFERENCIA -> {
                cbDestino.setSelectedIndex(-1);
                cab.campo("Bodega origen", cbBodega).campo("Bodega destino", cbDestino).campo("Observación", txtObservacion);
            }
        }
        cab.setBorder(BorderFactory.createTitledBorder("Encabezado"));

        // Editor de línea
        JPanel editor = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        editor.setBorder(BorderFactory.createTitledBorder("Agregar producto"));
        cbProducto.setPreferredSize(new Dimension(300, cbProducto.getPreferredSize().height));
        editor.add(cbProducto);
        if (usaUnidades()) editor.add(cbUnidad);
        editor.add(new JLabel("Cantidad"));
        editor.add(txtCantidad);
        if (tipo != Tipo.TRANSFERENCIA) {
            editor.add(new JLabel(tipo == Tipo.COMPRA ? "Costo Q" : "Precio Q"));
            editor.add(txtPrecio);
        }
        JButton btnAgregar = Ui.boton("Agregar", Ui.INFO);
        btnAgregar.addActionListener(e -> Ui.accion(this, this::agregarLinea));
        editor.add(btnAgregar);
        JButton btnQuitar = Ui.boton("Quitar línea");
        btnQuitar.addActionListener(e -> quitarLinea());
        editor.add(btnQuitar);
        lblDisponible.setForeground(Ui.INFO);
        editor.add(lblDisponible);
        getRootPane().setDefaultButton(btnAgregar);

        cbProducto.addActionListener(e -> Ui.accion(this, this::productoCambiado));
        cbUnidad.addActionListener(e -> sugerirPrecio());
        cbBodega.addActionListener(e -> Ui.accion(this, this::mostrarDisponible));
        Ui.accion(this, this::productoCambiado);

        // Pie
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 16f));
        JButton btnGuardar = Ui.boton(textoGuardar(tipo), Ui.EXITO);
        btnGuardar.addActionListener(e -> Ui.accion(this, this::guardar));
        JButton btnCancelar = Ui.boton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        JPanel pie = new JPanel(new BorderLayout());
        if (tipo != Tipo.TRANSFERENCIA) pie.add(lblTotal, BorderLayout.WEST);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.add(btnCancelar);
        acciones.add(btnGuardar);
        pie.add(acciones, BorderLayout.EAST);

        JPanel arriba = new JPanel(new BorderLayout(0, 8));
        arriba.add(cab, BorderLayout.NORTH);
        arriba.add(editor, BorderLayout.SOUTH);

        JPanel raiz = new JPanel(new BorderLayout(0, 8));
        raiz.setBorder(new EmptyBorder(12, 12, 12, 12));
        raiz.add(arriba, BorderLayout.NORTH);
        raiz.add(new JScrollPane(tabla), BorderLayout.CENTER);
        raiz.add(pie, BorderLayout.SOUTH);
        setContentPane(raiz);
        setSize(980, 620);
        setLocationRelativeTo(padre);
    }

    private static String titulo(Tipo t) {
        return switch (t) {
            case COMPRA -> "Nueva compra a proveedor";
            case VENTA -> "Nueva venta";
            case COTIZACION -> "Nueva cotización";
            case TRANSFERENCIA -> "Nueva transferencia entre bodegas";
        };
    }

    private static String textoGuardar(Tipo t) {
        return switch (t) {
            case COMPRA -> "Registrar compra";
            case VENTA -> "Confirmar venta";
            case COTIZACION -> "Guardar cotización";
            case TRANSFERENCIA -> "Enviar transferencia";
        };
    }

    private boolean usaUnidades() {
        return tipo == Tipo.VENTA || tipo == Tipo.COTIZACION;
    }

    /** Muestra el diálogo y devuelve el documento guardado (o null si se canceló). */
    public EntidadBase mostrar() {
        setVisible(true);
        return resultado;
    }

    private void productoCambiado() {
        Producto p = (Producto) cbProducto.getSelectedItem();
        if (p == null) return;
        if (usaUnidades()) {
            Ui.recargar(cbUnidad, app.ventas().unidadesDeVenta(p.getId()));
        }
        sugerirPrecio();
        mostrarDisponible();
    }

    private void sugerirPrecio() {
        Producto p = (Producto) cbProducto.getSelectedItem();
        if (p == null) return;
        if (tipo == Tipo.COMPRA) {
            txtPrecio.setText(p.getPrecioCosto().toPlainString());
        } else if (usaUnidades() && cbUnidad.getSelectedItem() instanceof ConversionUnidad u) {
            txtPrecio.setText(p.getPrecioVenta().multiply(u.getFactor()).setScale(2, RoundingMode.HALF_UP).toPlainString());
        }
    }

    private void mostrarDisponible() {
        Producto p = (Producto) cbProducto.getSelectedItem();
        Integer b = Ui.idDe(cbBodega);
        if (p == null || b == null || tipo == Tipo.COMPRA) {
            lblDisponible.setText(" ");
            return;
        }
        lblDisponible.setText("Disponible: " + Ui.cantidad(app.inventario().disponible(p.getId(), b)) + " " + p.getUnidad());
    }

    private void agregarLinea() {
        Producto p = (Producto) cbProducto.getSelectedItem();
        if (p == null) throw new ValidacionException("Seleccione un producto.");
        BigDecimal cant = Ui.decimal(txtCantidad, "la cantidad");
        if (cant.signum() <= 0) throw new ValidacionException("La cantidad debe ser mayor que cero.");
        if (lineas.stream().anyMatch(l -> l.producto().getId().equals(p.getId()))) {
            throw new ValidacionException("El producto ya está en el detalle. Quítelo para cambiar la cantidad.");
        }
        ConversionUnidad unidad = usaUnidades() ? (ConversionUnidad) cbUnidad.getSelectedItem() : null;
        if (usaUnidades() && unidad == null) throw new ValidacionException("Seleccione la unidad de venta.");
        BigDecimal precio = tipo == Tipo.TRANSFERENCIA ? null : Ui.decimal(txtPrecio, tipo == Tipo.COMPRA ? "el costo" : "el precio");
        if (precio != null && precio.signum() < 0) throw new ValidacionException("El precio no puede ser negativo.");
        Linea l = new Linea(p, unidad, cant, precio);
        lineas.add(l);
        modelo.addRow(new Object[]{p.getCodigo(), p.getNombre(), unidad != null ? unidad.getUnidad() : p.getUnidad(),
                Ui.cantidad(cant), precio == null ? "" : Ui.moneda(precio), precio == null ? "" : Ui.moneda(l.subtotal())});
        txtCantidad.setText("");
        actualizarTotal();
        txtCantidad.requestFocus();
    }

    private void quitarLinea() {
        int i = tabla.getSelectedRow();
        if (i < 0) return;
        i = tabla.convertRowIndexToModel(i);
        lineas.remove(i);
        modelo.removeRow(i);
        actualizarTotal();
    }

    private void actualizarTotal() {
        lblTotal.setText("Total: " + Ui.moneda(lineas.stream().map(Linea::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    private void guardar() {
        switch (tipo) {
            case COMPRA -> {
                Compra c = new Compra();
                c.setIdProveedor(Ui.idDe(cbProveedor));
                c.setIdBodega(Ui.idDe(cbBodega));
                for (Linea l : lineas) {
                    DetalleCompra d = new DetalleCompra();
                    d.setIdProducto(l.producto().getId());
                    d.setProducto(l.producto().getNombre());
                    d.setCantidad(l.cantidad());
                    d.setCostoUnitario(l.precio());
                    c.agregarDetalle(d);
                }
                resultado = app.compras().registrar(c, usuario);
                Ui.info(this, "Compra #" + resultado.getId() + " registrada como PENDIENTE.\nEl stock ingresa al confirmar la recepción.");
            }
            case VENTA, COTIZACION -> {
                Venta v = new Venta();
                v.setIdCliente(Ui.idDe(cbCliente));
                v.setIdBodega(Ui.idDe(cbBodega));
                for (Linea l : lineas) {
                    DetalleVenta d = new DetalleVenta();
                    d.setIdProducto(l.producto().getId());
                    d.setProducto(l.producto().getNombre());
                    d.setIdUnidad(l.unidad().getIdUnidad());
                    d.setCantidad(l.cantidad());
                    d.setPrecioUnitario(l.precio());
                    v.agregarDetalle(d);
                }
                Venta guardada = tipo == Tipo.VENTA ? app.ventas().vender(v, usuario) : app.ventas().cotizar(v, usuario);
                resultado = guardada;
                Ui.info(this, (tipo == Tipo.VENTA ? "Venta" : "Cotización") + " #" + guardada.getId()
                        + " guardada. Total: " + Ui.moneda(guardada.getTotal()));
            }
            case TRANSFERENCIA -> {
                Transferencia t = new Transferencia();
                t.setIdBodegaOrigen(Ui.idDe(cbBodega));
                t.setIdBodegaDestino(Ui.idDe(cbDestino));
                t.setObservacion(txtObservacion.getText().trim());
                for (Linea l : lineas) {
                    DetalleTransferencia d = new DetalleTransferencia();
                    d.setIdProducto(l.producto().getId());
                    d.setProducto(l.producto().getNombre());
                    d.setCantidad(l.cantidad());
                    t.agregarDetalle(d);
                }
                resultado = app.inventario().enviarTransferencia(t, usuario);
                Ui.info(this, "Transferencia #" + resultado.getId() + " enviada (EN TRÁNSITO).\n"
                        + "El stock ya salió del origen; ingresará al destino cuando el encargado confirme la recepción.");
            }
        }
        dispose();
    }
}
