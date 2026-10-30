package com.umg.ferreteria.vista;

import com.umg.ferreteria.modelo.Compra;
import com.umg.ferreteria.modelo.DetalleCompra;
import com.umg.ferreteria.modelo.DetalleDocumento;
import com.umg.ferreteria.modelo.Usuario;

import javax.swing.*;
import java.util.List;

public class PanelCompras extends PanelDocumentos<Compra> {

    public PanelCompras(AppEscritorio app, Usuario u) {
        super(app, u, "Compras y recepciones", "La compra se registra PENDIENTE; al recibirla ingresa el stock y se actualiza el costo.",
                new String[]{"No.", "Fecha", "Proveedor", "Bodega", "Registró", "Estado", "Recibida", "Total"},
                new String[]{"Código", "Producto", "Cantidad", "Costo unitario", "Subtotal"},
                Compra.PENDIENTE, Compra.RECIBIDA, Compra.ANULADA);
    }

    @Override
    protected List<Compra> cargar() { return app.compras().listar(); }

    @Override
    protected Object[] fila(Compra c) {
        return new Object[]{c.getId(), Ui.fecha(c.getFecha()), c.getProveedor(), c.getBodega(), c.getUsuario(),
                c.getEstado(), Ui.fecha(c.getFechaRecepcion()), Ui.moneda(c.getTotal())};
    }

    @Override
    protected Compra completo(Compra c) { return app.compras().buscar(c.getId()); }

    @Override
    protected Object[] filaDetalle(DetalleDocumento d) {
        DetalleCompra dc = (DetalleCompra) d;
        return new Object[]{dc.getCodigo(), dc.getProducto(), Ui.cantidad(dc.getCantidad()), Ui.moneda(dc.getCostoUnitario()), Ui.moneda(dc.getSubtotal())};
    }

    @Override
    protected void botones(JPanel barra) {
        JButton nueva = Ui.boton("Nueva compra", Ui.PRIMARIO);
        nueva.addActionListener(e -> {
            if (new DialogoDocumento(this, app, usuario, DialogoDocumento.Tipo.COMPRA).mostrar() != null) refrescar();
        });
        JButton recibir = Ui.boton("Confirmar recepción", Ui.EXITO);
        recibir.addActionListener(e -> Ui.accion(this, () -> {
            Compra c = requerirSeleccion();
            if (c == null || !Ui.confirmar(this, "¿Confirmar que se recibió la compra #" + c.getId() + " en " + c.getBodega() + "?")) return;
            app.compras().recibir(c.getId(), usuario);
            Ui.info(this, "Compra #" + c.getId() + " recibida. Existencias actualizadas.");
            refrescar();
        }));
        JButton anular = Ui.boton("Anular", Ui.PELIGRO);
        anular.addActionListener(e -> Ui.accion(this, () -> {
            Compra c = requerirSeleccion();
            if (c == null || !Ui.confirmar(this, "¿Anular la compra #" + c.getId() + "?")) return;
            app.compras().anular(c.getId(), usuario);
            refrescar();
        }));
        barra.add(nueva);
        barra.add(recibir);
        barra.add(anular);
    }
}
