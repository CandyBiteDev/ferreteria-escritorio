package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.Sucursal;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class SucursalDao extends DaoBase<Sucursal> {

    @Override
    protected String tabla() { return "sucursal"; }

    @Override
    protected String orden() { return "t.nombre"; }

    @Override
    protected Sucursal mapear(ResultSet rs) throws SQLException {
        Sucursal s = new Sucursal();
        s.setId(rs.getInt("id"));
        s.setNombre(rs.getString("nombre"));
        s.setDireccion(rs.getString("direccion"));
        s.setTelefono(rs.getString("telefono"));
        s.setActivo(rs.getBoolean("activo"));
        return s;
    }

    @Override
    public int insertar(Connection cn, Sucursal s) throws SQLException {
        return Jdbc.insertar(cn, "INSERT INTO sucursal (nombre, direccion, telefono, activo) VALUES (?, ?, ?, ?)",
                s.getNombre(), s.getDireccion(), s.getTelefono(), s.isActivo());
    }

    @Override
    public void actualizar(Connection cn, Sucursal s) throws SQLException {
        Jdbc.ejecutar(cn, "UPDATE sucursal SET nombre = ?, direccion = ?, telefono = ?, activo = ? WHERE id = ?",
                s.getNombre(), s.getDireccion(), s.getTelefono(), s.isActivo(), s.getId());
    }
}
