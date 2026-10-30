package com.umg.ferreteria.modelo;

import java.math.BigDecimal;

/** Línea de un documento maestro-detalle. */
public abstract class DetalleDocumento extends EntidadBase {

    protected Integer idProducto;
    protected String codigo;
    protected String producto;
    protected BigDecimal cantidad = BigDecimal.ZERO;

    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }

    /** Importe de la línea; cada tipo de detalle lo calcula distinto. */
    public abstract BigDecimal getSubtotal();

    @Override
    public void validar() {
        requerido(idProducto, "un producto");
        positivo(cantidad, "Cantidad");
    }
}
