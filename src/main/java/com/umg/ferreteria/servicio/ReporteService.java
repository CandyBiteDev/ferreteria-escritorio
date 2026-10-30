package com.umg.ferreteria.servicio;

import com.umg.ferreteria.dao.ConexionBD;
import com.umg.ferreteria.dao.ReporteDao;
import com.umg.ferreteria.excepcion.ValidacionException;
import com.umg.ferreteria.modelo.ResultadoReporte;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ReporteService {

    private final ConexionBD db;
    private final ReporteDao dao;

    public ReporteService(ConexionBD db, ReporteDao dao) {
        this.db = db;
        this.dao = dao;
    }

    /**
     * @param desde    fecha inicial (incluida); si es null se toman 30 días atrás
     * @param hasta    fecha final (incluida); si es null hoy
     * @param idBodega null = todas las bodegas
     * @param dias     umbral para "sin movimiento"
     */
    public ResultadoReporte generar(TipoReporte tipo, LocalDate desde, LocalDate hasta, Integer idBodega, int dias) {
        LocalDate h = hasta == null ? LocalDate.now() : hasta;
        LocalDate d = desde == null ? h.minusDays(30) : desde;
        if (d.isAfter(h)) throw new ValidacionException("La fecha inicial no puede ser posterior a la final.");
        LocalDateTime ini = d.atStartOfDay();
        LocalDateTime fin = h.plusDays(1).atStartOfDay();
        String titulo = tipo.getTitulo() + (tipo.usaFechas() ? " (" + d + " a " + h + ")" : "");

        return db.consultar(cn -> switch (tipo) {
            case EXISTENCIAS_BODEGA -> dao.ejecutar(cn, titulo,
                    "SELECT b.nombre AS `Bodega`, p.codigo AS `Código`, p.nombre AS `Producto`, c.nombre AS `Categoría`, "
                            + "e.cantidad AS `Existencia`, u.abreviatura AS `Unidad`, p.stock_minimo AS `Mínimo`, "
                            + "ROUND(e.cantidad * p.precio_costo, 2) AS `Valor costo (Q)`, "
                            + "CASE WHEN e.cantidad < p.stock_minimo THEN 'BAJO MÍNIMO' ELSE 'OK' END AS `Estado` "
                            + "FROM existencia e JOIN producto p ON p.id = e.id_producto JOIN categoria c ON c.id = p.id_categoria "
                            + "JOIN unidad_medida u ON u.id = p.id_unidad JOIN bodega b ON b.id = e.id_bodega "
                            + "WHERE (? IS NULL OR e.id_bodega = ?) ORDER BY b.nombre, p.codigo", idBodega, idBodega);

            case ROTACION -> dao.ejecutar(cn, titulo,
                    "SELECT p.codigo AS `Código`, p.nombre AS `Producto`, c.nombre AS `Categoría`, "
                            + "COALESCE(s.vendidas, 0) AS `Unidades vendidas`, COALESCE(x.stock, 0) AS `Existencia actual`, "
                            + "ROUND(COALESCE(s.vendidas, 0) / NULLIF((2 * COALESCE(x.stock, 0) + COALESCE(s.vendidas, 0)) / 2, 0), 2) AS `Índice rotación`, "
                            + "CASE WHEN COALESCE(s.vendidas, 0) = 0 THEN 'SIN VENTAS' "
                            + "     WHEN COALESCE(s.vendidas, 0) / NULLIF((2 * COALESCE(x.stock, 0) + COALESCE(s.vendidas, 0)) / 2, 0) >= 1 THEN 'ALTA' "
                            + "     WHEN COALESCE(s.vendidas, 0) / NULLIF((2 * COALESCE(x.stock, 0) + COALESCE(s.vendidas, 0)) / 2, 0) >= 0.2 THEN 'MEDIA' "
                            + "     ELSE 'BAJA' END AS `Rotación` "
                            + "FROM producto p JOIN categoria c ON c.id = p.id_categoria "
                            + "LEFT JOIN (SELECT id_producto, SUM(-cantidad) AS vendidas FROM movimiento_inventario "
                            + "           WHERE tipo = 'VENTA' AND fecha >= ? AND fecha < ? AND (? IS NULL OR id_bodega = ?) GROUP BY id_producto) s "
                            + "       ON s.id_producto = p.id "
                            + "LEFT JOIN (SELECT id_producto, SUM(cantidad) AS stock FROM existencia WHERE (? IS NULL OR id_bodega = ?) "
                            + "           GROUP BY id_producto) x ON x.id_producto = p.id "
                            + "ORDER BY `Índice rotación` DESC, p.codigo", ini, fin, idBodega, idBodega, idBodega, idBodega);

            case SIN_MOVIMIENTO -> dao.ejecutar(cn, tipo.getTitulo() + " (más de " + dias + " días)",
                    "SELECT p.codigo AS `Código`, p.nombre AS `Producto`, c.nombre AS `Categoría`, COALESCE(x.stock, 0) AS `Existencia`, "
                            + "ROUND(COALESCE(x.stock, 0) * p.precio_costo, 2) AS `Capital detenido (Q)`, "
                            + "DATE_FORMAT(m.ultimo, '%Y-%m-%d') AS `Último movimiento`, DATEDIFF(NOW(), m.ultimo) AS `Días sin movimiento` "
                            + "FROM producto p JOIN categoria c ON c.id = p.id_categoria "
                            + "LEFT JOIN (SELECT id_producto, MAX(fecha) AS ultimo FROM movimiento_inventario "
                            + "           WHERE (? IS NULL OR id_bodega = ?) GROUP BY id_producto) m ON m.id_producto = p.id "
                            + "LEFT JOIN (SELECT id_producto, SUM(cantidad) AS stock FROM existencia WHERE (? IS NULL OR id_bodega = ?) "
                            + "           GROUP BY id_producto) x ON x.id_producto = p.id "
                            + "WHERE p.activo = TRUE AND (m.ultimo IS NULL OR m.ultimo < NOW() - INTERVAL ? DAY) "
                            + "ORDER BY `Capital detenido (Q)` DESC", idBodega, idBodega, idBodega, idBodega, dias);

            case RENTABILIDAD_CATEGORIA -> dao.ejecutar(cn, titulo,
                    "SELECT c.nombre AS `Categoría`, COUNT(DISTINCT v.id) AS `Ventas`, SUM(d.cantidad_base) AS `Unidades`, "
                            + "ROUND(SUM(d.cantidad * d.precio_unitario), 2) AS `Ingresos (Q)`, "
                            + "ROUND(SUM(d.cantidad_base * d.costo_unitario), 2) AS `Costo (Q)`, "
                            + "ROUND(SUM(d.cantidad * d.precio_unitario) - SUM(d.cantidad_base * d.costo_unitario), 2) AS `Utilidad (Q)`, "
                            + "ROUND(100 * (SUM(d.cantidad * d.precio_unitario) - SUM(d.cantidad_base * d.costo_unitario)) "
                            + "      / NULLIF(SUM(d.cantidad * d.precio_unitario), 0), 2) AS `Margen %` "
                            + "FROM detalle_venta d JOIN venta v ON v.id = d.id_venta JOIN producto p ON p.id = d.id_producto "
                            + "JOIN categoria c ON c.id = p.id_categoria "
                            + "WHERE v.estado = 'CONFIRMADA' AND v.fecha >= ? AND v.fecha < ? "
                            + "GROUP BY c.id, c.nombre ORDER BY `Utilidad (Q)` DESC", ini, fin);

            case TRANSFERENCIAS_PENDIENTES -> dao.ejecutar(cn, titulo,
                    "SELECT t.id AS `No.`, DATE_FORMAT(t.fecha_envio, '%Y-%m-%d %H:%i') AS `Enviada`, bo.nombre AS `Origen`, "
                            + "bd.nombre AS `Destino`, u.nombre AS `Enviada por`, COUNT(d.id) AS `Productos`, SUM(d.cantidad) AS `Unidades`, "
                            + "DATEDIFF(NOW(), t.fecha_envio) AS `Días en tránsito` "
                            + "FROM transferencia t JOIN bodega bo ON bo.id = t.id_bodega_origen JOIN bodega bd ON bd.id = t.id_bodega_destino "
                            + "JOIN usuario u ON u.id = t.id_usuario_envia JOIN detalle_transferencia d ON d.id_transferencia = t.id "
                            + "WHERE t.estado = 'EN_TRANSITO' AND (? IS NULL OR t.id_bodega_destino = ? OR t.id_bodega_origen = ?) "
                            + "GROUP BY t.id, t.fecha_envio, bo.nombre, bd.nombre, u.nombre ORDER BY t.fecha_envio", idBodega, idBodega, idBodega);

            case STOCK_MINIMO -> dao.ejecutar(cn, titulo,
                    "SELECT b.nombre AS `Bodega`, p.codigo AS `Código`, p.nombre AS `Producto`, e.cantidad AS `Existencia`, "
                            + "p.stock_minimo AS `Mínimo`, p.stock_minimo - e.cantidad AS `Faltante` "
                            + "FROM existencia e JOIN producto p ON p.id = e.id_producto JOIN bodega b ON b.id = e.id_bodega "
                            + "WHERE e.cantidad < p.stock_minimo AND (? IS NULL OR e.id_bodega = ?) ORDER BY `Faltante` DESC", idBodega, idBodega);

            case VENTAS_PERIODO -> dao.ejecutar(cn, titulo,
                    "SELECT v.id AS `No.`, DATE_FORMAT(v.fecha, '%Y-%m-%d %H:%i') AS `Fecha`, c.nombre AS `Cliente`, "
                            + "u.nombre AS `Vendedor`, b.nombre AS `Bodega`, v.total AS `Total (Q)` "
                            + "FROM venta v JOIN cliente c ON c.id = v.id_cliente JOIN usuario u ON u.id = v.id_usuario "
                            + "JOIN bodega b ON b.id = v.id_bodega "
                            + "WHERE v.estado = 'CONFIRMADA' AND v.fecha >= ? AND v.fecha < ? AND (? IS NULL OR v.id_bodega = ?) "
                            + "ORDER BY v.fecha", ini, fin, idBodega, idBodega);

            case COMPRAS_PROVEEDOR -> dao.ejecutar(cn, titulo,
                    "SELECT p.nombre AS `Proveedor`, p.nit AS `NIT`, COUNT(c.id) AS `Compras`, "
                            + "SUM(CASE WHEN c.estado = 'RECIBIDA' THEN c.total ELSE 0 END) AS `Recibido (Q)`, "
                            + "SUM(CASE WHEN c.estado = 'PENDIENTE' THEN c.total ELSE 0 END) AS `Pendiente (Q)` "
                            + "FROM compra c JOIN proveedor p ON p.id = c.id_proveedor "
                            + "WHERE c.estado <> 'ANULADA' AND c.fecha >= ? AND c.fecha < ? "
                            + "GROUP BY p.id, p.nombre, p.nit ORDER BY `Recibido (Q)` DESC", ini, fin);

            case DEVOLUCIONES -> dao.ejecutar(cn, titulo,
                    "SELECT DATE_FORMAT(d.fecha, '%Y-%m-%d %H:%i') AS `Fecha`, d.id_venta AS `Venta`, p.nombre AS `Producto`, "
                            + "d.cantidad_base AS `Cantidad`, d.motivo AS `Motivo`, u.nombre AS `Registró` "
                            + "FROM devolucion d JOIN producto p ON p.id = d.id_producto JOIN usuario u ON u.id = d.id_usuario "
                            + "WHERE d.fecha >= ? AND d.fecha < ? ORDER BY d.motivo, d.fecha", ini, fin);

            case AJUSTES -> dao.ejecutar(cn, titulo,
                    "SELECT DATE_FORMAT(a.fecha, '%Y-%m-%d %H:%i') AS `Fecha`, b.nombre AS `Bodega`, p.nombre AS `Producto`, "
                            + "a.cantidad AS `Cantidad`, a.motivo AS `Motivo`, us.nombre AS `Solicitó`, ua.nombre AS `Autorizó` "
                            + "FROM ajuste_inventario a JOIN producto p ON p.id = a.id_producto JOIN bodega b ON b.id = a.id_bodega "
                            + "JOIN usuario us ON us.id = a.id_usuario_solicita JOIN usuario ua ON ua.id = a.id_usuario_autoriza "
                            + "WHERE a.fecha >= ? AND a.fecha < ? AND (? IS NULL OR a.id_bodega = ?) ORDER BY a.fecha DESC",
                    ini, fin, idBodega, idBodega);

            case AUDITORIA -> dao.ejecutar(cn, titulo,
                    "SELECT DATE_FORMAT(a.fecha, '%Y-%m-%d %H:%i:%s') AS `Fecha`, COALESCE(u.username, '-') AS `Usuario`, "
                            + "a.accion AS `Acción`, a.entidad AS `Entidad`, a.detalle AS `Detalle` "
                            + "FROM auditoria a LEFT JOIN usuario u ON u.id = a.id_usuario "
                            + "WHERE a.fecha >= ? AND a.fecha < ? ORDER BY a.id DESC LIMIT 1000", ini, fin);
        });
    }
}
