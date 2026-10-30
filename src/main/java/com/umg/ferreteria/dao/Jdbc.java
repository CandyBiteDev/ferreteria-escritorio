package com.umg.ferreteria.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Utilidades JDBC: todas las consultas usan PreparedStatement (evita inyección SQL). */
public final class Jdbc {

    @FunctionalInterface
    public interface Mapeador<T> {
        T mapear(ResultSet rs) throws SQLException;
    }

    private Jdbc() {
    }

    public static void parametros(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p instanceof LocalDateTime ldt) {
                ps.setTimestamp(i + 1, Timestamp.valueOf(ldt));
            } else {
                ps.setObject(i + 1, p);
            }
        }
    }

    public static <T> List<T> lista(Connection cn, String sql, Mapeador<T> m, Object... params) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            parametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> r = new ArrayList<>();
                while (rs.next()) r.add(m.mapear(rs));
                return r;
            }
        }
    }

    public static int ejecutar(Connection cn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            parametros(ps, params);
            return ps.executeUpdate();
        }
    }

    /** INSERT que devuelve la llave autogenerada. */
    public static int insertar(Connection cn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            parametros(ps, params);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public static BigDecimal escalar(Connection cn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            parametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                BigDecimal v = rs.getBigDecimal(1);
                return v;
            }
        }
    }

    public static Integer entero(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    public static LocalDateTime fecha(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }

    public static BigDecimal decimal(ResultSet rs, String col) throws SQLException {
        BigDecimal v = rs.getBigDecimal(col);
        return v == null ? BigDecimal.ZERO : v.stripTrailingZeros().scale() < 0 ? v.setScale(0) : v.stripTrailingZeros();
    }
}
