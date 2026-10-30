package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.Existencia;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Existencias por bodega y kardex (movimiento_inventario). */
@Repository
public class ExistenciaDao {

    private static final String BASE =
            "SELECT e.id, e.id_producto, p.codigo, p.nombre AS producto, c.nombre AS categoria, u.abreviatura AS unidad, "
                    + "e.id_bodega, b.nombre AS bodega, e.cantidad, p.stock_minimo "
                    + "FROM existencia e "
                    + "JOIN producto p ON p.id = e.id_producto "
                    + "JOIN categoria c ON c.id = p.id_categoria "
                    + "JOIN unidad_medida u ON u.id = p.id_unidad "
                    + "JOIN bodega b ON b.id = e.id_bodega ";

    /** Búsqueda con filtros opcionales: bodega y texto (código, nombre o categoría). */
    public List<Existencia> buscar(Connection cn, Integer idBodega, String texto) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (idBodega != null) {
            sql.append("AND e.id_bodega = ? ");
            params.add(idBodega);
        }
        if (texto != null && !texto.isBlank()) {
            sql.append("AND (p.codigo LIKE ? OR p.nombre LIKE ? OR c.nombre LIKE ?) ");
            String like = "%" + texto.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append("ORDER BY b.nombre, p.codigo");
        return Jdbc.lista(cn, sql.toString(), this::mapear, params.toArray());
    }

    /** Lee la cantidad bloqueando la fila (SELECT ... FOR UPDATE) hasta el commit. Null si no existe. */
    public BigDecimal cantidadParaActualizar(Connection cn, int idProducto, int idBodega) throws SQLException {
        return Jdbc.escalar(cn, "SELECT cantidad FROM existencia WHERE id_producto = ? AND id_bodega = ? FOR UPDATE",
                idProducto, idBodega);
    }

    public BigDecimal disponible(Connection cn, int idProducto, int idBodega) throws SQLException {
        BigDecimal v = Jdbc.escalar(cn, "SELECT cantidad FROM existencia WHERE id_producto = ? AND id_bodega = ?", idProducto, idBodega);
        return v == null ? BigDecimal.ZERO : v;
    }

    public void insertar(Connection cn, int idProducto, int idBodega, BigDecimal cantidad) throws SQLException {
        Jdbc.ejecutar(cn, "INSERT INTO existencia (id_producto, id_bodega, cantidad) VALUES (?, ?, ?)", idProducto, idBodega, cantidad);
    }

    public void actualizar(Connection cn, int idProducto, int idBodega, BigDecimal cantidad) throws SQLException {
        Jdbc.ejecutar(cn, "UPDATE existencia SET cantidad = ? WHERE id_producto = ? AND id_bodega = ?", cantidad, idProducto, idBodega);
    }

    public void registrarMovimiento(Connection cn, int idProducto, int idBodega, String tipo, BigDecimal cantidad,
                                    BigDecimal saldo, String referencia) throws SQLException {
        Jdbc.ejecutar(cn, "INSERT INTO movimiento_inventario (id_producto, id_bodega, tipo, cantidad, saldo, referencia) "
                + "VALUES (?, ?, ?, ?, ?, ?)", idProducto, idBodega, tipo, cantidad, saldo, referencia);
    }

    private Existencia mapear(ResultSet rs) throws SQLException {
        Existencia e = new Existencia();
        e.setId(rs.getInt("id"));
        e.setIdProducto(rs.getInt("id_producto"));
        e.setCodigo(rs.getString("codigo"));
        e.setProducto(rs.getString("producto"));
        e.setCategoria(rs.getString("categoria"));
        e.setUnidad(rs.getString("unidad"));
        e.setIdBodega(rs.getInt("id_bodega"));
        e.setBodega(rs.getString("bodega"));
        e.setCantidad(Jdbc.decimal(rs, "cantidad"));
        e.setStockMinimo(rs.getInt("stock_minimo"));
        return e;
    }
}
