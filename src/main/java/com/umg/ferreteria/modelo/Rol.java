package com.umg.ferreteria.modelo;

public class Rol extends CatalogoSimple {

    public static final String ADMIN = "ADMIN";
    public static final String SUPERVISOR = "SUPERVISOR";
    public static final String VENDEDOR = "VENDEDOR";
    public static final String ENCARGADO = "ENCARGADO";

    private String descripcion;

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
