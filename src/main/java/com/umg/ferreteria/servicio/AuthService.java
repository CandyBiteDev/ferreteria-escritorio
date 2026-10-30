package com.umg.ferreteria.servicio;

import com.umg.ferreteria.dao.AuditoriaDao;
import com.umg.ferreteria.dao.ConexionBD;
import com.umg.ferreteria.dao.UsuarioDao;
import com.umg.ferreteria.excepcion.AccesoDenegadoException;
import com.umg.ferreteria.excepcion.AutenticacionException;
import com.umg.ferreteria.modelo.Usuario;
import com.umg.ferreteria.seguridad.PasswordUtil;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final ConexionBD db;
    private final UsuarioDao usuarioDao;
    private final AuditoriaDao auditoriaDao;

    public AuthService(ConexionBD db, UsuarioDao usuarioDao, AuditoriaDao auditoriaDao) {
        this.db = db;
        this.usuarioDao = usuarioDao;
        this.auditoriaDao = auditoriaDao;
    }

    /** Verifica usuario y contraseña (hash PBKDF2) y registra el intento en auditoría. */
    public Usuario autenticar(String username, String password, String origen) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new AutenticacionException("Ingrese usuario y contraseña.");
        }
        String user = username.trim().toLowerCase();
        return db.transaccion(cn -> {
            Optional<Usuario> u = usuarioDao.buscarPorUsername(cn, user);
            boolean ok = u.isPresent() && u.get().isActivo()
                    && PasswordUtil.verificar(password, usuarioDao.obtenerHash(cn, u.get().getId()));
            auditoriaDao.registrar(cn, u.map(Usuario::getId).orElse(null), ok ? "LOGIN" : "LOGIN_FALLIDO", "Usuario",
                    "Usuario '" + user + "' desde " + origen);
            if (!ok) {
                // El commit guarda el intento fallido; luego se informa el error
                cn.commit();
                throw new AutenticacionException("Usuario o contraseña incorrectos.");
            }
            return u.get();
        });
    }

    /** Valida las credenciales de un supervisor/administrador para autorizar una operación. */
    public Usuario autorizar(String username, String password) {
        Usuario u = autenticar(username, password, "autorización");
        if (!u.puedeAutorizar()) {
            throw new AccesoDenegadoException("El usuario " + u.getUsername() + " no tiene permiso para autorizar.");
        }
        return u;
    }
}
