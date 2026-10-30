package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.Compra;
import com.umg.ferreteria.modelo.DetalleCompra;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class CompraDao extends DaoBase<Compra> {

    @Override
    protected String tabla() { return "compra"; }

    @Override
    protected String consultaBase() {
        return "SELECT t.*, p.nombre AS proveedor, b.nombre AS bodega, u.nombre AS usuario FROM compra t "
                + "JOIN proveedor p ON p.id = t.id_proveedor "
                + "JOIN bodega b ON b.id = t.id_bodega "
                + "JOIN usuario u ON u.id = t.id_usuario";
    }

    @Override
    protected String orden() { return "t.id DESC"; }

    @Override
    public Optional<Compra> buscarPorId(Connection cn, int id) throws SQLException {
        Optional<Compra> c = super.buscarPorId(cn, id);
        if (c.isPresent()) {
            for (DetalleCompra d : detalles(cn, id)) c.get().agregarDetalle(d);
        }
        return c;
    }

    public List<DetalleCompra> detalles(Connection cn, int idCompra) throws SQLException {
        return Jdbc.lista(cn, "SELECT d.*, p.codigo, p.nombre AS producto FROM detalle_compra d "
                + "JOIN producto p ON p.id = d.id_producto WHERE d.id_compra = ? ORDER BY d.id", rs -> {
            DetalleCompra d = new DetalleCompra();
            d.setId(rs.getInt("id"));
            d.setIdProducto(rs.getInt("id_producto"));
            d.setCodigo(rs.getString("codigo"));
            d.setProducto(rs.getString("producto"));
            d.setCantidad(Jdbc.decimal(rs, "cantidad"));
            d.setCostoUnitario(rs.getBigDecimal("costo_unitario"));
            return d;
        }, idCompra);
    }

    @Override
    protected Compra mapear(ResultSet rs) throws SQLException {
        Compra c = new Compra();
        c.setId(rs.getInt("id"));
        c.setFecha(Jdbc.fecha(rs, "fecha"));
        c.setIdProveedor(rs.getInt("id_proveedor"));
        c.setProveedor(rs.getString("proveedor"));
        c.setIdBodega(rs.getInt("id_bodega"));
        c.setBodega(rs.getString("bodega"));
        c.setIdUsuario(rs.getInt("id_usuario"));
        c.setUsuario(rs.getString("usuario"));
        c.setEstado(rs.getString("estado"));
        c.setFechaRecepcion(Jdbc.fecha(rs, "fecha_recepcion"));
        c.setTotal(rs.getBigDecimal("total"));
        return c;
    }

    /** Inserta encabezado y detalles (debe llamarse dentro de una transacción). */
    @Override
    public int insertar(Connection cn, Compra c) throws SQLException {
        int id = Jdbc.insertar(cn, "INSERT INTO compra (id_proveedor, id_bodega, id_usuario, estado, total) VALUES (?, ?, ?, ?, ?)",
                c.getIdProveedor(), c.getIdBodega(), c.getIdUsuario(), c.getEstado(), c.calcularTotal());
        for (DetalleCompra d : c.getDetalles()) {
            Jdbc.ejecutar(cn, "INSERT INTO detalle_compra (id_compra, id_producto, cantidad, costo_unitario) VALUES (?, ?, ?, ?)",
                    id, d.getIdProducto(), d.getCantidad(), d.getCostoUnitario());
        }
        return id;
    }

    /** Solo cambia estado / fecha de recepción: los detalles de un documento no se editan. */
    @Override
    public void actualizar(Connection cn, Compra c) throws SQLException {
        Jdbc.ejecutar(cn, "UPDATE compra SET estado = ?, fecha_recepcion = ? WHERE id = ?", c.getEstado(), c.getFechaRecepcion(), c.getId());
    }
}
