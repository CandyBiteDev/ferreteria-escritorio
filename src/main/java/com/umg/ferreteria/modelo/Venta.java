package com.umg.ferreteria.modelo;

import java.math.BigDecimal;

/** Una cotización es una venta en estado COTIZACION que aún no descuenta inventario. */
public class Venta extends Documento<DetalleVenta> {

    public static final String COTIZACION = "COTIZACION";
    public static final String CONFIRMADA = "CONFIRMADA";
    public static final String ANULADA = "ANULADA";

    private Integer idCliente;
    private String cliente;
    private Integer idBodega;
    private String bodega;
    private BigDecimal total = BigDecimal.ZERO;

    public Integer getIdCliente() { return idCliente; }
    public void setIdCliente(Integer idCliente) { this.idCliente = idCliente; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public Integer getIdBodega() { return idBodega; }
    public void setIdBodega(Integer idBodega) { this.idBodega = idBodega; }
    public String getBodega() { return bodega; }
    public void setBodega(String bodega) { this.bodega = bodega; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    @Override
    public String getTipoDocumento() {
        return COTIZACION.equals(estado) ? "Cotización" : "Venta";
    }

    @Override
    public void validar() {
        requerido(idCliente, "un cliente");
        requerido(idBodega, "la bodega de despacho");
        super.validar();
    }
}
