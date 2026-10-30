package com.umg.ferreteria.vista;

import com.umg.ferreteria.dao.Dao;
import com.umg.ferreteria.modelo.Sucursal;
import com.umg.ferreteria.modelo.Usuario;

import javax.swing.*;

public class PanelSucursales extends PanelCrud<Sucursal> {

    private final JTextField txtNombre = new JTextField();
    private final JTextField txtDireccion = new JTextField();
    private final JTextField txtTelefono = new JTextField();
    private final JCheckBox chkActivo = new JCheckBox("Activa", true);

    public PanelSucursales(AppEscritorio app, Usuario u) {
        super(app, u, "Sucursales", "Puntos de venta de la ferretería", true, "ID", "Nombre", "Dirección", "Teléfono", "Activa");
        inicializar();
    }

    @Override
    protected Dao<Sucursal> dao() { return app.catalogo().sucursales(); }

    @Override
    protected Object[] fila(Sucursal e) {
        return new Object[]{e.getId(), e.getNombre(), e.getDireccion(), e.getTelefono(), e.isActivo() ? "Sí" : "No"};
    }

    @Override
    protected void construirFormulario(Ui.Formulario f) {
        f.campo("Nombre", txtNombre).campo("Dirección", txtDireccion).campo("Teléfono", txtTelefono).campo("", chkActivo);
    }

    @Override
    protected void llenarFormulario(Sucursal e) {
        txtNombre.setText(e == null ? "" : e.getNombre());
        txtDireccion.setText(e == null ? "" : e.getDireccion());
        txtTelefono.setText(e == null ? "" : e.getTelefono());
        chkActivo.setSelected(e == null || e.isActivo());
    }

    @Override
    protected Sucursal leerFormulario() {
        Sucursal s = new Sucursal();
        s.setNombre(txtNombre.getText().trim());
        s.setDireccion(txtDireccion.getText().trim());
        s.setTelefono(txtTelefono.getText().trim());
        s.setActivo(chkActivo.isSelected());
        return s;
    }
}
