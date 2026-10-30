package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.Proveedor;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class ProveedorDao extends DaoBase<Proveedor> {

    @Override
    protected String tabla() { return "proveedor"; }

    @Override
    protected String orden() { return "t.nombre"; }

    @Override
    protected Proveedor mapear(ResultSet rs) throws SQLException {
        Proveedor p = new Proveedor();
        p.setId(rs.getInt("id"));
        p.setNombre(rs.getString("nombre"));
        p.setNit(rs.getString("nit"));
        p.setTelefono(rs.getString("telefono"));
        p.setCorreo(rs.getString("correo"));
        p.setDireccion(rs.getString("direccion"));
        p.setContacto(rs.getString("contacto"));
        return p;
    }

    @Override
    public int insertar(Connection cn, Proveedor p) throws SQLException {
        return Jdbc.insertar(cn, "INSERT INTO proveedor (nombre, nit, telefono, correo, direccion, contacto) VALUES (?, ?, ?, ?, ?, ?)",
                p.getNombre(), p.getNit(), p.getTelefono(), p.getCorreo(), p.getDireccion(), p.getContacto());
    }

    @Override
    public void actualizar(Connection cn, Proveedor p) throws SQLException {
        Jdbc.ejecutar(cn, "UPDATE proveedor SET nombre = ?, nit = ?, telefono = ?, correo = ?, direccion = ?, contacto = ? WHERE id = ?",
                p.getNombre(), p.getNit(), p.getTelefono(), p.getCorreo(), p.getDireccion(), p.getContacto(), p.getId());
    }
}
