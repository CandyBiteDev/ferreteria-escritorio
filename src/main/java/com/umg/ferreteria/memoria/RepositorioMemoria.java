package com.umg.ferreteria.memoria;

import com.umg.ferreteria.excepcion.ValidacionException;
import com.umg.ferreteria.modelo.EntidadBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * ENTREGA 2: almacenamiento con listas de objetos (ArrayList), sin base de datos.
 * Genérico: el mismo código guarda productos, clientes, ventas, etc.
 * Asigna ids autoincrementales como lo haría MySQL.
 */
public class RepositorioMemoria<T extends EntidadBase> {

    private final List<T> datos = new ArrayList<>();
    private int siguienteId = 1;

    /** Copia de la lista: quien la recibe no puede alterar el repositorio (encapsulamiento). */
    public List<T> listar() {
        return new ArrayList<>(datos);
    }

    public List<T> buscar(Predicate<T> filtro) {
        List<T> resultado = new ArrayList<>();
        for (T e : datos) {
            if (filtro.test(e)) resultado.add(e);
        }
        return resultado;
    }

    public Optional<T> porId(int id) {
        for (T e : datos) {
            if (e.getId() == id) return Optional.of(e);
        }
        return Optional.empty();
    }

    /** Alta si no tiene id, cambio si ya lo tiene. Siempre valida antes (polimorfismo de validar()). */
    public T guardar(T entidad) {
        entidad.validar();
        if (entidad.esNuevo()) {
            entidad.setId(siguienteId++);
            datos.add(entidad);
        } else {
            int i = indice(entidad.getId());
            if (i < 0) throw new ValidacionException("No existe el registro #" + entidad.getId());
            datos.set(i, entidad);
        }
        return entidad;
    }

    public void eliminar(int id) {
        int i = indice(id);
        if (i < 0) throw new ValidacionException("No existe el registro #" + id);
        datos.remove(i);
    }

    public int contar() {
        return datos.size();
    }

    private int indice(int id) {
        for (int i = 0; i < datos.size(); i++) {
            if (datos.get(i).getId() == id) return i;
        }
        return -1;
    }
}
