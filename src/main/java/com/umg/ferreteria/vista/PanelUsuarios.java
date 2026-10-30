package com.umg.ferreteria.vista;

import com.umg.ferreteria.dao.Dao;
import com.umg.ferreteria.modelo.Bodega;
import com.umg.ferreteria.modelo.Rol;
import com.umg.ferreteria.modelo.Usuario;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class PanelUsuarios extends PanelCrud<Usuario> {

    private final JTextField txtUsername = new JTextField();
    private final JTextField txtNombre = new JTextField();
    private final JComboBox<Rol> cbRol = new JComboBox<>();
    private final JComboBox<Bodega> cbBodega = new JComboBox<>();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JCheckBox chkActivo = new JCheckBox("Activo", true);

    public PanelUsuarios(AppEscritorio app, Usuario u) {
        super(app, u, "Usuarios y roles", "Contraseñas guardadas con hash PBKDF2. Deje la contraseña vacía para no cambiarla.", true,
                "ID", "Usuario", "Nombre", "Rol", "Bodega a cargo", "Activo");
        inicializar();
    }

    @Override
    protected Dao<Usuario> dao() { return app.catalogo().usuarios(); }

    @Override
    public void refrescar() {
        Ui.recargar(cbRol, app.catalogo().listar(app.catalogo().roles()));
        List<Bodega> bodegas = new ArrayList<>();
        bodegas.add(null);
        bodegas.addAll(app.catalogo().listar(app.catalogo().bodegas()));
        Ui.recargar(cbBodega, bodegas);
        super.refrescar();
    }

    @Override
    protected Object[] fila(Usuario e) {
        return new Object[]{e.getId(), e.getUsername(), e.getNombre(), e.getRol(), e.getBodega(), e.isActivo() ? "Sí" : "No"};
    }

    @Override
    protected void construirFormulario(Ui.Formulario f) {
        txtPassword.putClientProperty("JPasswordField.showRevealButton", true);
        f.campo("Usuario", txtUsername).campo("Nombre", txtNombre).campo("Rol", cbRol)
                .campo("Bodega (encargado)", cbBodega).campo("Contraseña", txtPassword).campo("", chkActivo);
    }

    @Override
    protected void llenarFormulario(Usuario e) {
        txtUsername.setText(e == null ? "" : e.getUsername());
        txtNombre.setText(e == null ? "" : e.getNombre());
        Ui.seleccionar(cbRol, e == null ? null : e.getIdRol());
        Ui.seleccionar(cbBodega, e == null ? null : e.getIdBodega());
        txtPassword.setText("");
        chkActivo.setSelected(e == null || e.isActivo());
    }

    @Override
    protected Usuario leerFormulario() {
        Usuario u = new Usuario();
        u.setUsername(txtUsername.getText());
        u.setNombre(txtNombre.getText().trim());
        u.setIdRol(Ui.idDe(cbRol));
        u.setIdBodega(Ui.idDe(cbBodega));
        u.setActivo(chkActivo.isSelected());
        return u;
    }

    @Override
    protected void guardar(Usuario e) {
        app.catalogo().guardarUsuario(e, new String(txtPassword.getPassword()), usuario);
    }

    @Override
    protected void eliminar(Usuario e) {
        throw new com.umg.ferreteria.excepcion.ReglaNegocioException(
                "Los usuarios no se eliminan para conservar la auditoría. Desmarque 'Activo' para deshabilitarlo.");
    }
}
