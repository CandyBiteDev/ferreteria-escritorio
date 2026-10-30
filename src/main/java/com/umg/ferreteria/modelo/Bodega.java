package com.umg.ferreteria.modelo;

public class Bodega extends EntidadBase {

    private String nombre;
    private String ubicacion;
    private Integer idSucursal;
    private String sucursal;
    private boolean activo = true;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public Integer getIdSucursal() { return idSucursal; }
    public void setIdSucursal(Integer idSucursal) { this.idSucursal = idSucursal; }
    public String getSucursal() { return sucursal; }
    public void setSucursal(String sucursal) { this.sucursal = sucursal; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public void validar() {
        requerido(nombre, "Nombre");
        requerido(idSucursal, "una sucursal");
    }

    @Override
    public String toString() {
        return nombre + (sucursal != null ? " — " + sucursal : "");
    }
}
