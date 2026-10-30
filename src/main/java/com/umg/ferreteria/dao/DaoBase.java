package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.EntidadBase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Implementación común de listar / buscar / eliminar. Las subclases solo
 * definen la tabla, el SELECT (alias "t") y cómo mapear una fila.
 */
public abstract class DaoBase<T extends EntidadBase> implements Dao<T> {

    protected abstract String tabla();

    protected abstract T mapear(ResultSet rs) throws SQLException;

    protected String consultaBase() {
        return "SELECT t.* FROM " + tabla() + " t";
    }

    protected String orden() {
        return "t.id";
    }

    @Override
    public List<T> listar(Connection cn) throws SQLException {
        return Jdbc.lista(cn, consultaBase() + " ORDER BY " + orden(), this::mapear);
    }

    @Override
    public Optional<T> buscarPorId(Connection cn, int id) throws SQLException {
        return Jdbc.lista(cn, consultaBase() + " WHERE t.id = ?", this::mapear, id).stream().findFirst();
    }

    @Override
    public void eliminar(Connection cn, int id) throws SQLException {
        Jdbc.ejecutar(cn, "DELETE FROM " + tabla() + " WHERE id = ?", id);
    }

    protected List<T> consultar(Connection cn, String sqlDespuesDeBase, Object... params) throws SQLException {
        return Jdbc.lista(cn, consultaBase() + " " + sqlDespuesDeBase, this::mapear, params);
    }
}
