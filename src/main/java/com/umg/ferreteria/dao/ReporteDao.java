package com.umg.ferreteria.dao;

import com.umg.ferreteria.modelo.ResultadoReporte;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Ejecuta cualquier consulta de reporte y arma columnas/filas a partir de los metadatos. */
@Repository
public class ReporteDao {

    public ResultadoReporte ejecutar(Connection cn, String titulo, String sql, Object... params) throws SQLException {
        ResultadoReporte r = new ResultadoReporte(titulo);
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            Jdbc.parametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                for (int i = 1; i <= n; i++) r.getColumnas().add(md.getColumnLabel(i));
                while (rs.next()) {
                    List<Object> fila = new ArrayList<>(n);
                    for (int i = 1; i <= n; i++) {
                        Object v = rs.getObject(i);
                        if (v instanceof BigDecimal bd) {
                            v = bd.stripTrailingZeros().scale() < 0 ? bd.setScale(0) : bd.stripTrailingZeros();
                        }
                        fila.add(v);
                    }
                    r.getFilas().add(fila);
                }
            }
        }
        return r;
    }
}
