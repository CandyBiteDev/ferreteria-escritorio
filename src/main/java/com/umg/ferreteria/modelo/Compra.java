package com.umg.ferreteria.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Compra extends Documento<DetalleCompra> {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String RECIBIDA = "RECIBIDA";
    public static final String ANULADA = "ANULADA";

    private Integer idProveedor;
    private String proveedor;
    private Integer idBodega;
    private String bodega;
    private LocalDateTime fechaRecepcion;
    private BigDecimal total = BigDecimal.ZERO;

    public Integer getIdProveedor() { return idProveedor; }
    public void setIdProveedor(Integer idProveedor) { this.idProveedor = idProveedor; }
    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }
    public Integer getIdBodega() { return idBodega; }
    public void setIdBodega(Integer idBodega) { this.idBodega = idBodega; }
    public String getBodega() { return bodega; }
    public void setBodega(String bodega) { this.bodega = bodega; }
    public LocalDateTime getFechaRecepcion() { return fechaRecepcion; }
    public void setFechaRecepcion(LocalDateTime fechaRecepcion) { this.fechaRecepcion = fechaRecepcion; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    @Override
    public String getTipoDocumento() {
        return "Compra";
    }

    @Override
    public void validar() {
        requerido(idProveedor, "un proveedor");
        requerido(idBodega, "la bodega de recepción");
        super.validar();
    }
}
