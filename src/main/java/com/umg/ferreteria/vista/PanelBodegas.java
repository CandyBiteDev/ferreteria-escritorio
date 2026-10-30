package com.umg.ferreteria.vista;

import com.umg.ferreteria.dao.Dao;
import com.umg.ferreteria.modelo.Bodega;
import com.umg.ferreteria.modelo.Sucursal;
import com.umg.ferreteria.modelo.Usuario;

import javax.swing.*;

public class PanelBodegas extends PanelCrud<Bodega> {

    private final JTextField txtNombre = new JTextField();
    private final JTextField txtUbicacion = new JTextField();
    private final JComboBox<Sucursal> cbSucursal = new JComboBox<>();
    private final JCheckBox chkActivo = new JCheckBox("Activa", true);

    public PanelBodegas(AppEscritorio app, Usuario u) {
        super(app, u, "Bodegas", "Cada sucursal puede tener varias bodegas", true, "ID", "Bodega", "Sucursal", "Ubicación", "Activa");
        inicializar();
    }

    @Override
    protected Dao<Bodega> dao() { return app.catalogo().bodegas(); }

    @Override
    public void refrescar() {
        Ui.recargar(cbSucursal, app.catalogo().listar(app.catalogo().sucursales()));
        super.refrescar();
    }

    @Override
    protected Object[] fila(Bodega e) {
        return new Object[]{e.getId(), e.getNombre(), e.getSucursal(), e.getUbicacion(), e.isActivo() ? "Sí" : "No"};
    }

    @Override
    protected void construirFormulario(Ui.Formulario f) {
        f.campo("Nombre", txtNombre).campo("Sucursal", cbSucursal).campo("Ubicación", txtUbicacion).campo("", chkActivo);
    }

    @Override
    protected void llenarFormulario(Bodega e) {
        txtNombre.setText(e == null ? "" : e.getNombre());
        txtUbicacion.setText(e == null ? "" : e.getUbicacion());
        Ui.seleccionar(cbSucursal, e == null ? null : e.getIdSucursal());
        chkActivo.setSelected(e == null || e.isActivo());
    }

    @Override
    protected Bodega leerFormulario() {
        Bodega b = new Bodega();
        b.setNombre(txtNombre.getText().trim());
        b.setUbicacion(txtUbicacion.getText().trim());
        b.setIdSucursal(Ui.idDe(cbSucursal));
        b.setActivo(chkActivo.isSelected());
        return b;
    }
}
