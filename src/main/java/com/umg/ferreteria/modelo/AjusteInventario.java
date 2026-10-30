package com.umg.ferreteria.modelo;

import com.umg.ferreteria.excepcion.ValidacionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Regla: todo ajuste requiere motivo y la autorización de un usuario con permisos. */
public class AjusteInventario extends EntidadBase {

    private LocalDateTime fecha;
    private Integer idProducto;
    private String producto;
    private Integer idBodega;
    private String bodega;
    private BigDecimal cantidad = BigDecimal.ZERO;
    private String motivo;
    private Integer idUsuarioSolicita;
    private String solicita;
    private Integer idUsuarioAutoriza;
    private String autoriza;

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }
    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }
    public Integer getIdBodega() { return idBodega; }
    public void setIdBodega(Integer idBodega) { this.idBodega = idBodega; }
    public String getBodega() { return bodega; }
    public void setBodega(String bodega) { this.bodega = bodega; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Integer getIdUsuarioSolicita() { return idUsuarioSolicita; }
    public void setIdUsuarioSolicita(Integer idUsuarioSolicita) { this.idUsuarioSolicita = idUsuarioSolicita; }
    public String getSolicita() { return solicita; }
    public void setSolicita(String solicita) { this.solicita = solicita; }
    public Integer getIdUsuarioAutoriza() { return idUsuarioAutoriza; }
    public void setIdUsuarioAutoriza(Integer idUsuarioAutoriza) { this.idUsuarioAutoriza = idUsuarioAutoriza; }
    public String getAutoriza() { return autoriza; }
    public void setAutoriza(String autoriza) { this.autoriza = autoriza; }

    @Override
    public void validar() {
        requerido(idProducto, "un producto");
        requerido(idBodega, "una bodega");
        requerido(motivo, "Motivo");
        if (motivo.trim().length() < 5) throw new ValidacionException("Describa el motivo del ajuste (mínimo 5 caracteres).");
        if (cantidad == null || cantidad.signum() == 0) throw new ValidacionException("La cantidad del ajuste no puede ser cero.");
    }
}
