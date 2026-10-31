package com.umg.ferreteria.servicio;

import com.umg.ferreteria.dao.ConexionBD;
import com.umg.ferreteria.dao.Jdbc;
import com.umg.ferreteria.dao.UsuarioDao;
import com.umg.ferreteria.dao.VentaDao;
import com.umg.ferreteria.modelo.*;
import com.umg.ferreteria.seguridad.PasswordUtil;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * Al arrancar por primera vez (tabla usuario vacía) crea los usuarios de prueba
 * con contraseña hasheada y genera operaciones de demostración pasando por los
 * mismos servicios que usa la aplicación (así el kardex y la auditoría quedan consistentes).
 * Las contraseñas de prueba están documentadas en README.md (son ficticias).
 */
@Component
public class DatosIniciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final ConexionBD db;
    private final UsuarioDao usuarioDao;
    private final VentaDao ventaDao;
    private final CompraService compras;
    private final VentaService ventas;
    private final InventarioService inventario;
    private final DataSource dataSource;

    public DatosIniciales(ConexionBD db, UsuarioDao usuarioDao, VentaDao ventaDao, CompraService compras,
                          VentaService ventas, InventarioService inventario, DataSource dataSource) {
        this.db = db;
        this.dataSource = dataSource;
        this.usuarioDao = usuarioDao;
        this.ventaDao = ventaDao;
        this.compras = compras;
        this.ventas = ventas;
        this.inventario = inventario;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            boolean faltaEsquema = db.consultar(cn -> Jdbc.escalar(cn, "SELECT COUNT(*) FROM information_schema.tables "
                    + "WHERE table_schema = DATABASE() AND table_name IN ('usuario', 'movimiento_inventario')")).intValue() < 2;
            if (args.containsOption("reiniciar-bd") || faltaEsquema) reiniciarBaseDeDatos();
            if (db.consultar(usuarioDao::contar) > 0) return;
            log.info("Base de datos nueva: creando usuarios y operaciones de demostración...");
            crearUsuarios();
            crearOperacionesDemo();
            log.info("Datos de demostración listos.");
        } catch (RuntimeException e) {
            log.error("No se pudieron crear los datos iniciales: {}. ¿Ejecutaste sql/db_ferreteria.sql?", e.getMessage(), e);
        }
    }

    /**
     * --reiniciar-bd: ejecuta sql/db_ferreteria.sql (borra y recrea la base con datos de prueba).
     * El script se toma de dentro de la app (Maven lo copia al classpath); si no está, de la carpeta sql/.
     */
    private void reiniciarBaseDeDatos() {
        Resource script = new ClassPathResource("sql/db_ferreteria.sql");
        if (!script.exists()) script = new FileSystemResource("sql/db_ferreteria.sql");
        if (!script.exists()) {
            throw new IllegalStateException("No se encontró el script sql/db_ferreteria.sql. "
                    + "Ejecútalo manualmente en MySQL Workbench y vuelve a abrir la aplicación");
        }
        Resource origen = script;
        db.consultar(cn -> {
            // El script crea "db_ferreteria"; se ejecuta sobre la base configurada (ej. db_ferreteria_test en pruebas)
            String base = cn.getCatalog();
            log.warn("Reiniciando la base de datos {} con {}", base, origen.getDescription());
            String sql;
            try {
                sql = origen.getContentAsString(StandardCharsets.UTF_8).replace("db_ferreteria", base);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer " + origen.getDescription(), e);
            }
            ScriptUtils.executeSqlScript(cn, new EncodedResource(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
            return null;
        });
        // Las demás conexiones del pool apuntaban a la base anterior: se renuevan
        if (dataSource instanceof HikariDataSource h && h.getHikariPoolMXBean() != null) {
            h.getHikariPoolMXBean().softEvictConnections();
        }
    }

    private void crearUsuarios() {
        db.transaccion(cn -> {
            crear(cn, "admin", "Admin2026", "Administrador General", Rol.ADMIN, null);
            crear(cn, "supervisor", "Super2026", "Sofía Ramírez (Supervisora)", Rol.SUPERVISOR, null);
            crear(cn, "vendedor", "Vende2026", "Marco Gómez (Vendedor)", Rol.VENDEDOR, null);
            crear(cn, "encargado.central", "Bodega2026", "Luis Ortiz (Bodega Principal)", Rol.ENCARGADO, 1);
            crear(cn, "encargado.mixco", "Bodega2026", "Ana Castillo (Bodega Mixco)", Rol.ENCARGADO, 3);
            return null;
        });
    }

    private void crear(java.sql.Connection cn, String user, String pass, String nombre, String rol, Integer bodega) throws java.sql.SQLException {
        Usuario u = new Usuario();
        u.setUsername(user);
        u.setNombre(nombre);
        u.setIdRol(Jdbc.escalar(cn, "SELECT id FROM rol WHERE nombre = ?", rol).intValue());
        u.setIdBodega(bodega);
        int id = usuarioDao.insertar(cn, u);
        usuarioDao.cambiarPassword(cn, id, PasswordUtil.hash(pass));
    }

    private Usuario usuario(String username) {
        return db.consultar(cn -> usuarioDao.buscarPorUsername(cn, username)).orElseThrow();
    }

    private void crearOperacionesDemo() {
        Usuario admin = usuario("admin");
        Usuario sup = usuario("supervisor");
        Usuario vend = usuario("vendedor");
        Usuario encMixco = usuario("encargado.mixco");

        // Compra recibida (Bodega Mixco) y compra pendiente (Bodega Principal)
        Compra c1 = new Compra();
        c1.setIdProveedor(3);
        c1.setIdBodega(3);
        c1.agregarDetalle(detCompra(6, 100, "70.50"));
        c1.agregarDetalle(detCompra(7, 50, "17.00"));
        compras.recibir(compras.registrar(c1, admin).getId(), encMixco);

        Compra c2 = new Compra();
        c2.setIdProveedor(1);
        c2.setIdBodega(1);
        c2.agregarDetalle(detCompra(1, 20, "44.00"));
        c2.agregarDetalle(detCompra(4, 4, "470.00"));
        compras.registrar(c2, admin);

        // Ventas confirmadas (una con unidad equivalente: cajas y rollos)
        Venta v1 = venta(2, 1);
        v1.agregarDetalle(detVenta(6, 3, 20));   // 20 sacos de cemento
        v1.agregarDetalle(detVenta(11, 2, 2));   // 2 cajas de tornillos = 200 u
        v1.agregarDetalle(detVenta(9, 5, 1));    // 1 rollo de cable = 100 m
        Venta vendida = ventas.vender(v1, vend);

        Venta v2 = venta(3, 2);
        v2.agregarDetalle(detVenta(1, 1, 2));
        v2.agregarDetalle(detVenta(2, 1, 3));
        ventas.vender(v2, vend);

        Venta v3 = venta(1, 3);
        v3.agregarDetalle(detVenta(6, 3, 15));
        v3.agregarDetalle(detVenta(8, 2, 1));    // 1 caja de codos = 50 u
        ventas.vender(v3, vend);

        // Cotización convertible y cotización que NO se puede convertir (falta stock)
        Venta q1 = venta(4, 3);
        q1.agregarDetalle(detVenta(1, 1, 3));
        q1.agregarDetalle(detVenta(6, 3, 10));
        ventas.cotizar(q1, vend);

        Venta q2 = venta(2, 2);
        q2.agregarDetalle(detVenta(4, 1, 5));    // Bodega Mostrador solo tiene 2 taladros
        ventas.cotizar(q2, vend);

        // Devolución parcial
        Devolucion d = new Devolucion();
        d.setIdVenta(vendida.getId());
        d.setIdProducto(11);
        d.setCantidadBase(new BigDecimal("20"));
        d.setMotivo("Producto defectuoso");
        ventas.devolver(d, vend);

        // Transferencias: dos en tránsito (para la app móvil) y una recibida
        Transferencia t1 = transferencia(1, 3, "Reabastecimiento semanal Mixco");
        t1.agregarDetalle(detTrf(6, 30));
        t1.agregarDetalle(detTrf(1, 5));
        t1.agregarDetalle(detTrf(9, 100));
        inventario.enviarTransferencia(t1, admin);

        Transferencia t2 = transferencia(1, 4, "Pedido Villa Nueva");
        t2.agregarDetalle(detTrf(10, 5));
        inventario.enviarTransferencia(t2, admin);

        Transferencia t3 = transferencia(1, 2, "Surtido de mostrador");
        t3.agregarDetalle(detTrf(2, 10));
        Transferencia enviada = inventario.enviarTransferencia(t3, admin);
        inventario.recibirTransferencia(enviada.getId(), sup, "Completo");

        // Ajuste autorizado
        AjusteInventario a = new AjusteInventario();
        a.setIdProducto(1);
        a.setIdBodega(2);
        a.setCantidad(new BigDecimal("-1"));
        a.setMotivo("Martillo dañado en exhibición");
        inventario.registrarAjuste(a, vend, sup);
    }

    private static DetalleCompra detCompra(int prod, int cant, String costo) {
        DetalleCompra d = new DetalleCompra();
        d.setIdProducto(prod);
        d.setCantidad(BigDecimal.valueOf(cant));
        d.setCostoUnitario(new BigDecimal(costo));
        return d;
    }

    private static Venta venta(int cliente, int bodega) {
        Venta v = new Venta();
        v.setIdCliente(cliente);
        v.setIdBodega(bodega);
        return v;
    }

    private static DetalleVenta detVenta(int prod, int unidad, int cant) {
        DetalleVenta d = new DetalleVenta();
        d.setIdProducto(prod);
        d.setIdUnidad(unidad);
        d.setCantidad(BigDecimal.valueOf(cant));
        return d;
    }

    private static Transferencia transferencia(int origen, int destino, String obs) {
        Transferencia t = new Transferencia();
        t.setIdBodegaOrigen(origen);
        t.setIdBodegaDestino(destino);
        t.setObservacion(obs);
        return t;
    }

    private static DetalleTransferencia detTrf(int prod, int cant) {
        DetalleTransferencia d = new DetalleTransferencia();
        d.setIdProducto(prod);
        d.setCantidad(BigDecimal.valueOf(cant));
        return d;
    }
}
