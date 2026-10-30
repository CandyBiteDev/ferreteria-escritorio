package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.Bodega;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class BodegaDao extends DaoBase<Bodega> {

    @Override
    protected String tabla() { return "bodega"; }

    @Override
    protected String consultaBase() {
        return "SELECT t.*, s.nombre AS sucursal FROM bodega t JOIN sucursal s ON s.id = t.id_sucursal";
    }

    @Override
    protected String orden() { return "s.nombre, t.nombre"; }

    @Override
    protected Bodega mapear(ResultSet rs) throws SQLException {
        Bodega b = new Bodega();
        b.setId(rs.getInt("id"));
        b.setNombre(rs.getString("nombre"));
        b.setUbicacion(rs.getString("ubicacion"));
        b.setIdSucursal(rs.getInt("id_sucursal"));
        b.setSucursal(rs.getString("sucursal"));
        b.setActivo(rs.getBoolean("activo"));
        return b;
    }

    @Override
    public int insertar(Connection cn, Bodega b) throws SQLException {
        return Jdbc.insertar(cn, "INSERT INTO bodega (nombre, ubicacion, id_sucursal, activo) VALUES (?, ?, ?, ?)",
                b.getNombre(), b.getUbicacion(), b.getIdSucursal(), b.isActivo());
    }

    @Override
    public void actualizar(Connection cn, Bodega b) throws SQLException {
        Jdbc.ejecutar(cn, "UPDATE bodega SET nombre = ?, ubicacion = ?, id_sucursal = ?, activo = ? WHERE id = ?",
                b.getNombre(), b.getUbicacion(), b.getIdSucursal(), b.isActivo(), b.getId());
    }
}
