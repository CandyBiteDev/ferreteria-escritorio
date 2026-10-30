package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.EntidadBase;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Contrato DAO genérico (patrón DAO). La conexión la provee la capa de servicio. */
public interface Dao<T extends EntidadBase> {

    List<T> listar(Connection cn) throws SQLException;

    Optional<T> buscarPorId(Connection cn, int id) throws SQLException;

    int insertar(Connection cn, T entidad) throws SQLException;

    void actualizar(Connection cn, T entidad) throws SQLException;

    void eliminar(Connection cn, int id) throws SQLException;
}
