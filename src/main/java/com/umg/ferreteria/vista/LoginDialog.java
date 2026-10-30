package com.umg.ferreteria.vista;

import com.umg.ferreteria.modelo.Usuario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Collections;

public class LoginDialog extends JDialog {

    private final JTextField txtUsuario = new JTextField(18);
    private final JPasswordField txtPassword = new JPasswordField(18);
    private Usuario usuario;

    public LoginDialog(AppEscritorio app) {
        super((Frame) null, "Iniciar sesión - Ferretería Multisucursal", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout(0, 15));
        raiz.setBorder(new EmptyBorder(25, 30, 20, 30));

        JLabel titulo = new JLabel("Ferretería Multisucursal", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 22f));
        titulo.setForeground(Ui.PRIMARIO);
        JLabel sub = new JLabel("Inventario, compras, ventas y transferencias", SwingConstants.CENTER);
        sub.setForeground(Color.GRAY);
        JPanel cab = new JPanel(new GridLayout(2, 1));
        cab.add(titulo);
        cab.add(sub);
        raiz.add(cab, BorderLayout.NORTH);

        txtUsuario.putClientProperty("JTextField.placeholderText", "usuario");
        txtPassword.putClientProperty("JTextField.placeholderText", "contraseña");
        txtPassword.putClientProperty("JPasswordField.showRevealButton", true);
        raiz.add(new Ui.Formulario().campo("Usuario", txtUsuario).campo("Contraseña", txtPassword), BorderLayout.CENTER);

        JButton btnEntrar = Ui.boton("Ingresar", Ui.PRIMARIO);
        btnEntrar.addActionListener(e -> Ui.accion(this, () -> {
            usuario = app.auth().autenticar(txtUsuario.getText(), new String(txtPassword.getPassword()), "escritorio");
            dispose();
        }));
        getRootPane().setDefaultButton(btnEntrar);

        JLabel api = new JLabel("API para la app móvil: http://" + ipLocal() + ":8080", SwingConstants.CENTER);
        api.setForeground(Ui.INFO);
        api.setFont(api.getFont().deriveFont(11f));
        JPanel pie = new JPanel(new BorderLayout(0, 10));
        pie.add(btnEntrar, BorderLayout.NORTH);
        pie.add(api, BorderLayout.SOUTH);
        raiz.add(pie, BorderLayout.SOUTH);

        setContentPane(raiz);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    public Usuario getUsuario() {
        return usuario;
    }

    /** IP de esta PC en la red local (la que debe escribirse en la app Android). */
    static String ipLocal() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                String nombre = ni.getDisplayName().toLowerCase();
                if (nombre.contains("virtual") || nombre.contains("vmware") || nombre.contains("hyper-v")) continue;
                for (var dir : Collections.list(ni.getInetAddresses())) {
                    if (dir instanceof Inet4Address && !dir.isLinkLocalAddress()) return dir.getHostAddress();
                }
            }
        } catch (Exception ignorada) {
            // sin red: se muestra localhost
        }
        return "localhost";
    }
}
