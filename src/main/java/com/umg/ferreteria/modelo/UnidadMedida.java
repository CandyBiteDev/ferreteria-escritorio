package com.umg.ferreteria.modelo;

public class UnidadMedida extends CatalogoSimple {

    private String abreviatura;

    public String getAbreviatura() { return abreviatura; }
    public void setAbreviatura(String abreviatura) { this.abreviatura = abreviatura; }

    @Override
    public void validar() {
        super.validar();
        requerido(abreviatura, "Abreviatura");
        maximo(abreviatura, 10, "Abreviatura");
    }
}
