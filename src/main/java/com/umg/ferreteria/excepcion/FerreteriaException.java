package com.umg.ferreteria.excepcion;

/**
 * Raíz de las excepciones propias del sistema.
 * Cada subclase indica el código HTTP con el que la API la reporta (polimorfismo).
 */
public abstract class FerreteriaException extends RuntimeException {

    protected FerreteriaException(String mensaje) {
        super(mensaje);
    }

    protected FerreteriaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public abstract int getCodigoHttp();
}
