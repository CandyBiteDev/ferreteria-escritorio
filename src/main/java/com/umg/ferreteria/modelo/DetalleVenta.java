package com.umg.ferreteria.modelo;

import java.math.BigDecimal;

public class DetalleVenta extends DetalleDocumento {

    private Integer idUnidad;
    private String unidad;
    private BigDecimal cantidadBase = BigDecimal.ZERO;
    private BigDecimal precioUnitario = BigDecimal.ZERO;
    private BigDecimal costoUnitario = BigDecimal.ZERO;

    public Integer getIdUnidad() { return idUnidad; }
    public void setIdUnidad(Integer idUnidad) { this.idUnidad = idUnidad; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public BigDecimal getCantidadBase() { return cantidadBase; }
    public void setCantidadBase(BigDecimal cantidadBase) { this.cantidadBase = cantidadBase; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }

    @Override
    public BigDecimal getSubtotal() {
        return cantidad.multiply(precioUnitario);
    }

    @Override
    public void validar() {
        super.validar();
        requerido(idUnidad, "la unidad de venta");
    }
}
