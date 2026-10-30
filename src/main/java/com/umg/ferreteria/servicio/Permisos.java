package com.umg.ferreteria.servicio;

import com.umg.ferreteria.excepcion.AccesoDenegadoException;
import com.umg.ferreteria.excepcion.AutenticacionException;
import com.umg.ferreteria.modelo.Usuario;

/** Validación de roles del lado del servidor (no basta con ocultar botones en la vista). */
final class Permisos {

    private Permisos() {
    }

    static void requiere(Usuario u, String accion, String... roles) {
        if (u == null) throw new AutenticacionException("Debe iniciar sesión.");
        if (!u.tieneRol(roles)) {
            throw new AccesoDenegadoException("Su rol (" + u.getRol() + ") no permite: " + accion + ".");
        }
    }
}
