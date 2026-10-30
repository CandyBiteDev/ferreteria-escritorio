package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.Categoria;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class CategoriaDao extends DaoBase<Categoria> {

    @Override
    protected String tabla() { return "categoria"; }

    @Override
    protected String orden() { return "t.nombre"; }

    @Override
    protected Categoria mapear(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setId(rs.getInt("id"));
        c.setNombre(rs.getString("nombre"));
        return c;
    }

    @Override
    public int insertar(Connection cn, Categoria c) throws SQLException {
        return Jdbc.insertar(cn, "INSERT INTO categoria (nombre) VALUES (?)", c.getNombre());
    }

    @Override
    public void actualizar(Connection cn, Categoria c) throws SQLException {
        Jdbc.ejecutar(cn, "UPDATE categoria SET nombre = ? WHERE id = ?", c.getNombre(), c.getId());
    }
}
