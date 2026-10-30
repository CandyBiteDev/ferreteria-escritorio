package com.umg.ferreteria;

/**
 * Las pruebas usan su propia base (db_ferreteria_test), que se recrea desde
 * sql/db_ferreteria.sql al iniciar. La base real db_ferreteria no se toca.
 */
final class BaseDePrueba {

    static final String URL = "spring.datasource.url=jdbc:mysql://localhost:3306/db_ferreteria_test"
            + "?createDatabaseIfNotExist=true&serverTimezone=America/Guatemala&characterEncoding=UTF-8";

    static final String REINICIAR = "--reiniciar-bd";

    private BaseDePrueba() {
    }
}
