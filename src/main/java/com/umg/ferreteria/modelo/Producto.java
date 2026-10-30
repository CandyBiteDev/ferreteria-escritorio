package com.umg.ferreteria.modelo;

import com.umg.ferreteria.excepcion.ValidacionException;

import java.math.BigDecimal;

public class Producto extends EntidadBase {

    private String codigo;
    private String nombre;
    private String descripcion;
    private BigDecimal precioCosto = BigDecimal.ZERO;
    private BigDecimal precioVenta = BigDecimal.ZERO;
    private int stockMinimo;
    private Integer idCategoria;
    private String categoria;
    private Integer idMarca;
    private String marca;
    private Integer idUnidad;
    private String unidad;
    private boolean activo = true;

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo == null ? null : codigo.trim().toUpperCase(); }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPrecioCosto() { return precioCosto; }
    public void setPrecioCosto(BigDecimal precioCosto) { this.precioCosto = precioCosto; }
    public BigDecimal getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(BigDecimal precioVenta) { this.precioVenta = precioVenta; }
    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }
    public Integer getIdCategoria() { return idCategoria; }
    public void setIdCategoria(Integer idCategoria) { this.idCategoria = idCategoria; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public Integer getIdMarca() { return idMarca; }
    public void setIdMarca(Integer idMarca) { this.idMarca = idMarca; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public Integer getIdUnidad() { return idUnidad; }
    public void setIdUnidad(Integer idUnidad) { this.idUnidad = idUnidad; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    /** Margen bruto unitario estimado en porcentaje. */
    public BigDecimal getMargen() {
        if (precioVenta == null || precioVenta.signum() == 0 || precioCosto == null) return BigDecimal.ZERO;
        return precioVenta.subtract(precioCosto).multiply(BigDecimal.valueOf(100))
                .divide(precioVenta, 2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public void validar() {
        requerido(codigo, "Código");
        requerido(nombre, "Nombre");
        maximo(codigo, 30, "Código");
        maximo(nombre, 120, "Nombre");
        noNegativo(precioCosto, "Precio costo");
        positivo(precioVenta, "Precio venta");
        requerido(idCategoria, "una categoría");
        requerido(idMarca, "una marca");
        requerido(idUnidad, "una unidad de medida");
        if (stockMinimo < 0) throw new ValidacionException("El stock mínimo no puede ser negativo.");
        if (precioVenta.compareTo(precioCosto) < 0) {
            throw new ValidacionException("El precio de venta no puede ser menor que el costo.");
        }
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}
