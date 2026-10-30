package com.umg.ferreteria.modelo;

import java.math.BigDecimal;

/** 1 [unidad] equivale a [factor] unidades base del producto. */
public class ConversionUnidad extends EntidadBase {

    private Integer idProducto;
    private Integer idUnidad;
    private String unidad;
    private BigDecimal factor;

    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }
    public Integer getIdUnidad() { return idUnidad; }
    public void setIdUnidad(Integer idUnidad) { this.idUnidad = idUnidad; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public BigDecimal getFactor() { return factor; }
    public void setFactor(BigDecimal factor) { this.factor = factor; }

    @Override
    public String toString() {
        return factor != null && factor.compareTo(BigDecimal.ONE) != 0
                ? unidad + " (x" + factor.stripTrailingZeros().toPlainString() + ")"
                : unidad;
    }

    @Override
    public void validar() {
        requerido(idProducto, "un producto");
        requerido(idUnidad, "una unidad");
        positivo(factor, "Factor");
    }
}
