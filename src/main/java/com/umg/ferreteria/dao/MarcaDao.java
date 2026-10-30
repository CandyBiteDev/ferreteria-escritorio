package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.Marca;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class MarcaDao extends DaoBase<Marca> {

    @Override
    protected String tabla() { return "marca"; }

    @Override
    protected String orden() { return "t.nombre"; }

    @Override
    protected Marca mapear(ResultSet rs) throws SQLException {
        Marca m = new Marca();
        m.setId(rs.getInt("id"));
        m.setNombre(rs.getString("nombre"));
        return m;
    }

    @Override
    public int insertar(Connection cn, Marca m) throws SQLException {
        return Jdbc.insertar(cn, "INSERT INTO marca (nombre) VALUES (?)", m.getNombre());
    }

    @Override
    public void actualizar(Connection cn, Marca m) throws SQLException {
        Jdbc.ejecutar(cn, "UPDATE marca SET nombre = ? WHERE id = ?", m.getNombre(), m.getId());
    }
}
