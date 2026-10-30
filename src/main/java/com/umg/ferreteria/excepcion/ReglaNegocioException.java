package com.umg.ferreteria.excepcion;

/** Se lanza cuando una operación viola una regla de negocio del enunciado. */
public class ReglaNegocioException extends FerreteriaException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }

    @Override
    public int getCodigoHttp() {
        return 409;
    }
}
