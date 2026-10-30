package com.umg.ferreteria.excepcion;

/** Usuario/contraseña incorrectos o token inválido. */
public class AutenticacionException extends FerreteriaException {

    public AutenticacionException(String mensaje) {
        super(mensaje);
    }

    @Override
    public int getCodigoHttp() {
        return 401;
    }
}
