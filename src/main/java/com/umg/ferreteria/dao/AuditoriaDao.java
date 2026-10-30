package com.umg.ferreteria.dao;

import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.SQLException;

@Repository
public class AuditoriaDao {

    public void registrar(Connection cn, Integer idUsuario, String accion, String entidad, String detalle) throws SQLException {
        String d = detalle == null ? null : detalle.length() > 500 ? detalle.substring(0, 500) : detalle;
        Jdbc.ejecutar(cn, "INSERT INTO auditoria (id_usuario, accion, entidad, detalle) VALUES (?, ?, ?, ?)", idUsuario, accion, entidad, d);
    }
}
