package com.umg.ferreteria.modelo;

import java.math.BigDecimal;

/** Stock de un producto en una bodega (vista enriquecida para consulta). */
public class Existencia extends EntidadBase {

    private Integer idProducto;
    private String codigo;
    private String producto;
    private String categoria;
    private String unidad;
    private Integer idBodega;
    private String bodega;
    private BigDecimal cantidad = BigDecimal.ZERO;
    private int stockMinimo;

    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public Integer getIdBodega() { return idBodega; }
    public void setIdBodega(Integer idBodega) { this.idBodega = idBodega; }
    public String getBodega() { return bodega; }
    public void setBodega(String bodega) { this.bodega = bodega; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }

    public boolean isBajoMinimo() {
        return cantidad.compareTo(BigDecimal.valueOf(stockMinimo)) < 0;
    }

    @Override
    public void validar() {
        noNegativo(cantidad, "Cantidad");
    }
}
