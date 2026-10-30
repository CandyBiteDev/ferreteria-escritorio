package com.umg.ferreteria.vista;

import com.umg.ferreteria.modelo.Rol;
import com.umg.ferreteria.modelo.Usuario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Ventana principal: muestra solo los módulos que el rol del usuario puede usar. */
public class VentanaPrincipal extends JFrame {

    public VentanaPrincipal(AppEscritorio app, Usuario u) {
        super("Ferretería Multisucursal — " + u.getNombre());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 760);
        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(null);

        // Barra superior
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Ui.PRIMARIO);
        barra.setBorder(new EmptyBorder(10, 16, 10, 16));
        JLabel titulo = new JLabel("Ferretería Multisucursal");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        barra.add(titulo, BorderLayout.WEST);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        derecha.setOpaque(false);
        String extra = u.getBodega() != null ? " · " + u.getBodega() : "";
        JLabel lblUsuario = new JLabel(u.getNombre() + "  [" + u.getRol() + "]" + extra);
        lblUsuario.setForeground(Color.WHITE);
        JButton btnSalir = Ui.boton("Cerrar sesión");
        btnSalir.addActionListener(e -> app.cerrarSesion());
        derecha.add(lblUsuario);
        derecha.add(btnSalir);
        barra.add(derecha, BorderLayout.EAST);

        // Módulos según rol
        JTabbedPane tabs = new JTabbedPane();
        tabs.putClientProperty("JTabbedPane.tabType", "card");
        boolean admin = u.tieneRol(Rol.ADMIN);
        boolean gestion = u.tieneRol(Rol.ADMIN, Rol.SUPERVISOR);

        tabs.addTab("Existencias", new PanelExistencias(app, u));
        if (u.tieneRol(Rol.ADMIN, Rol.SUPERVISOR, Rol.VENDEDOR)) tabs.addTab("Ventas y cotizaciones", new PanelVentas(app, u));
        if (u.tieneRol(Rol.ADMIN, Rol.SUPERVISOR, Rol.ENCARGADO)) {
            tabs.addTab("Transferencias", new PanelTransferencias(app, u));
            tabs.addTab("Compras", new PanelCompras(app, u));
        }
        tabs.addTab("Productos", new PanelProductos(app, u, gestion));
        if (u.tieneRol(Rol.ADMIN, Rol.SUPERVISOR, Rol.VENDEDOR)) tabs.addTab("Clientes", new PanelClientes(app, u));
        if (gestion) {
            tabs.addTab("Proveedores", new PanelProveedores(app, u));
            JTabbedPane catalogos = new JTabbedPane();
            catalogos.addTab("Categorías", PanelCatalogoSimple.categorias(app, u));
            catalogos.addTab("Marcas", PanelCatalogoSimple.marcas(app, u));
            catalogos.addTab("Unidades de medida", new PanelUnidades(app, u));
            catalogos.addTab("Sucursales", new PanelSucursales(app, u));
            catalogos.addTab("Bodegas", new PanelBodegas(app, u));
            tabs.addTab("Catálogos", catalogos);
        }
        tabs.addTab("Reportes", new PanelReportes(app, u));
        if (admin) tabs.addTab("Usuarios", new PanelUsuarios(app, u));

        JPanel contenido = new JPanel(new BorderLayout());
        contenido.setBorder(new EmptyBorder(8, 8, 8, 8));
        contenido.add(tabs, BorderLayout.CENTER);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(barra, BorderLayout.NORTH);
        getContentPane().add(contenido, BorderLayout.CENTER);
    }
}
