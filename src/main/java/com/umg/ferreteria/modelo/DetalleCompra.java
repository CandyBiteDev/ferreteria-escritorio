package com.umg.ferreteria.modelo;

import java.math.BigDecimal;

public class DetalleCompra extends DetalleDocumento {

    private BigDecimal costoUnitario = BigDecimal.ZERO;

    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }

    @Override
    public BigDecimal getSubtotal() {
        return cantidad.multiply(costoUnitario);
    }

    @Override
    public void validar() {
        super.validar();
        noNegativo(costoUnitario, "Costo unitario");
    }
}
