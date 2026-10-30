package com.umg.ferreteria.modelo;

import java.util.ArrayList;
import java.util.List;

/** Resultado tabular genérico de cualquier reporte (columnas + filas). */
public class ResultadoReporte {

    private final String titulo;
    private final List<String> columnas = new ArrayList<>();
    private final List<List<Object>> filas = new ArrayList<>();

    public ResultadoReporte(String titulo) {
        this.titulo = titulo;
    }

    public String getTitulo() { return titulo; }
    public List<String> getColumnas() { return columnas; }
    public List<List<Object>> getFilas() { return filas; }
}
