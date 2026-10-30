package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.Devolucion;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class DevolucionDao extends DaoBase<Devolucion> {

    @Override
    protected String tabla() { return "devolucion"; }

    @Override
    protected String consultaBase() {
        return "SELECT t.*, p.nombre AS producto, u.nombre AS usuario FROM devolucion t "
                + "JOIN producto p ON p.id = t.id_producto JOIN usuario u ON u.id = t.id_usuario";
    }

    @Override
    protected String orden() { return "t.id DESC"; }

    public List<Devolucion> listarPorVenta(Connection cn, int idVenta) throws SQLException {
        return consultar(cn, "WHERE t.id_venta = ? ORDER BY t.id", idVenta);
    }

    @Override
    protected Devolucion mapear(ResultSet rs) throws SQLException {
        Devolucion d = new Devolucion();
        d.setId(rs.getInt("id"));
        d.setFecha(Jdbc.fecha(rs, "fecha"));
        d.setIdVenta(rs.getInt("id_venta"));
        d.setIdProducto(rs.getInt("id_producto"));
        d.setProducto(rs.getString("producto"));
        d.setCantidadBase(Jdbc.decimal(rs, "cantidad_base"));
        d.setMotivo(rs.getString("motivo"));
        d.setIdUsuario(rs.getInt("id_usuario"));
        d.setUsuario(rs.getString("usuario"));
        return d;
    }

    @Override
    public int insertar(Connection cn, Devolucion d) throws SQLException {
        return Jdbc.insertar(cn, "INSERT INTO devolucion (id_venta, id_producto, cantidad_base, motivo, id_usuario) VALUES (?, ?, ?, ?, ?)",
                d.getIdVenta(), d.getIdProducto(), d.getCantidadBase(), d.getMotivo(), d.getIdUsuario());
    }

    @Override
    public void actualizar(Connection cn, Devolucion d) throws SQLException {
        throw new UnsupportedOperationException("Una devolución registrada no se modifica.");
    }
}
