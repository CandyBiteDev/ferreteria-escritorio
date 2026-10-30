package com.umg.ferreteria.excepcion;

import java.sql.SQLException;

/** Falla controlada de la base de datos (envuelve SQLException con un mensaje entendible). */
public class PersistenciaException extends FerreteriaException {

    public PersistenciaException(SQLException causa) {
        super(traducir(causa), causa);
    }

    @Override
    public int getCodigoHttp() {
        return 500;
    }

    private static String traducir(SQLException e) {
        return switch (e.getErrorCode()) {
            case 1062 -> "Ya existe un registro con ese valor (código, NIT o nombre duplicado).";
            case 1451 -> "No se puede eliminar: el registro está siendo usado por otros datos.";
            case 1452 -> "Referencia inválida: el registro relacionado no existe.";
            case 3819 -> "La base de datos rechazó la operación por una restricción (ej. stock negativo).";
            case 1146 -> "Falta una tabla. Ejecuta el script sql/db_ferreteria.sql.";
            case 1049 -> "No existe la base db_ferreteria. Ejecuta el script sql/db_ferreteria.sql.";
            default -> "Error de base de datos: " + e.getMessage();
        };
    }
}
