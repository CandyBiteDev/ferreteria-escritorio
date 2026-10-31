package com.umg.ferreteria;

import com.umg.ferreteria.excepcion.ReglaNegocioException;
import com.umg.ferreteria.excepcion.StockInsuficienteException;
import com.umg.ferreteria.excepcion.ValidacionException;
import com.umg.ferreteria.memoria.InventarioMemoria;
import com.umg.ferreteria.modelo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de la versión en memoria (Entrega 2). No necesitan MySQL ni Spring. */
class InventarioMemoriaTest {

    private InventarioMemoria inv;

    @BeforeEach
    void preparar() {
        inv = new InventarioMemoria();
    }

    private Producto producto(String codigo) {
        return inv.productos().stream().filter(p -> p.getCodigo().equals(codigo)).findFirst().orElseThrow();
    }

    private Bodega bodega(String nombre) {
        return inv.bodegas().stream().filter(b -> b.getNombre().equals(nombre)).findFirst().orElseThrow();
    }

    private Venta venta(String bodega, String codigo, int cantidad) {
        Venta v = new Venta();
        v.setIdCliente(inv.clientes().getFirst().getId());
        v.setIdBodega(bodega(bodega).getId());
        DetalleVenta d = new DetalleVenta();
        d.setIdProducto(producto(codigo).getId());
        d.setCantidad(BigDecimal.valueOf(cantidad));
        v.agregarDetalle(d);
        return v;
    }

    @Test
    @DisplayName("Alta de producto en la lista y código único")
    void altaYCodigoUnico() {
        int antes = inv.productos().size();
        Producto p = new Producto();
        p.setCodigo("PR-100");
        p.setNombre("Llave Stillson 14 pulgadas");
        p.setIdCategoria(inv.categorias().getFirst().getId());
        p.setIdMarca(inv.marcas().getFirst().getId());
        p.setIdUnidad(inv.unidades().getFirst().getId());
        p.setPrecioCosto(new BigDecimal("120"));
        p.setPrecioVenta(new BigDecimal("185"));
        inv.guardarProducto(p);
        assertThat(inv.productos()).hasSize(antes + 1);

        Producto repetido = new Producto();
        repetido.setCodigo("PR-100");
        repetido.setNombre("Otro");
        repetido.setIdCategoria(p.getIdCategoria());
        repetido.setIdMarca(p.getIdMarca());
        repetido.setIdUnidad(p.getIdUnidad());
        repetido.setPrecioCosto(BigDecimal.ONE);
        repetido.setPrecioVenta(BigDecimal.TEN);
        assertThatThrownBy(() -> inv.guardarProducto(repetido)).isInstanceOf(ValidacionException.class);
    }

    @Test
    @DisplayName("Venta válida descuenta stock de la lista")
    void ventaValida() {
        BigDecimal antes = inv.stock(producto("HM-001").getId(), bodega("Bodega Principal").getId());
        Venta v = inv.vender(venta("Bodega Principal", "HM-001", 3));
        assertThat(v.getEstado()).isEqualTo(Venta.CONFIRMADA);
        assertThat(v.getTotal()).isEqualByComparingTo("225");
        assertThat(inv.stock(producto("HM-001").getId(), bodega("Bodega Principal").getId()))
                .isEqualByComparingTo(antes.subtract(BigDecimal.valueOf(3)));
    }

    @Test
    @DisplayName("Regla: sin stock la venta se rechaza y no cambia nada")
    void ventaSinStock() {
        int ventasAntes = inv.ventas().size();
        assertThatThrownBy(() -> inv.vender(venta("Bodega Mixco", "HM-001", 50)))
                .isInstanceOf(StockInsuficienteException.class);
        assertThat(inv.ventas()).hasSize(ventasAntes);
        assertThat(inv.stock(producto("HM-001").getId(), bodega("Bodega Mixco").getId())).isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("Regla: transferencia con salida y recepción separadas")
    void transferencia() {
        int prod = producto("MC-001").getId();
        int principal = bodega("Bodega Principal").getId();
        int mixco = bodega("Bodega Mixco").getId();
        Transferencia t = new Transferencia();
        t.setIdBodegaOrigen(principal);
        t.setIdBodegaDestino(mixco);
        DetalleTransferencia d = new DetalleTransferencia();
        d.setIdProducto(prod);
        d.setCantidad(BigDecimal.valueOf(20));
        t.agregarDetalle(d);

        int id = inv.enviar(t).getId();
        assertThat(inv.stock(prod, principal)).isEqualByComparingTo("280");
        assertThat(inv.stock(prod, mixco)).isEqualByComparingTo("40");

        inv.recibir(id);
        assertThat(inv.stock(prod, mixco)).isEqualByComparingTo("60");
        assertThatThrownBy(() -> inv.recibir(id)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Polimorfismo: una List<Persona> con clientes y proveedores")
    void polimorfismo() {
        assertThat(inv.terceros()).extracting(Persona::getTipoPersona).contains("Cliente", "Proveedor");
    }
}
