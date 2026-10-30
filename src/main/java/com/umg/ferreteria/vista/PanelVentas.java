package com.umg.ferreteria.vista;

import com.umg.ferreteria.modelo.*;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.List;

public class PanelVentas extends PanelDocumentos<Venta> {

    public PanelVentas(AppEscritorio app, Usuario u) {
        super(app, u, "Ventas, cotizaciones y devoluciones",
                "Una cotización no descuenta stock; al convertirla en venta se valida la existencia.",
                new String[]{"No.", "Fecha", "Tipo", "Cliente", "Bodega", "Vendedor", "Estado", "Total"},
                new String[]{"Código", "Producto", "Unidad", "Cantidad", "Cant. base", "Precio", "Subtotal"},
                Venta.COTIZACION, Venta.CONFIRMADA, Venta.ANULADA);
    }

    @Override
    protected List<Venta> cargar() { return app.ventas().listar(); }

    @Override
    protected Object[] fila(Venta v) {
        return new Object[]{v.getId(), Ui.fecha(v.getFecha()), v.getTipoDocumento(), v.getCliente(), v.getBodega(),
                v.getUsuario(), v.getEstado(), Ui.moneda(v.getTotal())};
    }

    @Override
    protected Venta completo(Venta v) { return app.ventas().buscar(v.getId()); }

    @Override
    protected String resumen(Venta v) {
        List<Devolucion> devs = app.ventas().devolucionesDe(v.getId());
        String extra = devs.isEmpty() ? "" : " · Devoluciones: " + devs.stream()
                .map(d -> Ui.cantidad(d.getCantidadBase()) + " " + d.getProducto() + " (" + d.getMotivo() + ")").toList();
        return super.resumen(v) + " · Total " + Ui.moneda(v.getTotal()) + extra;
    }

    @Override
    protected Object[] filaDetalle(DetalleDocumento d) {
        DetalleVenta dv = (DetalleVenta) d;
        return new Object[]{dv.getCodigo(), dv.getProducto(), dv.getUnidad(), Ui.cantidad(dv.getCantidad()),
                Ui.cantidad(dv.getCantidadBase()), Ui.moneda(dv.getPrecioUnitario()), Ui.moneda(dv.getSubtotal())};
    }

    @Override
    protected void botones(JPanel barra) {
        JButton venta = Ui.boton("Nueva venta", Ui.PRIMARIO);
        venta.addActionListener(e -> {
            if (new DialogoDocumento(this, app, usuario, DialogoDocumento.Tipo.VENTA).mostrar() != null) refrescar();
        });
        JButton cotizacion = Ui.boton("Nueva cotización", Ui.INFO);
        cotizacion.addActionListener(e -> {
            if (new DialogoDocumento(this, app, usuario, DialogoDocumento.Tipo.COTIZACION).mostrar() != null) refrescar();
        });
        JButton convertir = Ui.boton("Convertir en venta", Ui.EXITO);
        convertir.addActionListener(e -> Ui.accion(this, () -> {
            Venta v = requerirSeleccion();
            if (v == null || !Ui.confirmar(this, "¿Convertir la cotización #" + v.getId() + " en venta?\nSe descontará el inventario.")) return;
            app.ventas().convertirCotizacion(v.getId(), usuario);
            Ui.info(this, "Cotización #" + v.getId() + " convertida en venta.");
            refrescar();
        }));
        JButton devolucion = Ui.boton("Devolución");
        devolucion.addActionListener(e -> Ui.accion(this, this::registrarDevolucion));
        JButton anular = Ui.boton("Anular", Ui.PELIGRO);
        anular.addActionListener(e -> Ui.accion(this, () -> {
            Venta v = requerirSeleccion();
            if (v == null || !Ui.confirmar(this, "¿Anular " + v.getTipoDocumento().toLowerCase() + " #" + v.getId() + "?")) return;
            app.ventas().anular(v.getId(), usuario);
            refrescar();
        }));
        barra.add(venta);
        barra.add(cotizacion);
        barra.add(convertir);
        barra.add(devolucion);
        barra.add(anular);
    }

    /** Regla: la devolución referencia la venta y no puede exceder lo vendido. */
    private void registrarDevolucion() {
        Venta sel = requerirSeleccion();
        if (sel == null) return;
        Venta v = app.ventas().buscar(sel.getId());
        JComboBox<DetalleVenta> cbDet = new JComboBox<>();
        v.getDetalles().forEach(cbDet::addItem);
        cbDet.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> l, Object val, int i, boolean s, boolean f) {
                String txt = val instanceof DetalleVenta d ? d.getProducto() + " — vendido: " + Ui.cantidad(d.getCantidadBase()) + " (unidad base)" : "";
                return super.getListCellRendererComponent(l, txt, i, s, f);
            }
        });
        JTextField txtCant = new JTextField();
        JTextField txtMotivo = new JTextField();
        Ui.Formulario f = new Ui.Formulario().campo("Venta", new JLabel("#" + v.getId() + " — " + v.getCliente() + " (" + v.getEstado() + ")"))
                .campo("Producto", cbDet).campo("Cantidad (unidad base)", txtCant).campo("Motivo", txtMotivo);
        if (JOptionPane.showConfirmDialog(this, f, "Registrar devolución", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                != JOptionPane.OK_OPTION) return;
        DetalleVenta d = (DetalleVenta) cbDet.getSelectedItem();
        Devolucion dev = new Devolucion();
        dev.setIdVenta(v.getId());
        dev.setIdProducto(d == null ? null : d.getIdProducto());
        BigDecimal cant = Ui.decimal(txtCant, "la cantidad");
        dev.setCantidadBase(cant);
        dev.setMotivo(txtMotivo.getText().trim());
        app.ventas().devolver(dev, usuario);
        Ui.info(this, "Devolución registrada; el producto regresó a " + v.getBodega() + ".");
        refrescar();
    }
}
