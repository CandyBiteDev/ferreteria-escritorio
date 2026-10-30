package com.umg.ferreteria.modelo;

import com.umg.ferreteria.excepcion.ValidacionException;

import java.time.LocalDateTime;

/**
 * Regla: salida en origen y recepción en destino con estados separados.
 * EN_TRANSITO (ya salió de origen) -> RECIBIDA (entró a destino) | ANULADA (regresa a origen)
 */
public class Transferencia extends Documento<DetalleTransferencia> {

    public static final String EN_TRANSITO = "EN_TRANSITO";
    public static final String RECIBIDA = "RECIBIDA";
    public static final String ANULADA = "ANULADA";

    private Integer idBodegaOrigen;
    private String bodegaOrigen;
    private Integer idBodegaDestino;
    private String bodegaDestino;
    private Integer idUsuarioRecibe;
    private String usuarioRecibe;
    private LocalDateTime fechaRecepcion;
    private String observacion;

    public Integer getIdBodegaOrigen() { return idBodegaOrigen; }
    public void setIdBodegaOrigen(Integer idBodegaOrigen) { this.idBodegaOrigen = idBodegaOrigen; }
    public String getBodegaOrigen() { return bodegaOrigen; }
    public void setBodegaOrigen(String bodegaOrigen) { this.bodegaOrigen = bodegaOrigen; }
    public Integer getIdBodegaDestino() { return idBodegaDestino; }
    public void setIdBodegaDestino(Integer idBodegaDestino) { this.idBodegaDestino = idBodegaDestino; }
    public String getBodegaDestino() { return bodegaDestino; }
    public void setBodegaDestino(String bodegaDestino) { this.bodegaDestino = bodegaDestino; }
    public Integer getIdUsuarioRecibe() { return idUsuarioRecibe; }
    public void setIdUsuarioRecibe(Integer idUsuarioRecibe) { this.idUsuarioRecibe = idUsuarioRecibe; }
    public String getUsuarioRecibe() { return usuarioRecibe; }
    public void setUsuarioRecibe(String usuarioRecibe) { this.usuarioRecibe = usuarioRecibe; }
    public LocalDateTime getFechaRecepcion() { return fechaRecepcion; }
    public void setFechaRecepcion(LocalDateTime fechaRecepcion) { this.fechaRecepcion = fechaRecepcion; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    @Override
    public String getTipoDocumento() {
        return "Transferencia";
    }

    @Override
    public void validar() {
        requerido(idBodegaOrigen, "la bodega de origen");
        requerido(idBodegaDestino, "la bodega de destino");
        if (idBodegaOrigen.equals(idBodegaDestino)) {
            throw new ValidacionException("La bodega de origen y destino deben ser distintas.");
        }
        super.validar();
    }
}
