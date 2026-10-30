package com.umg.ferreteria.servicio;

import com.umg.ferreteria.dao.AuditoriaDao;
import com.umg.ferreteria.dao.CompraDao;
import com.umg.ferreteria.dao.ConexionBD;
import com.umg.ferreteria.dao.ProductoDao;
import com.umg.ferreteria.excepcion.AccesoDenegadoException;
import com.umg.ferreteria.excepcion.ReglaNegocioException;
import com.umg.ferreteria.excepcion.ValidacionException;
import com.umg.ferreteria.modelo.Compra;
import com.umg.ferreteria.modelo.DetalleCompra;
import com.umg.ferreteria.modelo.Rol;
import com.umg.ferreteria.modelo.Usuario;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** Compras a proveedores: se registran PENDIENTES y la RECEPCIÓN ingresa el stock. */
@Service
public class CompraService {

    private final ConexionBD db;
    private final CompraDao compras;
    private final ProductoDao productos;
    private final InventarioService inventario;
    private final AuditoriaDao auditoria;

    public CompraService(ConexionBD db, CompraDao compras, ProductoDao productos, InventarioService inventario, AuditoriaDao auditoria) {
        this.db = db;
        this.compras = compras;
        this.productos = productos;
        this.inventario = inventario;
        this.auditoria = auditoria;
    }

    public List<Compra> listar() {
        return db.consultar(compras::listar);
    }

    public Compra buscar(int id) {
        return db.consultar(cn -> compras.buscarPorId(cn, id)).orElseThrow(() -> new ValidacionException("No existe la compra #" + id));
    }

    public Compra registrar(Compra c, Usuario u) {
        Permisos.requiere(u, "registrar compras", Rol.ADMIN, Rol.SUPERVISOR, Rol.ENCARGADO);
        c.validar();
        c.setIdUsuario(u.getId());
        c.setEstado(Compra.PENDIENTE);
        return db.transaccion(cn -> {
            c.setId(compras.insertar(cn, c));
            auditoria.registrar(cn, u.getId(), "CREAR", "Compra", "#" + c.getId() + " total=Q" + c.calcularTotal());
            return c;
        });
    }

    /** Recepción: suma existencias en la bodega y actualiza el costo del producto (una sola transacción). */
    public Compra recibir(int id, Usuario u) {
        Permisos.requiere(u, "recibir compras", Rol.ADMIN, Rol.SUPERVISOR, Rol.ENCARGADO);
        return db.transaccion(cn -> {
            Compra c = compras.buscarPorId(cn, id).orElseThrow();
            if (!Compra.PENDIENTE.equals(c.getEstado())) {
                throw new ReglaNegocioException("La compra #" + id + " ya fue " + c.getEstado().toLowerCase() + ".");
            }
            if (u.tieneRol(Rol.ENCARGADO) && !c.getIdBodega().equals(u.getIdBodega())) {
                throw new AccesoDenegadoException("Solo el encargado de " + c.getBodega() + " puede recibir esta compra.");
            }
            for (DetalleCompra d : c.getDetalles()) {
                inventario.moverStock(cn, d.getIdProducto(), c.getIdBodega(), d.getCantidad(), "COMPRA", "Compra #" + id);
                productos.actualizarCosto(cn, d.getIdProducto(), d.getCostoUnitario());
            }
            c.setEstado(Compra.RECIBIDA);
            c.setFechaRecepcion(LocalDateTime.now());
            compras.actualizar(cn, c);
            auditoria.registrar(cn, u.getId(), "RECIBIR", "Compra", "#" + id + " en " + c.getBodega());
            return c;
        });
    }

    public void anular(int id, Usuario u) {
        Permisos.requiere(u, "anular compras", Rol.ADMIN, Rol.SUPERVISOR);
        db.transaccion(cn -> {
            Compra c = compras.buscarPorId(cn, id).orElseThrow();
            if (!Compra.PENDIENTE.equals(c.getEstado())) {
                throw new ReglaNegocioException("Solo se puede anular una compra pendiente de recepción.");
            }
            c.setEstado(Compra.ANULADA);
            compras.actualizar(cn, c);
            auditoria.registrar(cn, u.getId(), "ANULAR", "Compra", "#" + id);
            return null;
        });
    }
}
