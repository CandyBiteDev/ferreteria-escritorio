package com.umg.ferreteria.modelo;

public class Sucursal extends EntidadBase {

    private String nombre;
    private String direccion;
    private String telefono;
    private boolean activo = true;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public void validar() {
        requerido(nombre, "Nombre");
        maximo(nombre, 80, "Nombre");
    }

    @Override
    public String toString() {
        return nombre;
    }
}
