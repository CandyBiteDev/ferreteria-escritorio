package com.umg.ferreteria.excepcion;

import java.math.BigDecimal;

/** Regla: no se permite stock negativo en ninguna bodega. */
public class StockInsuficienteException extends ReglaNegocioException {

    private final BigDecimal disponible;
    private final BigDecimal solicitado;

    public StockInsuficienteException(String producto, String bodega, BigDecimal disponible, BigDecimal solicitado) {
        super("Stock insuficiente de '" + producto + "' en " + bodega
                + ". Disponible: " + disponible.stripTrailingZeros().toPlainString()
                + ", requerido: " + solicitado.stripTrailingZeros().toPlainString());
        this.disponible = disponible;
        this.solicitado = solicitado;
    }

    public BigDecimal getDisponible() { return disponible; }
    public BigDecimal getSolicitado() { return solicitado; }
}
