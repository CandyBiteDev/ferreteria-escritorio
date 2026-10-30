package com.umg.ferreteria.vista;

import com.umg.ferreteria.modelo.DetalleDocumento;
import com.umg.ferreteria.modelo.DetalleTransferencia;
import com.umg.ferreteria.modelo.Transferencia;
import com.umg.ferreteria.modelo.Usuario;

import javax.swing.*;
import java.util.List;

public class PanelTransferencias extends PanelDocumentos<Transferencia> {

    public PanelTransferencias(AppEscritorio app, Usuario u) {
        super(app, u, "Transferencias entre bodegas",
                "Salida en origen (EN_TRANSITO) y recepción en destino (RECIBIDA) son pasos separados.",
                new String[]{"No.", "Enviada", "Origen", "Destino", "Envió", "Estado", "Recibida", "Recibió", "Observación"},
                new String[]{"Código", "Producto", "Cantidad", "Unidad"},
                Transferencia.EN_TRANSITO, Transferencia.RECIBIDA, Transferencia.ANULADA);
    }

    @Override
    protected List<Transferencia> cargar() { return app.inventario().listarTransferencias(null, null); }

    @Override
    protected Object[] fila(Transferencia t) {
        return new Object[]{t.getId(), Ui.fecha(t.getFecha()), t.getBodegaOrigen(), t.getBodegaDestino(), t.getUsuario(),
                t.getEstado(), Ui.fecha(t.getFechaRecepcion()), t.getUsuarioRecibe(), t.getObservacion()};
    }

    @Override
    protected Transferencia completo(Transferencia t) { return t; }

    @Override
    protected Object[] filaDetalle(DetalleDocumento d) {
        DetalleTransferencia dt = (DetalleTransferencia) d;
        return new Object[]{dt.getCodigo(), dt.getProducto(), Ui.cantidad(dt.getCantidad()), dt.getUnidad()};
    }

    @Override
    protected void botones(JPanel barra) {
        JButton nueva = Ui.boton("Nueva transferencia", Ui.PRIMARIO);
        nueva.addActionListener(e -> {
            if (new DialogoDocumento(this, app, usuario, DialogoDocumento.Tipo.TRANSFERENCIA).mostrar() != null) refrescar();
        });
        JButton recibir = Ui.boton("Confirmar recepción", Ui.EXITO);
        recibir.addActionListener(e -> Ui.accion(this, () -> {
            Transferencia t = requerirSeleccion();
            if (t == null) return;
            String obs = JOptionPane.showInputDialog(this, "Confirmar recepción de la transferencia #" + t.getId()
                    + " en " + t.getBodegaDestino() + ".\nObservación (opcional):", "Recepción", JOptionPane.QUESTION_MESSAGE);
            if (obs == null) return;
            app.inventario().recibirTransferencia(t.getId(), usuario, obs);
            Ui.info(this, "Transferencia #" + t.getId() + " recibida. Stock ingresado en " + t.getBodegaDestino() + ".");
            refrescar();
        }));
        JButton anular = Ui.boton("Anular", Ui.PELIGRO);
        anular.addActionListener(e -> Ui.accion(this, () -> {
            Transferencia t = requerirSeleccion();
            if (t == null) return;
            String motivo = JOptionPane.showInputDialog(this, "Motivo de la anulación (el stock regresa al origen):");
            if (motivo == null) return;
            app.inventario().anularTransferencia(t.getId(), usuario, motivo);
            refrescar();
        }));
        barra.add(nueva);
        barra.add(recibir);
        barra.add(anular);
    }
}
