package com.umg.ferreteria.vista;

import com.umg.ferreteria.dao.Dao;
import com.umg.ferreteria.modelo.Cliente;
import com.umg.ferreteria.modelo.Usuario;

import javax.swing.*;

public class PanelClientes extends PanelCrud<Cliente> {

    private final JTextField txtNombre = new JTextField();
    private final JTextField txtNit = new JTextField();
    private final JTextField txtTelefono = new JTextField();
    private final JTextField txtCorreo = new JTextField();
    private final JTextField txtDireccion = new JTextField();

    public PanelClientes(AppEscritorio app, Usuario u) {
        super(app, u, "Clientes", "NIT con formato 1234567-8 o CF para consumidor final", true,
                "ID", "Nombre", "NIT", "Teléfono", "Correo", "Dirección");
        inicializar();
    }

    @Override
    protected Dao<Cliente> dao() { return app.catalogo().clientes(); }

    @Override
    protected Object[] fila(Cliente e) {
        return new Object[]{e.getId(), e.getNombre(), e.getNit(), e.getTelefono(), e.getCorreo(), e.getDireccion()};
    }

    @Override
    protected void construirFormulario(Ui.Formulario f) {
        f.campo("Nombre", txtNombre).campo("NIT", txtNit).campo("Teléfono", txtTelefono).campo("Correo", txtCorreo).campo("Dirección", txtDireccion);
    }

    @Override
    protected void llenarFormulario(Cliente e) {
        txtNombre.setText(e == null ? "" : e.getNombre());
        txtNit.setText(e == null ? "" : e.getNit());
        txtTelefono.setText(e == null ? "" : e.getTelefono());
        txtCorreo.setText(e == null ? "" : e.getCorreo());
        txtDireccion.setText(e == null ? "" : e.getDireccion());
    }

    @Override
    protected Cliente leerFormulario() {
        Cliente c = new Cliente();
        c.setNombre(txtNombre.getText().trim());
        c.setNit(txtNit.getText());
        c.setTelefono(txtTelefono.getText().trim());
        c.setCorreo(txtCorreo.getText().trim());
        c.setDireccion(txtDireccion.getText().trim());
        return c;
    }
}
