package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.Producto;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ProductoDao extends DaoBase<Producto> {

    @Override
    protected String tabla() { return "producto"; }

    @Override
    protected String consultaBase() {
        return "SELECT t.*, c.nombre AS categoria, m.nombre AS marca, u.nombre AS unidad FROM producto t "
                + "JOIN categoria c ON c.id = t.id_categoria "
                + "JOIN marca m ON m.id = t.id_marca "
                + "JOIN unidad_medida u ON u.id = t.id_unidad";
    }

    @Override
    protected String orden() { return "t.codigo"; }

    public List<Producto> listarActivos(Connection cn) throws SQLException {
        return consultar(cn, "WHERE t.activo = TRUE ORDER BY t.codigo");
    }

    @Override
    protected Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getInt("id"));
        p.setCodigo(rs.getString("codigo"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setPrecioCosto(rs.getBigDecimal("precio_costo"));
        p.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        p.setStockMinimo(rs.getInt("stock_minimo"));
        p.setIdCategoria(rs.getInt("id_categoria"));
        p.setCategoria(rs.getString("categoria"));
        p.setIdMarca(rs.getInt("id_marca"));
        p.setMarca(rs.getString("marca"));
        p.setIdUnidad(rs.getInt("id_unidad"));
        p.setUnidad(rs.getString("unidad"));
        p.setActivo(rs.getBoolean("activo"));
        return p;
    }

    @Override
    public int insertar(Connection cn, Producto p) throws SQLException {
        return Jdbc.insertar(cn, "INSERT INTO producto (codigo, nombre, descripcion, precio_costo, precio_venta, stock_minimo, "
                        + "id_categoria, id_marca, id_unidad, activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                p.getCodigo(), p.getNombre(), p.getDescripcion(), p.getPrecioCosto(), p.getPrecioVenta(), p.getStockMinimo(),
                p.getIdCategoria(), p.getIdMarca(), p.getIdUnidad(), p.isActivo());
    }

    @Override
    public void actualizar(Connection cn, Producto p) throws SQLException {
        Jdbc.ejecutar(cn, "UPDATE producto SET codigo = ?, nombre = ?, descripcion = ?, precio_costo = ?, precio_venta = ?, "
                        + "stock_minimo = ?, id_categoria = ?, id_marca = ?, id_unidad = ?, activo = ? WHERE id = ?",
                p.getCodigo(), p.getNombre(), p.getDescripcion(), p.getPrecioCosto(), p.getPrecioVenta(), p.getStockMinimo(),
                p.getIdCategoria(), p.getIdMarca(), p.getIdUnidad(), p.isActivo(), p.getId());
    }

    public void actualizarCosto(Connection cn, int idProducto, java.math.BigDecimal costo) throws SQLException {
        Jdbc.ejecutar(cn, "UPDATE producto SET precio_costo = ? WHERE id = ?", costo, idProducto);
    }
}
