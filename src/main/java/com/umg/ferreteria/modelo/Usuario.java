package com.umg.ferreteria.modelo;

import com.umg.ferreteria.excepcion.ValidacionException;

import java.util.Arrays;

/** Usuario del sistema. El hash de contraseña nunca viaja en este objeto. */
public class Usuario extends EntidadBase {

    private String username;
    private String nombre;
    private Integer idRol;
    private String rol;
    private Integer idBodega;
    private String bodega;
    private boolean activo = true;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username == null ? null : username.trim().toLowerCase(); }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getIdRol() { return idRol; }
    public void setIdRol(Integer idRol) { this.idRol = idRol; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public Integer getIdBodega() { return idBodega; }
    public void setIdBodega(Integer idBodega) { this.idBodega = idBodega; }
    public String getBodega() { return bodega; }
    public void setBodega(String bodega) { this.bodega = bodega; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public boolean tieneRol(String... roles) {
        return Arrays.asList(roles).contains(rol);
    }

    public boolean puedeAutorizar() {
        return tieneRol(Rol.ADMIN, Rol.SUPERVISOR);
    }

    @Override
    public void validar() {
        requerido(username, "Usuario");
        requerido(nombre, "Nombre");
        requerido(idRol, "un rol");
        if (!username.matches("[a-z0-9._]{3,40}")) {
            throw new ValidacionException("El usuario solo admite minúsculas, números, punto y guion bajo (3-40).");
        }
    }

    @Override
    public String toString() {
        return nombre + " (" + username + ")";
    }
}
