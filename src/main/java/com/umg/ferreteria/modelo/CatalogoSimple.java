package com.umg.ferreteria.modelo;

/** Catálogo que solo tiene nombre (Categoría, Marca). */
public abstract class CatalogoSimple extends EntidadBase {

    protected String nombre;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre == null ? null : nombre.trim(); }

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
