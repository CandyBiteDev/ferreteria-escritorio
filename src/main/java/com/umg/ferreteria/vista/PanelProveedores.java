package com.umg.ferreteria.vista;

import com.umg.ferreteria.dao.Dao;
import com.umg.ferreteria.modelo.Proveedor;
import com.umg.ferreteria.modelo.Usuario;

import javax.swing.*;

public class PanelProveedores extends PanelCrud<Proveedor> {

    private final JTextField txtNombre = new JTextField();
    private final JTextField txtNit = new JTextField();
    private final JTextField txtContacto = new JTextField();
    private final JTextField txtTelefono = new JTextField();
    private final JTextField txtCorreo = new JTextField();
    private final JTextField txtDireccion = new JTextField();

    public PanelProveedores(AppEscritorio app, Usuario u) {
        super(app, u, "Proveedores", "Empresas a las que se compra mercadería", true,
                "ID", "Nombre", "NIT", "Contacto", "Teléfono", "Correo");
        inicializar();
    }

    @Override
    protected Dao<Proveedor> dao() { return app.catalogo().proveedores(); }

    @Override
    protected Object[] fila(Proveedor e) {
        return new Object[]{e.getId(), e.getNombre(), e.getNit(), e.getContacto(), e.getTelefono(), e.getCorreo()};
    }

    @Override
    protected void construirFormulario(Ui.Formulario f) {
        f.campo("Nombre", txtNombre).campo("NIT", txtNit).campo("Contacto", txtContacto)
                .campo("Teléfono", txtTelefono).campo("Correo", txtCorreo).campo("Dirección", txtDireccion);
    }

    @Override
    protected void llenarFormulario(Proveedor e) {
        txtNombre.setText(e == null ? "" : e.getNombre());
        txtNit.setText(e == null ? "" : e.getNit());
        txtContacto.setText(e == null ? "" : e.getContacto());
        txtTelefono.setText(e == null ? "" : e.getTelefono());
        txtCorreo.setText(e == null ? "" : e.getCorreo());
        txtDireccion.setText(e == null ? "" : e.getDireccion());
    }

    @Override
    protected Proveedor leerFormulario() {
        Proveedor p = new Proveedor();
        p.setNombre(txtNombre.getText().trim());
        p.setNit(txtNit.getText());
        p.setContacto(txtContacto.getText().trim());
        p.setTelefono(txtTelefono.getText().trim());
        p.setCorreo(txtCorreo.getText().trim());
        p.setDireccion(txtDireccion.getText().trim());
        return p;
    }
}
