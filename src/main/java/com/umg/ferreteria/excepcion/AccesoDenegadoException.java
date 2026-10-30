package com.umg.ferreteria.excepcion;

/** El usuario está autenticado pero su rol no le permite la operación. */
public class AccesoDenegadoException extends FerreteriaException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }

    @Override
    public int getCodigoHttp() {
        return 403;
    }
}
