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
            case 1045 -> "MySQL rechazó el usuario o la contraseña. Crea el archivo application-local.properties "
                    + "junto al pom.xml (copia de src/main/resources/application-local.properties.example) "
                    + "y escribe ahí la contraseña de tu MySQL. Luego vuelve a ejecutar.";
            case 0 -> e.getMessage() != null && e.getMessage().contains("Communications link failure")
                    ? "No se pudo conectar a MySQL. Verifica que el servicio MySQL esté encendido (puerto 3306)."
                    : "Error de base de datos: " + e.getMessage();
            case 1062 -> "Ya existe un registro con ese valor (código, NIT o nombre duplicado).";
            case 1451 -> "No se puede eliminar: el registro está siendo usado por otros datos.";
            case 1452 -> "Referencia inválida: el registro relacionado no existe.";
            case 3819 -> "La base de datos rechazó la operación por una restricción (ej. stock negativo).";
            case 1146 -> "Faltan tablas en la base de datos. Cierra y vuelve a abrir la aplicación para que las cree; "
                    + "si persiste, ejecuta sql/db_ferreteria.sql en MySQL Workbench (File > Open SQL Script y el rayo).";
            case 1049 -> "No existe la base db_ferreteria. Ejecuta el script sql/db_ferreteria.sql.";
            default -> "Error de base de datos: " + e.getMessage();
        };
    }
}
