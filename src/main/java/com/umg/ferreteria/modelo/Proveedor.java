package com.umg.ferreteria.modelo;

import com.umg.ferreteria.excepcion.ValidacionException;

public class Proveedor extends Persona {

    private String contacto;

    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }

    @Override
    public String getTipoPersona() {
        return "Proveedor";
    }

    @Override
    public void validar() {
        super.validar();
        if ("CF".equals(nit)) {
            throw new ValidacionException("Un proveedor debe tener NIT (no CF).");
        }
    }
}
