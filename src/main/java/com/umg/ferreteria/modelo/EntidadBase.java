package com.umg.ferreteria.modelo;

import com.umg.ferreteria.excepcion.ValidacionException;

import java.math.BigDecimal;

/**
 * Clase abstracta raíz del modelo. Toda entidad persistente tiene un id
 * y sabe validarse a sí misma (cada subclase sobrescribe validar()).
 */
public abstract class EntidadBase {

    protected Integer id;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public boolean esNuevo() {
        return id == null || id == 0;
    }

    /** Regla de validación propia de cada entidad. Lanza ValidacionException. */
    public abstract void validar();

    protected static void requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacionException("El campo '" + campo + "' es obligatorio.");
        }
    }

    protected static void requerido(Integer valor, String campo) {
        if (valor == null || valor <= 0) {
            throw new ValidacionException("Debe seleccionar " + campo + ".");
        }
    }

    protected static void noNegativo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() < 0) {
            throw new ValidacionException("El campo '" + campo + "' debe ser mayor o igual a cero.");
        }
    }

    protected static void positivo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() <= 0) {
            throw new ValidacionException("El campo '" + campo + "' debe ser mayor que cero.");
        }
    }

    protected static void maximo(String valor, int largo, String campo) {
        if (valor != null && valor.length() > largo) {
            throw new ValidacionException("El campo '" + campo + "' admite máximo " + largo + " caracteres.");
        }
    }
}
