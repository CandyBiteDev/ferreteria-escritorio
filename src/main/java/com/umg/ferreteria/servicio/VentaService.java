package com.umg.ferreteria.servicio;

import com.umg.ferreteria.dao.*;
import com.umg.ferreteria.excepcion.ReglaNegocioException;
import com.umg.ferreteria.excepcion.StockInsuficienteException;
import com.umg.ferreteria.excepcion.ValidacionException;
import com.umg.ferreteria.modelo.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** Cotizaciones, ventas y devoluciones. */
@Service
public class VentaService {

    private final ConexionBD db;
    private final VentaDao ventas;
    private final DevolucionDao devoluciones;
    private final ProductoDao productos;
    private final ConversionUnidadDao conversiones;
    private final UnidadMedidaDao unidades;
    private final InventarioService inventario;
    private final AuditoriaDao auditoria;

    public VentaService(ConexionBD db, VentaDao ventas, DevolucionDao devoluciones, ProductoDao productos,
                        ConversionUnidadDao conversiones, UnidadMedidaDao unidades, InventarioService inventario, AuditoriaDao auditoria) {
        this.db = db;
        this.ventas = ventas;
        this.devoluciones = devoluciones;
        this.productos = productos;
        this.conversiones = conversiones;
        this.unidades = unidades;
        this.inventario = inventario;
        this.auditoria = auditoria;
    }

    public List<Venta> listar() {
        return db.consultar(ventas::listar);
    }

    public Venta buscar(int id) {
        return db.consultar(cn -> ventas.buscarPorId(cn, id)).orElseThrow(() -> new ValidacionException("No existe la venta #" + id));
    }

    public List<Devolucion> devolucionesDe(int idVenta) {
        return db.consultar(cn -> devoluciones.listarPorVenta(cn, idVenta));
    }

    /** Unidades en las que se puede vender un producto: su unidad base + las que tienen conversión. */
    public List<ConversionUnidad> unidadesDeVenta(int idProducto) {
        return db.consultar(cn -> {
            Producto p = productos.buscarPorId(cn, idProducto).orElseThrow();
            List<ConversionUnidad> lista = new java.util.ArrayList<>();
            ConversionUnidad base = new ConversionUnidad();
            base.setIdProducto(idProducto);
            base.setIdUnidad(p.getIdUnidad());
            base.setUnidad(p.getUnidad());
            base.setFactor(BigDecimal.ONE);
            lista.add(base);
            lista.addAll(conversiones.listarPorProducto(cn, idProducto));
            return lista;
        });
    }

    /**
     * Regla: la cantidad vendida puede estar en unidades equivalentes SOLO si hay conversión definida.
     * Calcula cantidad en unidad base, precio y costo del momento.
     */
    private void prepararDetalle(Connection cn, DetalleVenta d) throws SQLException {
        Producto p = productos.buscarPorId(cn, d.getIdProducto())
                .orElseThrow(() -> new ValidacionException("Producto inexistente #" + d.getIdProducto()));
        if (!p.isActivo()) throw new ReglaNegocioException("El producto " + p.getNombre() + " está inactivo.");
        BigDecimal factor;
        if (p.getIdUnidad().equals(d.getIdUnidad())) {
            factor = BigDecimal.ONE;
        } else {
            factor = conversiones.factor(cn, p.getId(), d.getIdUnidad());
            if (factor == null) {
                String unidad = unidades.buscarPorId(cn, d.getIdUnidad()).map(UnidadMedida::getNombre).orElse("?");
                throw new ReglaNegocioException("No hay conversión definida de '" + unidad + "' a '" + p.getUnidad()
                        + "' para " + p.getNombre() + ". Defínala en Productos > Conversiones.");
            }
        }
        d.setProducto(p.getNombre());
        d.setCantidadBase(d.getCantidad().multiply(factor));
        if (d.getPrecioUnitario() == null || d.getPrecioUnitario().signum() <= 0) {
            d.setPrecioUnitario(p.getPrecioVenta().multiply(factor).setScale(2, RoundingMode.HALF_UP));
        }
        d.setCostoUnitario(p.getPrecioCosto());
    }

    public Venta cotizar(Venta v, Usuario u) {
        Permisos.requiere(u, "cotizar", Rol.ADMIN, Rol.SUPERVISOR, Rol.VENDEDOR);
        v.validar();
        v.setIdUsuario(u.getId());
        v.setEstado(Venta.COTIZACION);
        return db.transaccion(cn -> {
            for (DetalleVenta d : v.getDetalles()) prepararDetalle(cn, d);
            v.setId(ventas.insertar(cn, v));
            v.setTotal(v.calcularTotal());
            auditoria.registrar(cn, u.getId(), "CREAR", "Cotizacion", "#" + v.getId() + " total=Q" + v.getTotal());
            return v;
        });
    }

    /** Venta directa: encabezado + detalle + rebaja de inventario en una única transacción. */
    public Venta vender(Venta v, Usuario u) {
        Permisos.requiere(u, "vender", Rol.ADMIN, Rol.SUPERVISOR, Rol.VENDEDOR);
        v.validar();
        v.setIdUsuario(u.getId());
        v.setEstado(Venta.CONFIRMADA);
        return db.transaccion(cn -> {
            for (DetalleVenta d : v.getDetalles()) prepararDetalle(cn, d);
            int id = ventas.insertar(cn, v);
            v.setId(id);
            v.setTotal(v.calcularTotal());
            for (DetalleVenta d : v.getDetalles()) {
                inventario.moverStock(cn, d.getIdProducto(), v.getIdBodega(), d.getCantidadBase().negate(), "VENTA", "Venta #" + id);
            }
            auditoria.registrar(cn, u.getId(), "CREAR", "Venta", "#" + id + " total=Q" + v.getTotal());
            return v;
        });
    }

    /** Regla: no convertir una cotización en venta si faltan existencias (rollback completo). */
    public Venta convertirCotizacion(int id, Usuario u) {
        Permisos.requiere(u, "convertir cotizaciones", Rol.ADMIN, Rol.SUPERVISOR, Rol.VENDEDOR);
        try {
            return db.transaccion(cn -> {
                Venta v = ventas.buscarPorId(cn, id).orElseThrow();
                if (!Venta.COTIZACION.equals(v.getEstado())) {
                    throw new ReglaNegocioException("El documento #" + id + " no es una cotización vigente (estado: " + v.getEstado() + ").");
                }
                for (DetalleVenta d : v.getDetalles()) {
                    inventario.moverStock(cn, d.getIdProducto(), v.getIdBodega(), d.getCantidadBase().negate(), "VENTA", "Venta #" + id);
                }
                v.setEstado(Venta.CONFIRMADA);
                v.setFecha(LocalDateTime.now());
                ventas.actualizar(cn, v);
                auditoria.registrar(cn, u.getId(), "CONVERTIR", "Cotizacion", "#" + id + " convertida en venta");
                return v;
            });
        } catch (StockInsuficienteException e) {
            throw new ReglaNegocioException("No se puede convertir la cotización #" + id + " en venta. " + e.getMessage());
        }
    }

    /** Anula una cotización o una venta sin devoluciones (la venta regresa el stock). */
    public void anular(int id, Usuario u) {
        Permisos.requiere(u, "anular ventas", Rol.ADMIN, Rol.SUPERVISOR);
        db.transaccion(cn -> {
            Venta v = ventas.buscarPorId(cn, id).orElseThrow();
            if (Venta.ANULADA.equals(v.getEstado())) throw new ReglaNegocioException("El documento ya está anulado.");
            if (Venta.CONFIRMADA.equals(v.getEstado())) {
                if (!devoluciones.listarPorVenta(cn, id).isEmpty()) {
                    throw new ReglaNegocioException("La venta tiene devoluciones registradas; no puede anularse.");
                }
                for (DetalleVenta d : v.getDetalles()) {
                    inventario.moverStock(cn, d.getIdProducto(), v.getIdBodega(), d.getCantidadBase(), "VENTA_ANULADA", "Venta #" + id);
                }
            }
            v.setEstado(Venta.ANULADA);
            ventas.actualizar(cn, v);
            auditoria.registrar(cn, u.getId(), "ANULAR", v.getTipoDocumento(), "#" + id);
            return null;
        });
    }

    /** Regla: la devolución referencia una venta confirmada y no excede lo vendido menos lo ya devuelto. */
    public Devolucion devolver(Devolucion dev, Usuario u) {
        Permisos.requiere(u, "registrar devoluciones", Rol.ADMIN, Rol.SUPERVISOR, Rol.VENDEDOR);
        dev.validar();
        dev.setIdUsuario(u.getId());
        return db.transaccion(cn -> {
            Venta v = ventas.buscarPorId(cn, dev.getIdVenta())
                    .orElseThrow(() -> new ValidacionException("No existe la venta #" + dev.getIdVenta()));
            if (!Venta.CONFIRMADA.equals(v.getEstado())) {
                throw new ReglaNegocioException("Solo se aceptan devoluciones de ventas confirmadas.");
            }
            BigDecimal vendido = v.getDetalles().stream().filter(d -> d.getIdProducto().equals(dev.getIdProducto()))
                    .map(DetalleVenta::getCantidadBase).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (vendido.signum() == 0) throw new ReglaNegocioException("Ese producto no pertenece a la venta #" + v.getId() + ".");
            BigDecimal devuelto = ventas.cantidadDevuelta(cn, v.getId(), dev.getIdProducto());
            BigDecimal maximo = vendido.subtract(devuelto);
            if (dev.getCantidadBase().compareTo(maximo) > 0) {
                throw new ReglaNegocioException("La devolución excede lo vendido. Vendido: " + vendido.stripTrailingZeros().toPlainString()
                        + ", ya devuelto: " + devuelto.stripTrailingZeros().toPlainString()
                        + ", máximo permitido: " + maximo.stripTrailingZeros().toPlainString());
            }
            int id = devoluciones.insertar(cn, dev);
            dev.setId(id);
            inventario.moverStock(cn, dev.getIdProducto(), v.getIdBodega(), dev.getCantidadBase(), "DEVOLUCION", "Devolución #" + id);
            auditoria.registrar(cn, u.getId(), "CREAR", "Devolucion", "#" + id + " venta=" + v.getId() + " motivo=" + dev.getMotivo());
            return dev;
        });
    }
}
