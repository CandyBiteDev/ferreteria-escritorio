package com.umg.ferreteria.modelo;

import java.math.BigDecimal;

public class DetalleTransferencia extends DetalleDocumento {

    private String unidad;

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    /** Una transferencia no tiene importe: solo mueve unidades. */
    @Override
    public BigDecimal getSubtotal() {
        return BigDecimal.ZERO;
    }
}
