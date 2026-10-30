package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.Usuario;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class UsuarioDao extends DaoBase<Usuario> {

    @Override
    protected String tabla() { return "usuario"; }

    @Override
    protected String consultaBase() {
        return "SELECT t.id, t.username, t.nombre, t.id_rol, t.id_bodega, t.activo, r.nombre AS rol, b.nombre AS bodega "
                + "FROM usuario t JOIN rol r ON r.id = t.id_rol LEFT JOIN bodega b ON b.id = t.id_bodega";
    }

    @Override
    protected String orden() { return "t.username"; }

    public Optional<Usuario> buscarPorUsername(Connection cn, String username) throws SQLException {
        return consultar(cn, "WHERE t.username = ?", username).stream().findFirst();
    }

    public List<Usuario> listarPorRol(Connection cn, String... roles) throws SQLException {
        String marcas = String.join(",", java.util.Collections.nCopies(roles.length, "?"));
        return consultar(cn, "WHERE r.nombre IN (" + marcas + ") AND t.activo = TRUE ORDER BY t.nombre", (Object[]) roles);
    }

    public String obtenerHash(Connection cn, int idUsuario) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT password_hash FROM usuario WHERE id = ?")) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public void cambiarPassword(Connection cn, int idUsuario, String hash) throws SQLException {
        Jdbc.ejecutar(cn, "UPDATE usuario SET password_hash = ? WHERE id = ?", hash, idUsuario);
    }

    public int contar(Connection cn) throws SQLException {
        return Jdbc.escalar(cn, "SELECT COUNT(*) FROM usuario").intValue();
    }

    @Override
    protected Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setNombre(rs.getString("nombre"));
        u.setIdRol(rs.getInt("id_rol"));
        u.setRol(rs.getString("rol"));
        u.setIdBodega(Jdbc.entero(rs, "id_bodega"));
        u.setBodega(rs.getString("bodega"));
        u.setActivo(rs.getBoolean("activo"));
        return u;
    }

    /** Inserta sin contraseña; el servicio llama cambiarPassword() en la misma transacción. */
    @Override
    public int insertar(Connection cn, Usuario u) throws SQLException {
        return Jdbc.insertar(cn, "INSERT INTO usuario (username, password_hash, nombre, id_rol, id_bodega, activo) VALUES (?, '', ?, ?, ?, ?)",
                u.getUsername(), u.getNombre(), u.getIdRol(), u.getIdBodega(), u.isActivo());
    }

    @Override
    public void actualizar(Connection cn, Usuario u) throws SQLException {
        Jdbc.ejecutar(cn, "UPDATE usuario SET username = ?, nombre = ?, id_rol = ?, id_bodega = ?, activo = ? WHERE id = ?",
                u.getUsername(), u.getNombre(), u.getIdRol(), u.getIdBodega(), u.isActivo(), u.getId());
    }
}
