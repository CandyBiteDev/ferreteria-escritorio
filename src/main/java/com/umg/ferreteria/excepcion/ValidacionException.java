package com.umg.ferreteria.excepcion;

/** Datos de entrada incompletos o con formato incorrecto. */
public class ValidacionException extends FerreteriaException {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }

    @Override
    public int getCodigoHttp() {
        return 400;
    }
}
