package com.umg.ferreteria.vista;

import com.umg.ferreteria.modelo.Usuario;
import com.umg.ferreteria.seguridad.SesionManager;
import com.umg.ferreteria.servicio.*;
import org.springframework.stereotype.Component;

/**
 * Controlador de la aplicación de escritorio: maneja el ciclo login -> ventana -> logout
 * y da a las pantallas acceso a los servicios (fachadas).
 */
@Component
public class AppEscritorio {

    private final AuthService auth;
    private final CatalogoService catalogo;
    private final InventarioService inventario;
    private final CompraService compras;
    private final VentaService ventas;
    private final ReporteService reportes;
    private VentanaPrincipal ventana;

    public AppEscritorio(AuthService auth, CatalogoService catalogo, InventarioService inventario,
                         CompraService compras, VentaService ventas, ReporteService reportes) {
        this.auth = auth;
        this.catalogo = catalogo;
        this.inventario = inventario;
        this.compras = compras;
        this.ventas = ventas;
        this.reportes = reportes;
    }

    public void iniciar() {
        Ui.instalarTema();
        mostrarLogin();
    }

    private void mostrarLogin() {
        LoginDialog login = new LoginDialog(this);
        login.setVisible(true);
        Usuario u = login.getUsuario();
        if (u == null) {
            System.exit(0);
            return;
        }
        SesionManager.getInstancia().iniciarSesion(u);
        ventana = new VentanaPrincipal(this, u);
        ventana.setVisible(true);
    }

    public void cerrarSesion() {
        SesionManager.getInstancia().cerrarSesion();
        if (ventana != null) ventana.dispose();
        mostrarLogin();
    }

    public AuthService auth() { return auth; }
    public CatalogoService catalogo() { return catalogo; }
    public InventarioService inventario() { return inventario; }
    public CompraService compras() { return compras; }
    public VentaService ventas() { return ventas; }
    public ReporteService reportes() { return reportes; }
}
