package com.umg.ferreteria;

import com.umg.ferreteria.dao.ConexionBD;
import com.umg.ferreteria.excepcion.*;
import com.umg.ferreteria.modelo.*;
import com.umg.ferreteria.seguridad.PasswordUtil;
import com.umg.ferreteria.servicio.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración de las reglas de negocio del Proyecto 05 contra MySQL
 * (base db_ferreteria_test con los datos de demostración).
 * Cubren: operación válida, operación inválida por regla y falla controlada de persistencia.
 */
@SpringBootTest(args = BaseDePrueba.REINICIAR, properties = BaseDePrueba.URL)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReglasNegocioTest {

    // Credenciales de los usuarios ficticios que crea DatosIniciales
    private static final String PASS_VENDEDOR = "Vende2026";
    private static final String PASS_SUPERVISOR = "Super2026";
    private static final String PASS_ADMIN = "Admin2026";
    private static final String PASS_BODEGA = "Bodega2026";

    @Autowired private AuthService auth;
    @Autowired private CatalogoService catalogo;
    @Autowired private InventarioService inventario;
    @Autowired private VentaService ventas;
    @Autowired private CompraService compras;
    @Autowired private ConexionBD db;

    // ---------- Ayudas ----------
    private Usuario vendedor() { return auth.autenticar("vendedor", PASS_VENDEDOR, "prueba"); }
    private Usuario supervisor() { return auth.autenticar("supervisor", PASS_SUPERVISOR, "prueba"); }
    private Usuario admin() { return auth.autenticar("admin", PASS_ADMIN, "prueba"); }
    private Usuario encargadoMixco() { return auth.autenticar("encargado.mixco", PASS_BODEGA, "prueba"); }

    private int producto(String codigo) {
        return catalogo.listar(catalogo.productos()).stream().filter(p -> p.getCodigo().equals(codigo)).findFirst().orElseThrow().getId();
    }

    private int bodega(String nombre) {
        return catalogo.listar(catalogo.bodegas()).stream().filter(b -> b.getNombre().equals(nombre)).findFirst().orElseThrow().getId();
    }

    private int unidad(String nombre) {
        return catalogo.listar(catalogo.unidades()).stream().filter(u -> u.getNombre().equals(nombre)).findFirst().orElseThrow().getId();
    }

    private BigDecimal stock(String codigo, String bodega) {
        return inventario.disponible(producto(codigo), bodega(bodega));
    }

    private Venta cotizacionEn(String bodega) {
        return ventas.listar().stream()
                .filter(v -> Venta.COTIZACION.equals(v.getEstado()) && v.getBodega().equals(bodega))
                .findFirst().orElseThrow();
    }

    private Venta ventaNueva(String bodega, String codigo, String unidad, int cantidad) {
        Venta v = new Venta();
        v.setIdCliente(1);
        v.setIdBodega(bodega(bodega));
        DetalleVenta d = new DetalleVenta();
        d.setIdProducto(producto(codigo));
        d.setIdUnidad(unidad(unidad));
        d.setCantidad(BigDecimal.valueOf(cantidad));
        v.agregarDetalle(d);
        return v;
    }

    private int contarVentas() {
        return db.consultar(cn -> com.umg.ferreteria.dao.Jdbc.escalar(cn, "SELECT COUNT(*) FROM venta")).intValue();
    }

    private static void igual(BigDecimal real, int esperado) {
        assertThat(real).isEqualByComparingTo(BigDecimal.valueOf(esperado));
    }

    // ---------- Cotizaciones ----------
    @Test
    @Order(1)
    @DisplayName("Regla: no convertir una cotización en venta si faltan existencias")
    void cotizacionSinStockNoSeConvierte() {
        Venta q = cotizacionEn("Bodega Mostrador"); // 5 taladros; la bodega tiene 2
        BigDecimal antes = stock("HE-001", "Bodega Mostrador");

        assertThatThrownBy(() -> ventas.convertirCotizacion(q.getId(), vendedor()))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("No se puede convertir")
                .hasMessageContaining("Stock insuficiente");

        assertThat(ventas.buscar(q.getId()).getEstado()).isEqualTo(Venta.COTIZACION);
        assertThat(stock("HE-001", "Bodega Mostrador")).isEqualByComparingTo(antes);
    }

    @Test
    @Order(2)
    @DisplayName("Operación válida: convertir una cotización con stock descuenta el inventario")
    void cotizacionConStockSeConvierte() {
        Venta q = cotizacionEn("Bodega Mixco"); // 3 martillos y 10 sacos de cemento
        BigDecimal martillos = stock("HM-001", "Bodega Mixco");
        BigDecimal cemento = stock("MC-001", "Bodega Mixco");

        ventas.convertirCotizacion(q.getId(), vendedor());

        assertThat(ventas.buscar(q.getId()).getEstado()).isEqualTo(Venta.CONFIRMADA);
        assertThat(stock("HM-001", "Bodega Mixco")).isEqualByComparingTo(martillos.subtract(BigDecimal.valueOf(3)));
        assertThat(stock("MC-001", "Bodega Mixco")).isEqualByComparingTo(cemento.subtract(BigDecimal.TEN));
    }

    // ---------- Unidades equivalentes ----------
    @Test
    @Order(3)
    @DisplayName("Regla: vender 1 caja de tornillos descuenta 100 unidades (conversión definida)")
    void ventaEnCajaUsaConversion() {
        BigDecimal antes = stock("TO-001", "Bodega Principal");

        Venta v = ventas.vender(ventaNueva("Bodega Principal", "TO-001", "Caja", 1), vendedor());

        igual(v.getDetalles().getFirst().getCantidadBase(), 100);
        assertThat(stock("TO-001", "Bodega Principal")).isEqualByComparingTo(antes.subtract(BigDecimal.valueOf(100)));
    }

    @Test
    @Order(4)
    @DisplayName("Regla: no se vende en una unidad sin conversión definida")
    void ventaSinConversionSeRechaza() {
        int antes = contarVentas();

        assertThatThrownBy(() -> ventas.vender(ventaNueva("Bodega Principal", "MC-001", "Caja", 1), vendedor()))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("No hay conversión definida");

        assertThat(contarVentas()).isEqualTo(antes);
    }

    // ---------- Stock negativo + rollback ----------
    @Test
    @Order(5)
    @DisplayName("Regla: no permitir stock negativo; la venta completa hace rollback")
    void ventaSinStockHaceRollback() {
        int ventasAntes = contarVentas();
        BigDecimal taladros = stock("HE-001", "Bodega Principal");

        assertThatThrownBy(() -> ventas.vender(ventaNueva("Bodega Principal", "HE-001", "Unidad", 50), vendedor()))
                .isInstanceOf(StockInsuficienteException.class);

        assertThat(contarVentas()).as("no quedó encabezado de venta a medias").isEqualTo(ventasAntes);
        assertThat(stock("HE-001", "Bodega Principal")).isEqualByComparingTo(taladros);
    }

    @Test
    @Order(6)
    @DisplayName("Regla: un ajuste no puede dejar stock negativo")
    void ajusteNoDejaStockNegativo() {
        AjusteInventario a = new AjusteInventario();
        a.setIdProducto(producto("HM-001"));
        a.setIdBodega(bodega("Bodega Principal"));
        a.setCantidad(BigDecimal.valueOf(-10_000));
        a.setMotivo("Prueba de stock negativo");

        assertThatThrownBy(() -> inventario.registrarAjuste(a, supervisor(), supervisor()))
                .isInstanceOf(StockInsuficienteException.class);
    }

    // ---------- Ajustes autorizados ----------
    @Test
    @Order(7)
    @DisplayName("Regla: un ajuste requiere autorización de un usuario con permisos")
    void ajusteRequiereAutorizacion() {
        AjusteInventario a = new AjusteInventario();
        a.setIdProducto(producto("HM-002"));
        a.setIdBodega(bodega("Bodega Principal"));
        a.setCantidad(BigDecimal.valueOf(-1));
        a.setMotivo("Desarmador dañado");

        assertThatThrownBy(() -> auth.autorizar("vendedor", PASS_VENDEDOR))
                .isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> inventario.registrarAjuste(a, vendedor(), vendedor()))
                .isInstanceOf(AccesoDenegadoException.class);

        BigDecimal antes = stock("HM-002", "Bodega Principal");
        inventario.registrarAjuste(a, vendedor(), auth.autorizar("supervisor", PASS_SUPERVISOR));
        assertThat(stock("HM-002", "Bodega Principal")).isEqualByComparingTo(antes.subtract(BigDecimal.ONE));
    }

    @Test
    @Order(8)
    @DisplayName("Regla: un ajuste requiere motivo")
    void ajusteRequiereMotivo() {
        AjusteInventario a = new AjusteInventario();
        a.setIdProducto(producto("HM-002"));
        a.setIdBodega(bodega("Bodega Principal"));
        a.setCantidad(BigDecimal.ONE);
        a.setMotivo("");

        assertThatThrownBy(() -> inventario.registrarAjuste(a, supervisor(), supervisor()))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("Motivo");
    }

    // ---------- Transferencias ----------
    @Test
    @Order(9)
    @DisplayName("Regla: la transferencia sale de origen al enviar y entra a destino solo al recibir")
    void transferenciaConEstadosSeparados() {
        BigDecimal origen = stock("HM-003", "Bodega Principal");
        BigDecimal destino = stock("HM-003", "Bodega Villa Nueva");

        Transferencia t = new Transferencia();
        t.setIdBodegaOrigen(bodega("Bodega Principal"));
        t.setIdBodegaDestino(bodega("Bodega Villa Nueva"));
        DetalleTransferencia d = new DetalleTransferencia();
        d.setIdProducto(producto("HM-003"));
        d.setCantidad(BigDecimal.valueOf(4));
        t.agregarDetalle(d);
        int id = inventario.enviarTransferencia(t, admin()).getId();

        assertThat(inventario.buscarTransferencia(id).getEstado()).isEqualTo(Transferencia.EN_TRANSITO);
        assertThat(stock("HM-003", "Bodega Principal")).isEqualByComparingTo(origen.subtract(BigDecimal.valueOf(4)));
        assertThat(stock("HM-003", "Bodega Villa Nueva")).isEqualByComparingTo(destino);

        assertThatThrownBy(() -> inventario.recibirTransferencia(id, encargadoMixco(), null))
                .as("el encargado de otra bodega no puede recibir")
                .isInstanceOf(AccesoDenegadoException.class);

        inventario.recibirTransferencia(id, supervisor(), "completo");
        assertThat(inventario.buscarTransferencia(id).getEstado()).isEqualTo(Transferencia.RECIBIDA);
        assertThat(stock("HM-003", "Bodega Villa Nueva")).isEqualByComparingTo(destino.add(BigDecimal.valueOf(4)));

        assertThatThrownBy(() -> inventario.recibirTransferencia(id, supervisor(), null))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @Order(10)
    @DisplayName("Validación: origen y destino de una transferencia deben ser distintos")
    void transferenciaMismaBodegaSeRechaza() {
        Transferencia t = new Transferencia();
        t.setIdBodegaOrigen(bodega("Bodega Principal"));
        t.setIdBodegaDestino(bodega("Bodega Principal"));
        DetalleTransferencia d = new DetalleTransferencia();
        d.setIdProducto(producto("HM-001"));
        d.setCantidad(BigDecimal.ONE);
        t.agregarDetalle(d);

        assertThatThrownBy(() -> inventario.enviarTransferencia(t, admin()))
                .isInstanceOf(ValidacionException.class);
    }

    // ---------- Compras y devoluciones ----------
    @Test
    @Order(11)
    @DisplayName("Operación válida: recibir una compra suma el stock una sola vez")
    void compraRecibidaSumaStock() {
        Compra pendiente = compras.listar().stream().filter(c -> Compra.PENDIENTE.equals(c.getEstado())).findFirst().orElseThrow();
        BigDecimal martillos = stock("HM-001", "Bodega Principal");

        compras.recibir(pendiente.getId(), admin()); // 20 martillos y 4 taladros

        assertThat(stock("HM-001", "Bodega Principal")).isEqualByComparingTo(martillos.add(BigDecimal.valueOf(20)));
        assertThatThrownBy(() -> compras.recibir(pendiente.getId(), admin())).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @Order(12)
    @DisplayName("Regla: una devolución no puede exceder lo vendido")
    void devolucionNoExcedeLoVendido() {
        Venta v = ventas.listar().stream()
                .filter(x -> Venta.CONFIRMADA.equals(x.getEstado()) && x.getCliente().startsWith("Juan Carlos"))
                .findFirst().orElseThrow(); // vendió 2 martillos

        Devolucion demasiado = new Devolucion();
        demasiado.setIdVenta(v.getId());
        demasiado.setIdProducto(producto("HM-001"));
        demasiado.setCantidadBase(BigDecimal.valueOf(5));
        demasiado.setMotivo("Prueba");
        assertThatThrownBy(() -> ventas.devolver(demasiado, vendedor()))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("excede");

        Devolucion valida = new Devolucion();
        valida.setIdVenta(v.getId());
        valida.setIdProducto(producto("HM-001"));
        valida.setCantidadBase(BigDecimal.ONE);
        valida.setMotivo("Mango quebrado");
        assertThat(ventas.devolver(valida, vendedor()).getId()).isPositive();
    }

    // ---------- Permisos y persistencia ----------
    @Test
    @Order(13)
    @DisplayName("Permisos: un vendedor no puede anular ventas")
    void vendedorNoPuedeAnular() {
        Venta cualquiera = ventas.listar().getFirst();
        assertThatThrownBy(() -> ventas.anular(cualquiera.getId(), vendedor()))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @Order(14)
    @DisplayName("Falla controlada de persistencia: no se elimina una categoría con productos")
    void eliminarCategoriaEnUsoEsFallaControlada() {
        Categoria cat = catalogo.listar(catalogo.categorias()).stream()
                .filter(c -> c.getNombre().equals("Herramientas Manuales")).findFirst().orElseThrow();

        assertThatThrownBy(() -> catalogo.eliminar(catalogo.categorias(), cat, admin()))
                .isInstanceOf(PersistenciaException.class)
                .hasMessageContaining("No se puede eliminar");

        assertThat(catalogo.listar(catalogo.categorias())).extracting(Categoria::getNombre).contains("Herramientas Manuales");
    }

    // ---------- Seguridad ----------
    @Test
    @Order(15)
    @DisplayName("Seguridad: login con contraseña incorrecta se rechaza")
    void loginIncorrecto() {
        assertThatThrownBy(() -> auth.autenticar("admin", "otra-clave1", "prueba"))
                .isInstanceOf(AutenticacionException.class);
    }

    @Test
    @Order(16)
    @DisplayName("Seguridad: las contraseñas se guardan con hash PBKDF2 y sal")
    void contrasenasConHash() {
        String h1 = PasswordUtil.hash("Clave2026");
        String h2 = PasswordUtil.hash("Clave2026");

        assertThat(h1).startsWith("pbkdf2$").isNotEqualTo(h2).doesNotContain("Clave2026");
        assertThat(PasswordUtil.verificar("Clave2026", h1)).isTrue();
        assertThat(PasswordUtil.verificar("Clave2027", h1)).isFalse();

        String guardado = db.consultar(cn -> {
            try (var ps = cn.prepareStatement("SELECT password_hash FROM usuario WHERE username = 'admin'");
                 var rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        });
        assertThat(guardado).startsWith("pbkdf2$").doesNotContain(PASS_ADMIN);
    }
}
