package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.Rol;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class RolDao extends DaoBase<Rol> {

    @Override
    protected String tabla() { return "rol"; }

    @Override
    protected Rol mapear(ResultSet rs) throws SQLException {
        Rol r = new Rol();
        r.setId(rs.getInt("id"));
        r.setNombre(rs.getString("nombre"));
        r.setDescripcion(rs.getString("descripcion"));
        return r;
    }

    @Override
    public int insertar(Connection cn, Rol r) throws SQLException {
        return Jdbc.insertar(cn, "INSERT INTO rol (nombre, descripcion) VALUES (?, ?)", r.getNombre(), r.getDescripcion());
    }

    @Override
    public void actualizar(Connection cn, Rol r) throws SQLException {
        Jdbc.ejecutar(cn, "UPDATE rol SET nombre = ?, descripcion = ? WHERE id = ?", r.getNombre(), r.getDescripcion(), r.getId());
    }
}
