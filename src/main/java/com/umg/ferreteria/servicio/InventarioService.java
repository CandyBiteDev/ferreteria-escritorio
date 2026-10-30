package com.umg.ferreteria.servicio;

import com.umg.ferreteria.dao.*;
import com.umg.ferreteria.excepcion.AccesoDenegadoException;
import com.umg.ferreteria.excepcion.ReglaNegocioException;
import com.umg.ferreteria.excepcion.StockInsuficienteException;
import com.umg.ferreteria.excepcion.ValidacionException;
import com.umg.ferreteria.modelo.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** Existencias, ajustes y transferencias. Todo cambio de stock pasa por moverStock(). */
@Service
public class InventarioService {

    private final ConexionBD db;
    private final ExistenciaDao existencias;
    private final ProductoDao productos;
    private final BodegaDao bodegas;
    private final TransferenciaDao transferencias;
    private final AjusteInventarioDao ajustes;
    private final AuditoriaDao auditoria;

    public InventarioService(ConexionBD db, ExistenciaDao existencias, ProductoDao productos, BodegaDao bodegas,
                             TransferenciaDao transferencias, AjusteInventarioDao ajustes, AuditoriaDao auditoria) {
        this.db = db;
        this.existencias = existencias;
        this.productos = productos;
        this.bodegas = bodegas;
        this.transferencias = transferencias;
        this.ajustes = ajustes;
        this.auditoria = auditoria;
    }

    public List<Existencia> existencias(Integer idBodega, String texto) {
        return db.consultar(cn -> existencias.buscar(cn, idBodega, texto));
    }

    public BigDecimal disponible(int idProducto, int idBodega) {
        return db.consultar(cn -> existencias.disponible(cn, idProducto, idBodega));
    }

    /**
     * Suma (delta > 0) o resta (delta < 0) stock dentro de la transacción recibida.
     * Regla: NUNCA deja stock negativo. Bloquea la fila para evitar ventas simultáneas.
     */
    void moverStock(Connection cn, int idProducto, int idBodega, BigDecimal delta, String tipo, String referencia) throws SQLException {
        BigDecimal actual = existencias.cantidadParaActualizar(cn, idProducto, idBodega);
        BigDecimal base = actual == null ? BigDecimal.ZERO : actual;
        BigDecimal nuevo = base.add(delta);
        if (nuevo.signum() < 0) {
            String prod = productos.buscarPorId(cn, idProducto).map(Producto::getNombre).orElse("#" + idProducto);
            String bod = bodegas.buscarPorId(cn, idBodega).map(Bodega::getNombre).orElse("#" + idBodega);
            throw new StockInsuficienteException(prod, bod, base, delta.negate());
        }
        if (actual == null) existencias.insertar(cn, idProducto, idBodega, nuevo);
        else existencias.actualizar(cn, idProducto, idBodega, nuevo);
        existencias.registrarMovimiento(cn, idProducto, idBodega, tipo, delta, nuevo, referencia);
    }

    // ---------- Ajustes ----------
    /** Regla: requiere motivo y autorización de un usuario con permisos (SUPERVISOR/ADMIN). */
    public AjusteInventario registrarAjuste(AjusteInventario a, Usuario solicita, Usuario autoriza) {
        a.validar();
        if (autoriza == null || !autoriza.puedeAutorizar()) {
            throw new AccesoDenegadoException("El ajuste debe ser autorizado por un supervisor o administrador.");
        }
        a.setIdUsuarioSolicita(solicita.getId());
        a.setIdUsuarioAutoriza(autoriza.getId());
        return db.transaccion(cn -> {
            int id = ajustes.insertar(cn, a);
            a.setId(id);
            moverStock(cn, a.getIdProducto(), a.getIdBodega(), a.getCantidad(), "AJUSTE", "Ajuste #" + id);
            auditoria.registrar(cn, solicita.getId(), "AJUSTE", "AjusteInventario",
                    "#" + id + " prod=" + a.getIdProducto() + " bodega=" + a.getIdBodega() + " cant=" + a.getCantidad()
                            + " motivo=" + a.getMotivo() + " autorizó=" + autoriza.getUsername());
            return a;
        });
    }

    public List<AjusteInventario> listarAjustes() {
        return db.consultar(ajustes::listar);
    }

    // ---------- Transferencias ----------
    public List<Transferencia> listarTransferencias(Integer idBodegaDestino, String estado) {
        return db.consultar(cn -> transferencias.buscar(cn, idBodegaDestino, estado));
    }

    public Transferencia buscarTransferencia(int id) {
        return db.consultar(cn -> transferencias.buscarPorId(cn, id))
                .orElseThrow(() -> new ValidacionException("No existe la transferencia #" + id));
    }

    /** Registra la SALIDA de origen: descuenta stock y queda EN_TRANSITO, todo en una transacción. */
    public Transferencia enviarTransferencia(Transferencia t, Usuario u) {
        Permisos.requiere(u, "enviar transferencias", Rol.ADMIN, Rol.SUPERVISOR, Rol.ENCARGADO);
        if (u.tieneRol(Rol.ENCARGADO) && !t.getIdBodegaOrigen().equals(u.getIdBodega())) {
            throw new AccesoDenegadoException("Un encargado solo puede enviar desde su propia bodega.");
        }
        t.validar();
        t.setIdUsuario(u.getId());
        t.setEstado(Transferencia.EN_TRANSITO);
        return db.transaccion(cn -> {
            int id = transferencias.insertar(cn, t);
            t.setId(id);
            for (DetalleTransferencia d : t.getDetalles()) {
                moverStock(cn, d.getIdProducto(), t.getIdBodegaOrigen(), d.getCantidad().negate(), "TRF_SALIDA", "Transferencia #" + id);
            }
            auditoria.registrar(cn, u.getId(), "ENVIAR", "Transferencia",
                    "#" + id + " origen=" + t.getIdBodegaOrigen() + " destino=" + t.getIdBodegaDestino() + " lineas=" + t.getDetalles().size());
            return t;
        });
    }

    /** Registra la RECEPCIÓN en destino (lo usa la app móvil del encargado). */
    public Transferencia recibirTransferencia(int id, Usuario u, String observacion) {
        Permisos.requiere(u, "recibir transferencias", Rol.ADMIN, Rol.SUPERVISOR, Rol.ENCARGADO);
        return db.transaccion(cn -> {
            Transferencia t = transferencias.buscarPorId(cn, id)
                    .orElseThrow(() -> new ValidacionException("No existe la transferencia #" + id));
            if (!Transferencia.EN_TRANSITO.equals(t.getEstado())) {
                throw new ReglaNegocioException("La transferencia #" + id + " no está en tránsito (estado: " + t.getEstado() + ").");
            }
            if (u.tieneRol(Rol.ENCARGADO) && !t.getIdBodegaDestino().equals(u.getIdBodega())) {
                throw new AccesoDenegadoException("Solo el encargado de " + t.getBodegaDestino() + " puede confirmar esta recepción.");
            }
            for (DetalleTransferencia d : t.getDetalles()) {
                moverStock(cn, d.getIdProducto(), t.getIdBodegaDestino(), d.getCantidad(), "TRF_ENTRADA", "Transferencia #" + id);
            }
            t.setEstado(Transferencia.RECIBIDA);
            t.setIdUsuarioRecibe(u.getId());
            t.setUsuarioRecibe(u.getNombre());
            t.setFechaRecepcion(LocalDateTime.now());
            if (observacion != null && !observacion.isBlank()) {
                t.setObservacion((t.getObservacion() == null ? "" : t.getObservacion() + " | ") + "Recepción: " + observacion.trim());
            }
            transferencias.actualizar(cn, t);
            auditoria.registrar(cn, u.getId(), "RECIBIR", "Transferencia", "#" + id + " en " + t.getBodegaDestino());
            return t;
        });
    }

    /** Anula una transferencia en tránsito: el stock regresa a la bodega de origen. */
    public void anularTransferencia(int id, Usuario u, String motivo) {
        Permisos.requiere(u, "anular transferencias", Rol.ADMIN, Rol.SUPERVISOR);
        if (motivo == null || motivo.isBlank()) throw new ValidacionException("Indique el motivo de la anulación.");
        db.transaccion(cn -> {
            Transferencia t = transferencias.buscarPorId(cn, id).orElseThrow();
            if (!Transferencia.EN_TRANSITO.equals(t.getEstado())) {
                throw new ReglaNegocioException("Solo se puede anular una transferencia en tránsito.");
            }
            for (DetalleTransferencia d : t.getDetalles()) {
                moverStock(cn, d.getIdProducto(), t.getIdBodegaOrigen(), d.getCantidad(), "TRF_ANULADA", "Transferencia #" + id);
            }
            t.setEstado(Transferencia.ANULADA);
            t.setObservacion((t.getObservacion() == null ? "" : t.getObservacion() + " | ") + "Anulada: " + motivo);
            transferencias.actualizar(cn, t);
            auditoria.registrar(cn, u.getId(), "ANULAR", "Transferencia", "#" + id + " motivo=" + motivo);
            return null;
        });
    }
}
