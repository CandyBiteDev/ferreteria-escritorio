package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.UnidadMedida;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class UnidadMedidaDao extends DaoBase<UnidadMedida> {

    @Override
    protected String tabla() { return "unidad_medida"; }

    @Override
    protected String orden() { return "t.nombre"; }

    @Override
    protected UnidadMedida mapear(ResultSet rs) throws SQLException {
        UnidadMedida u = new UnidadMedida();
        u.setId(rs.getInt("id"));
        u.setNombre(rs.getString("nombre"));
        u.setAbreviatura(rs.getString("abreviatura"));
        return u;
    }

    @Override
    public int insertar(Connection cn, UnidadMedida u) throws SQLException {
        return Jdbc.insertar(cn, "INSERT INTO unidad_medida (nombre, abreviatura) VALUES (?, ?)",
                u.getNombre(), u.getAbreviatura());
    }

    @Override
    public void actualizar(Connection cn, UnidadMedida u) throws SQLException {
        Jdbc.ejecutar(cn, "UPDATE unidad_medida SET nombre = ?, abreviatura = ? WHERE id = ?",
                u.getNombre(), u.getAbreviatura(), u.getId());
    }
}
