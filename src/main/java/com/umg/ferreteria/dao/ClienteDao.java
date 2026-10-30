package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.Cliente;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class ClienteDao extends DaoBase<Cliente> {

    @Override
    protected String tabla() { return "cliente"; }

    @Override
    protected String orden() { return "t.nombre"; }

    @Override
    protected Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setNombre(rs.getString("nombre"));
        c.setNit(rs.getString("nit"));
        c.setTelefono(rs.getString("telefono"));
        c.setCorreo(rs.getString("correo"));
        c.setDireccion(rs.getString("direccion"));
        return c;
    }

    @Override
    public int insertar(Connection cn, Cliente c) throws SQLException {
        return Jdbc.insertar(cn, "INSERT INTO cliente (nombre, nit, telefono, correo, direccion) VALUES (?, ?, ?, ?, ?)",
                c.getNombre(), c.getNit(), c.getTelefono(), c.getCorreo(), c.getDireccion());
    }

    @Override
    public void actualizar(Connection cn, Cliente c) throws SQLException {
        Jdbc.ejecutar(cn, "UPDATE cliente SET nombre = ?, nit = ?, telefono = ?, correo = ?, direccion = ? WHERE id = ?",
                c.getNombre(), c.getNit(), c.getTelefono(), c.getCorreo(), c.getDireccion(), c.getId());
    }
}
