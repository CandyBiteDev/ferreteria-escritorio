package com.umg.ferreteria.modelo;

import com.umg.ferreteria.excepcion.ValidacionException;

/** Datos comunes de terceros (Cliente y Proveedor heredan de aquí). */
public abstract class Persona extends EntidadBase {

    protected String nombre;
    protected String nit;
    protected String telefono;
    protected String correo;
    protected String direccion;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit == null ? null : nit.trim().toUpperCase(); }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    /** "Cliente" o "Proveedor": cada subclase lo define. */
    public abstract String getTipoPersona();

    @Override
    public void validar() {
        requerido(nombre, "Nombre");
        requerido(nit, "NIT");
        maximo(nombre, 120, "Nombre");
        if (!nit.equals("CF") && !nit.matches("[0-9]+-?[0-9K]")) {
            throw new ValidacionException("NIT inválido. Formato: 1234567-8 o CF.");
        }
        if (correo != null && !correo.isBlank() && !correo.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            throw new ValidacionException("Correo electrónico inválido.");
        }
        if (telefono != null && !telefono.isBlank() && !telefono.matches("[0-9 +-]{8,20}")) {
            throw new ValidacionException("Teléfono inválido (mínimo 8 dígitos).");
        }
    }

    @Override
    public String toString() {
        return nombre + " (" + nit + ")";
    }
}
