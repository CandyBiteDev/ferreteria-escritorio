package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.DetalleVenta;
import com.umg.ferreteria.modelo.Venta;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class VentaDao extends DaoBase<Venta> {

    @Override
    protected String tabla() { return "venta"; }

    @Override
    protected String consultaBase() {
        return "SELECT t.*, c.nombre AS cliente, b.nombre AS bodega, u.nombre AS usuario FROM venta t "
                + "JOIN cliente c ON c.id = t.id_cliente "
                + "JOIN bodega b ON b.id = t.id_bodega "
                + "JOIN usuario u ON u.id = t.id_usuario";
    }

    @Override
    protected String orden() { return "t.id DESC"; }

    @Override
    public Optional<Venta> buscarPorId(Connection cn, int id) throws SQLException {
        Optional<Venta> v = super.buscarPorId(cn, id);
        if (v.isPresent()) {
            for (DetalleVenta d : detalles(cn, id)) v.get().agregarDetalle(d);
        }
        return v;
    }

    public List<DetalleVenta> detalles(Connection cn, int idVenta) throws SQLException {
        return Jdbc.lista(cn, "SELECT d.*, p.codigo, p.nombre AS producto, u.nombre AS unidad FROM detalle_venta d "
                + "JOIN producto p ON p.id = d.id_producto JOIN unidad_medida u ON u.id = d.id_unidad "
                + "WHERE d.id_venta = ? ORDER BY d.id", rs -> {
            DetalleVenta d = new DetalleVenta();
            d.setId(rs.getInt("id"));
            d.setIdProducto(rs.getInt("id_producto"));
            d.setCodigo(rs.getString("codigo"));
            d.setProducto(rs.getString("producto"));
            d.setIdUnidad(rs.getInt("id_unidad"));
            d.setUnidad(rs.getString("unidad"));
            d.setCantidad(Jdbc.decimal(rs, "cantidad"));
            d.setCantidadBase(Jdbc.decimal(rs, "cantidad_base"));
            d.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
            d.setCostoUnitario(rs.getBigDecimal("costo_unitario"));
            return d;
        }, idVenta);
    }

    public BigDecimal cantidadDevuelta(Connection cn, int idVenta, int idProducto) throws SQLException {
        BigDecimal v = Jdbc.escalar(cn, "SELECT COALESCE(SUM(cantidad_base), 0) FROM devolucion WHERE id_venta = ? AND id_producto = ?",
                idVenta, idProducto);
        return v == null ? BigDecimal.ZERO : v;
    }

    @Override
    protected Venta mapear(ResultSet rs) throws SQLException {
        Venta v = new Venta();
        v.setId(rs.getInt("id"));
        v.setFecha(Jdbc.fecha(rs, "fecha"));
        v.setIdCliente(rs.getInt("id_cliente"));
        v.setCliente(rs.getString("cliente"));
        v.setIdBodega(rs.getInt("id_bodega"));
        v.setBodega(rs.getString("bodega"));
        v.setIdUsuario(rs.getInt("id_usuario"));
        v.setUsuario(rs.getString("usuario"));
        v.setEstado(rs.getString("estado"));
        v.setTotal(rs.getBigDecimal("total"));
        return v;
    }

    @Override
    public int insertar(Connection cn, Venta v) throws SQLException {
        int id = Jdbc.insertar(cn, "INSERT INTO venta (id_cliente, id_bodega, id_usuario, estado, total) VALUES (?, ?, ?, ?, ?)",
                v.getIdCliente(), v.getIdBodega(), v.getIdUsuario(), v.getEstado(), v.calcularTotal());
        for (DetalleVenta d : v.getDetalles()) {
            Jdbc.ejecutar(cn, "INSERT INTO detalle_venta (id_venta, id_producto, id_unidad, cantidad, cantidad_base, precio_unitario, costo_unitario) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    id, d.getIdProducto(), d.getIdUnidad(), d.getCantidad(), d.getCantidadBase(), d.getPrecioUnitario(), d.getCostoUnitario());
        }
        return id;
    }

    /** Cambia estado; al confirmar se actualiza también la fecha (fecha real de la venta). */
    @Override
    public void actualizar(Connection cn, Venta v) throws SQLException {
        Jdbc.ejecutar(cn, "UPDATE venta SET estado = ?, fecha = COALESCE(?, fecha) WHERE id = ?", v.getEstado(), v.getFecha(), v.getId());
    }
}
