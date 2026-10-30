package com.umg.ferreteria.vista;

import com.umg.ferreteria.dao.Dao;
import com.umg.ferreteria.modelo.UnidadMedida;
import com.umg.ferreteria.modelo.Usuario;

import javax.swing.*;

public class PanelUnidades extends PanelCrud<UnidadMedida> {

    private final JTextField txtNombre = new JTextField();
    private final JTextField txtAbrev = new JTextField();

    public PanelUnidades(AppEscritorio app, Usuario u) {
        super(app, u, "Unidades de medida", "Las conversiones por producto se definen en Productos", true, "ID", "Nombre", "Abreviatura");
        inicializar();
    }

    @Override
    protected Dao<UnidadMedida> dao() { return app.catalogo().unidades(); }

    @Override
    protected Object[] fila(UnidadMedida e) { return new Object[]{e.getId(), e.getNombre(), e.getAbreviatura()}; }

    @Override
    protected void construirFormulario(Ui.Formulario f) {
        f.campo("Nombre", txtNombre).campo("Abreviatura", txtAbrev);
    }

    @Override
    protected void llenarFormulario(UnidadMedida e) {
        txtNombre.setText(e == null ? "" : e.getNombre());
        txtAbrev.setText(e == null ? "" : e.getAbreviatura());
    }

    @Override
    protected UnidadMedida leerFormulario() {
        UnidadMedida u = new UnidadMedida();
        u.setNombre(txtNombre.getText());
        u.setAbreviatura(txtAbrev.getText().trim());
        return u;
    }
}
