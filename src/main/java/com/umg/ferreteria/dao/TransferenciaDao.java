package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.DetalleTransferencia;
import com.umg.ferreteria.modelo.Transferencia;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class TransferenciaDao extends DaoBase<Transferencia> {

    @Override
    protected String tabla() { return "transferencia"; }

    @Override
    protected String consultaBase() {
        return "SELECT t.*, bo.nombre AS bodega_origen, bd.nombre AS bodega_destino, ue.nombre AS usuario, ur.nombre AS usuario_recibe "
                + "FROM transferencia t "
                + "JOIN bodega bo ON bo.id = t.id_bodega_origen "
                + "JOIN bodega bd ON bd.id = t.id_bodega_destino "
                + "JOIN usuario ue ON ue.id = t.id_usuario_envia "
                + "LEFT JOIN usuario ur ON ur.id = t.id_usuario_recibe";
    }

    @Override
    protected String orden() { return "t.id DESC"; }

    /** Filtros opcionales por bodega destino y estado; incluye detalles (lo usa la app móvil). */
    public List<Transferencia> buscar(Connection cn, Integer idBodegaDestino, String estado) throws SQLException {
        StringBuilder sql = new StringBuilder("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (idBodegaDestino != null) {
            sql.append("AND t.id_bodega_destino = ? ");
            params.add(idBodegaDestino);
        }
        if (estado != null && !estado.isBlank()) {
            sql.append("AND t.estado = ? ");
            params.add(estado);
        }
        sql.append("ORDER BY t.id DESC");
        List<Transferencia> lista = consultar(cn, sql.toString(), params.toArray());
        for (Transferencia t : lista) {
            for (DetalleTransferencia d : detalles(cn, t.getId())) t.agregarDetalle(d);
        }
        return lista;
    }

    @Override
    public Optional<Transferencia> buscarPorId(Connection cn, int id) throws SQLException {
        Optional<Transferencia> t = super.buscarPorId(cn, id);
        if (t.isPresent()) {
            for (DetalleTransferencia d : detalles(cn, id)) t.get().agregarDetalle(d);
        }
        return t;
    }

    public List<DetalleTransferencia> detalles(Connection cn, int idTransferencia) throws SQLException {
        return Jdbc.lista(cn, "SELECT d.*, p.codigo, p.nombre AS producto, u.abreviatura AS unidad FROM detalle_transferencia d "
                + "JOIN producto p ON p.id = d.id_producto JOIN unidad_medida u ON u.id = p.id_unidad "
                + "WHERE d.id_transferencia = ? ORDER BY d.id", rs -> {
            DetalleTransferencia d = new DetalleTransferencia();
            d.setId(rs.getInt("id"));
            d.setIdProducto(rs.getInt("id_producto"));
            d.setCodigo(rs.getString("codigo"));
            d.setProducto(rs.getString("producto"));
            d.setUnidad(rs.getString("unidad"));
            d.setCantidad(Jdbc.decimal(rs, "cantidad"));
            return d;
        }, idTransferencia);
    }

    @Override
    protected Transferencia mapear(ResultSet rs) throws SQLException {
        Transferencia t = new Transferencia();
        t.setId(rs.getInt("id"));
        t.setFecha(Jdbc.fecha(rs, "fecha_envio"));
        t.setFechaRecepcion(Jdbc.fecha(rs, "fecha_recepcion"));
        t.setIdBodegaOrigen(rs.getInt("id_bodega_origen"));
        t.setBodegaOrigen(rs.getString("bodega_origen"));
        t.setIdBodegaDestino(rs.getInt("id_bodega_destino"));
        t.setBodegaDestino(rs.getString("bodega_destino"));
        t.setIdUsuario(rs.getInt("id_usuario_envia"));
        t.setUsuario(rs.getString("usuario"));
        t.setIdUsuarioRecibe(Jdbc.entero(rs, "id_usuario_recibe"));
        t.setUsuarioRecibe(rs.getString("usuario_recibe"));
        t.setEstado(rs.getString("estado"));
        t.setObservacion(rs.getString("observacion"));
        return t;
    }

    @Override
    public int insertar(Connection cn, Transferencia t) throws SQLException {
        int id = Jdbc.insertar(cn, "INSERT INTO transferencia (id_bodega_origen, id_bodega_destino, id_usuario_envia, estado, observacion) "
                + "VALUES (?, ?, ?, ?, ?)", t.getIdBodegaOrigen(), t.getIdBodegaDestino(), t.getIdUsuario(), t.getEstado(), t.getObservacion());
        for (DetalleTransferencia d : t.getDetalles()) {
            Jdbc.ejecutar(cn, "INSERT INTO detalle_transferencia (id_transferencia, id_producto, cantidad) VALUES (?, ?, ?)",
                    id, d.getIdProducto(), d.getCantidad());
        }
        return id;
    }

    @Override
    public void actualizar(Connection cn, Transferencia t) throws SQLException {
        Jdbc.ejecutar(cn, "UPDATE transferencia SET estado = ?, id_usuario_recibe = ?, fecha_recepcion = ?, observacion = ? WHERE id = ?",
                t.getEstado(), t.getIdUsuarioRecibe(), t.getFechaRecepcion(), t.getObservacion(), t.getId());
    }
}
