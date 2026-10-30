package com.umg.ferreteria.dao;

import com.umg.ferreteria.excepcion.PersistenciaException;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Punto único de acceso a conexiones JDBC.
 * - consultar(): una conexión en autocommit para lecturas u operaciones simples.
 * - transaccion(): setAutoCommit(false) + commit, o ROLLBACK si algo falla
 *   (regla de negocio o error SQL). Así un documento con sus detalles y el
 *   movimiento de inventario se guardan completos o no se guarda nada.
 */
@Component
public class ConexionBD {

    @FunctionalInterface
    public interface Trabajo<T> {
        T ejecutar(Connection cn) throws SQLException;
    }

    private final DataSource dataSource;

    public ConexionBD(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> T consultar(Trabajo<T> trabajo) {
        try (Connection cn = dataSource.getConnection()) {
            return trabajo.ejecutar(cn);
        } catch (SQLException e) {
            throw new PersistenciaException(e);
        }
    }

    public <T> T transaccion(Trabajo<T> trabajo) {
        try (Connection cn = dataSource.getConnection()) {
            cn.setAutoCommit(false);
            try {
                T resultado = trabajo.ejecutar(cn);
                cn.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            } finally {
                cn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new PersistenciaException(e);
        }
    }
}
