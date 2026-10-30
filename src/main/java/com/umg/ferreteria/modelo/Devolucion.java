package com.umg.ferreteria.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Devolucion extends EntidadBase {

    private LocalDateTime fecha;
    private Integer idVenta;
    private Integer idProducto;
    private String producto;
    private BigDecimal cantidadBase = BigDecimal.ZERO;
    private String motivo;
    private Integer idUsuario;
    private String usuario;

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Integer getIdVenta() { return idVenta; }
    public void setIdVenta(Integer idVenta) { this.idVenta = idVenta; }
    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }
    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }
    public BigDecimal getCantidadBase() { return cantidadBase; }
    public void setCantidadBase(BigDecimal cantidadBase) { this.cantidadBase = cantidadBase; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    @Override
    public void validar() {
        requerido(idVenta, "la venta");
        requerido(idProducto, "un producto");
        positivo(cantidadBase, "Cantidad");
        requerido(motivo, "Motivo");
    }
}
