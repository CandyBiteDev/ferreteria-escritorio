package com.umg.ferreteria.servicio;

/** Reportes disponibles. Los cinco primeros son los obligatorios del enunciado. */
public enum TipoReporte {
    ROTACION("Rotación de inventario", true, true),
    SIN_MOVIMIENTO("Productos sin movimiento", false, true),
    EXISTENCIAS_BODEGA("Existencias por bodega", false, true),
    RENTABILIDAD_CATEGORIA("Rentabilidad estimada por categoría", true, false),
    TRANSFERENCIAS_PENDIENTES("Transferencias pendientes", false, true),
    STOCK_MINIMO("Productos bajo stock mínimo", false, true),
    VENTAS_PERIODO("Ventas por período y vendedor", true, true),
    COMPRAS_PROVEEDOR("Compras por proveedor", true, false),
    DEVOLUCIONES("Devoluciones por motivo", true, false),
    AJUSTES("Ajustes de inventario", true, true),
    AUDITORIA("Bitácora de auditoría", true, false);

    private final String titulo;
    private final boolean usaFechas;
    private final boolean usaBodega;

    TipoReporte(String titulo, boolean usaFechas, boolean usaBodega) {
        this.titulo = titulo;
        this.usaFechas = usaFechas;
        this.usaBodega = usaBodega;
    }

    public String getTitulo() { return titulo; }
    public boolean usaFechas() { return usaFechas; }
    public boolean usaBodega() { return usaBodega; }

    @Override
    public String toString() {
        return titulo;
    }
}
