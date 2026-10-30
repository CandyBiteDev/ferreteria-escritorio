package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.ConversionUnidad;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ConversionUnidadDao extends DaoBase<ConversionUnidad> {

    @Override
    protected String tabla() { return "conversion_unidad"; }

    @Override
    protected String consultaBase() {
        return "SELECT t.*, u.nombre AS unidad FROM conversion_unidad t JOIN unidad_medida u ON u.id = t.id_unidad";
    }

    public List<ConversionUnidad> listarPorProducto(Connection cn, int idProducto) throws SQLException {
        return consultar(cn, "WHERE t.id_producto = ? ORDER BY u.nombre", idProducto);
    }

    /** Factor de conversión hacia la unidad base, o null si no existe conversión definida. */
    public BigDecimal factor(Connection cn, int idProducto, int idUnidad) throws SQLException {
        return Jdbc.escalar(cn, "SELECT factor FROM conversion_unidad WHERE id_producto = ? AND id_unidad = ?", idProducto, idUnidad);
    }

    @Override
    protected ConversionUnidad mapear(ResultSet rs) throws SQLException {
        ConversionUnidad c = new ConversionUnidad();
        c.setId(rs.getInt("id"));
        c.setIdProducto(rs.getInt("id_producto"));
        c.setIdUnidad(rs.getInt("id_unidad"));
        c.setUnidad(rs.getString("unidad"));
        c.setFactor(Jdbc.decimal(rs, "factor"));
        return c;
    }

    @Override
    public int insertar(Connection cn, ConversionUnidad c) throws SQLException {
        return Jdbc.insertar(cn, "INSERT INTO conversion_unidad (id_producto, id_unidad, factor) VALUES (?, ?, ?)",
                c.getIdProducto(), c.getIdUnidad(), c.getFactor());
    }

    @Override
    public void actualizar(Connection cn, ConversionUnidad c) throws SQLException {
        Jdbc.ejecutar(cn, "UPDATE conversion_unidad SET id_unidad = ?, factor = ? WHERE id = ?", c.getIdUnidad(), c.getFactor(), c.getId());
    }
}
