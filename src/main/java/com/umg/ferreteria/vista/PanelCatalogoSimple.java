package com.umg.ferreteria.vista;

import com.umg.ferreteria.dao.Dao;
import com.umg.ferreteria.modelo.CatalogoSimple;
import com.umg.ferreteria.modelo.Categoria;
import com.umg.ferreteria.modelo.Marca;
import com.umg.ferreteria.modelo.Usuario;

import javax.swing.*;
import java.util.function.Supplier;

/** Un solo panel reutilizado para Categorías y Marcas (genéricos). */
public class PanelCatalogoSimple<T extends CatalogoSimple> extends PanelCrud<T> {

    private final Dao<T> dao;
    private final Supplier<T> fabrica;
    private final JTextField txtNombre = new JTextField();

    private PanelCatalogoSimple(AppEscritorio app, Usuario u, String titulo, Dao<T> dao, Supplier<T> fabrica) {
        super(app, u, titulo, "Catálogo utilizado por los productos", true, "ID", "Nombre");
        this.dao = dao;
        this.fabrica = fabrica;
        inicializar();
    }

    public static PanelCatalogoSimple<Categoria> categorias(AppEscritorio app, Usuario u) {
        return new PanelCatalogoSimple<>(app, u, "Categorías", app.catalogo().categorias(), Categoria::new);
    }

    public static PanelCatalogoSimple<Marca> marcas(AppEscritorio app, Usuario u) {
        return new PanelCatalogoSimple<>(app, u, "Marcas", app.catalogo().marcas(), Marca::new);
    }

    @Override
    protected Dao<T> dao() { return dao; }

    @Override
    protected Object[] fila(T e) { return new Object[]{e.getId(), e.getNombre()}; }

    @Override
    protected void construirFormulario(Ui.Formulario f) {
        f.campo("Nombre", txtNombre);
    }

    @Override
    protected void llenarFormulario(T e) {
        txtNombre.setText(e == null ? "" : e.getNombre());
    }

    @Override
    protected T leerFormulario() {
        T e = fabrica.get();
        e.setNombre(txtNombre.getText());
        return e;
    }
}
