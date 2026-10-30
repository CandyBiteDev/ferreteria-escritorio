package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.AjusteInventario;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class AjusteInventarioDao extends DaoBase<AjusteInventario> {

    @Override
    protected String tabla() { return "ajuste_inventario"; }

    @Override
    protected String consultaBase() {
        return "SELECT t.*, p.nombre AS producto, b.nombre AS bodega, us.nombre AS solicita, ua.nombre AS autoriza "
                + "FROM ajuste_inventario t JOIN producto p ON p.id = t.id_producto JOIN bodega b ON b.id = t.id_bodega "
                + "JOIN usuario us ON us.id = t.id_usuario_solicita JOIN usuario ua ON ua.id = t.id_usuario_autoriza";
    }

    @Override
    protected String orden() { return "t.id DESC"; }

    @Override
    protected AjusteInventario mapear(ResultSet rs) throws SQLException {
        AjusteInventario a = new AjusteInventario();
        a.setId(rs.getInt("id"));
        a.setFecha(Jdbc.fecha(rs, "fecha"));
        a.setIdProducto(rs.getInt("id_producto"));
        a.setProducto(rs.getString("producto"));
        a.setIdBodega(rs.getInt("id_bodega"));
        a.setBodega(rs.getString("bodega"));
        a.setCantidad(Jdbc.decimal(rs, "cantidad"));
        a.setMotivo(rs.getString("motivo"));
        a.setIdUsuarioSolicita(rs.getInt("id_usuario_solicita"));
        a.setSolicita(rs.getString("solicita"));
        a.setIdUsuarioAutoriza(rs.getInt("id_usuario_autoriza"));
        a.setAutoriza(rs.getString("autoriza"));
        return a;
    }

    @Override
    public int insertar(Connection cn, AjusteInventario a) throws SQLException {
        return Jdbc.insertar(cn, "INSERT INTO ajuste_inventario (id_producto, id_bodega, cantidad, motivo, id_usuario_solicita, id_usuario_autoriza) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                a.getIdProducto(), a.getIdBodega(), a.getCantidad(), a.getMotivo(), a.getIdUsuarioSolicita(), a.getIdUsuarioAutoriza());
    }

    @Override
    public void actualizar(Connection cn, AjusteInventario a) throws SQLException {
        throw new UnsupportedOperationException("Un ajuste registrado no se modifica; registre un ajuste inverso.");
    }
}
