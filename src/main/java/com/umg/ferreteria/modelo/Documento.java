package com.umg.ferreteria.modelo;

import com.umg.ferreteria.excepcion.ValidacionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Documento encabezado-detalle. COMPOSICIÓN: el documento es dueño de sus
 * detalles; un detalle no existe sin su documento (ON DELETE CASCADE en BD).
 */
public abstract class Documento<D extends DetalleDocumento> extends EntidadBase {

    protected LocalDateTime fecha;
    protected Integer idUsuario;
    protected String usuario;
    protected String estado;
    protected final List<D> detalles = new ArrayList<>();

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public List<D> getDetalles() { return detalles; }

    public void agregarDetalle(D detalle) {
        detalles.add(detalle);
    }

    public BigDecimal calcularTotal() {
        return detalles.stream().map(DetalleDocumento::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Nombre del tipo de documento para auditoría y reportes. */
    public abstract String getTipoDocumento();

    @Override
    public void validar() {
        if (detalles.isEmpty()) {
            throw new ValidacionException("El documento debe tener al menos un producto.");
        }
        Set<Integer> productos = new HashSet<>();
        for (D d : detalles) {
            d.validar();
            if (!productos.add(d.getIdProducto())) {
                throw new ValidacionException("El producto " + d.getProducto() + " está repetido en el detalle.");
            }
        }
    }
}
