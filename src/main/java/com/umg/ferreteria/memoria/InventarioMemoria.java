package com.umg.ferreteria.memoria;

import com.umg.ferreteria.excepcion.ReglaNegocioException;
import com.umg.ferreteria.excepcion.StockInsuficienteException;
import com.umg.ferreteria.excepcion.ValidacionException;
import com.umg.ferreteria.modelo.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ENTREGA 2: reglas de negocio de la ferretería operando sobre listas en memoria.
 * Usa las mismas clases del modelo y las mismas excepciones propias que la versión con MySQL.
 * Al no haber transacciones de base de datos, cada operación verifica TODO antes de modificar
 * algo, para que una venta o transferencia se aplique completa o no se aplique.
 */
public class InventarioMemoria {

    final RepositorioMemoria<Categoria> categorias = new RepositorioMemoria<>();
    final RepositorioMemoria<Marca> marcas = new RepositorioMemoria<>();
    final RepositorioMemoria<UnidadMedida> unidades = new RepositorioMemoria<>();
    final RepositorioMemoria<Bodega> bodegas = new RepositorioMemoria<>();
    final RepositorioMemoria<Producto> productos = new RepositorioMemoria<>();
    final RepositorioMemoria<Cliente> clientes = new RepositorioMemoria<>();
    final RepositorioMemoria<Proveedor> proveedores = new RepositorioMemoria<>();
    final RepositorioMemoria<Existencia> existencias = new RepositorioMemoria<>();
    final RepositorioMemoria<Venta> ventas = new RepositorioMemoria<>();
    final RepositorioMemoria<Transferencia> transferencias = new RepositorioMemoria<>();

    public InventarioMemoria() {
        cargarDatosDePrueba();
    }

    // ---------- Consultas ----------
    public List<Categoria> categorias() { return categorias.listar(); }
    public List<Marca> marcas() { return marcas.listar(); }
    public List<UnidadMedida> unidades() { return unidades.listar(); }
    public List<Bodega> bodegas() { return bodegas.listar(); }
    public List<Producto> productos() { return productos.listar(); }
    public List<Cliente> clientes() { return clientes.listar(); }
    public List<Venta> ventas() { return ventas.listar(); }
    public List<Transferencia> transferencias() { return transferencias.listar(); }

    /** Clientes y proveedores juntos en una lista de Persona (polimorfismo: cada uno dice su tipo). */
    public List<Persona> terceros() {
        List<Persona> lista = new ArrayList<>(clientes.listar());
        lista.addAll(proveedores.listar());
        return lista;
    }

    public List<Existencia> existencias(Integer idBodega) {
        return existencias.buscar(e -> idBodega == null || e.getIdBodega().equals(idBodega));
    }

    public BigDecimal stock(int idProducto, int idBodega) {
        return existencias.buscar(e -> e.getIdProducto() == idProducto && e.getIdBodega() == idBodega)
                .stream().map(Existencia::getCantidad).findFirst().orElse(BigDecimal.ZERO);
    }

    // ---------- Productos ----------
    public Producto guardarProducto(Producto p) {
        p.validar();
        boolean repetido = !productos.buscar(x -> x.getCodigo().equals(p.getCodigo()) && !Objects.equals(x.getId(), p.getId())).isEmpty();
        if (repetido) throw new ValidacionException("Ya existe un producto con el código " + p.getCodigo() + ".");
        p.setCategoria(categorias.porId(p.getIdCategoria()).map(Categoria::getNombre).orElse("?"));
        p.setMarca(marcas.porId(p.getIdMarca()).map(Marca::getNombre).orElse("?"));
        p.setUnidad(unidades.porId(p.getIdUnidad()).map(UnidadMedida::getNombre).orElse("?"));
        productos.guardar(p);
        // Si cambió nombre o código, las existencias muestran el dato nuevo
        for (Existencia e : existencias.buscar(x -> x.getIdProducto().equals(p.getId()))) completarExistencia(e);
        return p;
    }

    public void eliminarProducto(int idProducto) {
        boolean conStock = existencias.buscar(e -> e.getIdProducto() == idProducto && e.getCantidad().signum() > 0).size() > 0;
        if (conStock) throw new ReglaNegocioException("No se puede eliminar: el producto todavía tiene existencias.");
        boolean vendido = ventas.buscar(v -> v.getDetalles().stream().anyMatch(d -> d.getIdProducto() == idProducto)).size() > 0;
        if (vendido) throw new ReglaNegocioException("No se puede eliminar: el producto aparece en ventas registradas.");
        for (Existencia e : existencias.buscar(e -> e.getIdProducto() == idProducto)) existencias.eliminar(e.getId());
        productos.eliminar(idProducto);
    }

    // ---------- Inventario ----------
    /** Regla: no se permite stock negativo en ninguna bodega. */
    private void validarDisponible(int idProducto, int idBodega, BigDecimal requerido) {
        BigDecimal disponible = stock(idProducto, idBodega);
        if (disponible.compareTo(requerido) < 0) {
            String prod = productos.porId(idProducto).map(Producto::getNombre).orElse("#" + idProducto);
            String bod = bodegas.porId(idBodega).map(Bodega::getNombre).orElse("#" + idBodega);
            throw new StockInsuficienteException(prod, bod, disponible, requerido);
        }
    }

    private void moverStock(int idProducto, int idBodega, BigDecimal delta) {
        if (delta.signum() < 0) validarDisponible(idProducto, idBodega, delta.negate());
        Existencia e = existencias.buscar(x -> x.getIdProducto() == idProducto && x.getIdBodega() == idBodega)
                .stream().findFirst().orElse(null);
        if (e == null) {
            e = new Existencia();
            e.setIdProducto(idProducto);
            e.setIdBodega(idBodega);
            completarExistencia(e);
        }
        e.setCantidad(e.getCantidad().add(delta));
        existencias.guardar(e);
    }

    private void completarExistencia(Existencia e) {
        productos.porId(e.getIdProducto()).ifPresent(p -> {
            e.setCodigo(p.getCodigo());
            e.setProducto(p.getNombre());
            e.setCategoria(p.getCategoria());
            e.setUnidad(p.getUnidad());
            e.setStockMinimo(p.getStockMinimo());
        });
        bodegas.porId(e.getIdBodega()).ifPresent(b -> e.setBodega(b.getNombre()));
    }

    // ---------- Ventas ----------
    /** Valida todas las líneas antes de descontar: la venta se aplica completa o no se aplica. */
    public Venta vender(Venta v) {
        for (DetalleVenta d : v.getDetalles()) {
            Producto p = productos.porId(d.getIdProducto()).orElseThrow(() -> new ValidacionException("Producto inexistente."));
            d.setProducto(p.getNombre());
            d.setCodigo(p.getCodigo());
            d.setIdUnidad(p.getIdUnidad());
            d.setUnidad(p.getUnidad());
            d.setCantidadBase(d.getCantidad());
            d.setPrecioUnitario(p.getPrecioVenta());
            d.setCostoUnitario(p.getPrecioCosto());
        }
        v.validar();
        for (DetalleVenta d : v.getDetalles()) validarDisponible(d.getIdProducto(), v.getIdBodega(), d.getCantidad());
        for (DetalleVenta d : v.getDetalles()) moverStock(d.getIdProducto(), v.getIdBodega(), d.getCantidad().negate());
        v.setCliente(clientes.porId(v.getIdCliente()).map(Cliente::getNombre).orElse("?"));
        v.setBodega(bodegas.porId(v.getIdBodega()).map(Bodega::getNombre).orElse("?"));
        v.setEstado(Venta.CONFIRMADA);
        v.setFecha(LocalDateTime.now());
        v.setTotal(v.calcularTotal());
        return ventas.guardar(v);
    }

    // ---------- Transferencias ----------
    /** Regla: la salida en origen y la recepción en destino son pasos separados. */
    public Transferencia enviar(Transferencia t) {
        for (DetalleTransferencia d : t.getDetalles()) {
            productos.porId(d.getIdProducto()).ifPresent(p -> {
                d.setProducto(p.getNombre());
                d.setCodigo(p.getCodigo());
                d.setUnidad(p.getUnidad());
            });
        }
        t.validar();
        for (DetalleTransferencia d : t.getDetalles()) validarDisponible(d.getIdProducto(), t.getIdBodegaOrigen(), d.getCantidad());
        for (DetalleTransferencia d : t.getDetalles()) moverStock(d.getIdProducto(), t.getIdBodegaOrigen(), d.getCantidad().negate());
        t.setBodegaOrigen(bodegas.porId(t.getIdBodegaOrigen()).map(Bodega::getNombre).orElse("?"));
        t.setBodegaDestino(bodegas.porId(t.getIdBodegaDestino()).map(Bodega::getNombre).orElse("?"));
        t.setEstado(Transferencia.EN_TRANSITO);
        t.setFecha(LocalDateTime.now());
        return transferencias.guardar(t);
    }

    public Transferencia recibir(int id) {
        Transferencia t = transferencias.porId(id).orElseThrow(() -> new ValidacionException("No existe la transferencia #" + id));
        if (!Transferencia.EN_TRANSITO.equals(t.getEstado())) {
            throw new ReglaNegocioException("La transferencia #" + id + " no está en tránsito (estado: " + t.getEstado() + ").");
        }
        for (DetalleTransferencia d : t.getDetalles()) moverStock(d.getIdProducto(), t.getIdBodegaDestino(), d.getCantidad());
        t.setEstado(Transferencia.RECIBIDA);
        t.setFechaRecepcion(LocalDateTime.now());
        return transferencias.guardar(t);
    }

    // ---------- Datos iniciales (ficticios) ----------
    private void cargarDatosDePrueba() {
        Categoria manuales = categorias.guardar(cat(new Categoria(), "Herramientas Manuales"));
        Categoria construccion = categorias.guardar(cat(new Categoria(), "Materiales de Construcción"));
        Categoria plomeria = categorias.guardar(cat(new Categoria(), "Plomería"));
        Marca truper = marcas.guardar(cat(new Marca(), "Truper"));
        Marca stanley = marcas.guardar(cat(new Marca(), "Stanley"));
        Marca progreso = marcas.guardar(cat(new Marca(), "Cementos Progreso"));
        Marca tubovinil = marcas.guardar(cat(new Marca(), "Tubovinil"));
        UnidadMedida unidad = unidades.guardar(unidad("Unidad", "u"));
        UnidadMedida saco = unidades.guardar(unidad("Saco", "sco"));

        Bodega principal = bodegas.guardar(bodega("Bodega Principal", "Central Zona 1"));
        Bodega mixco = bodegas.guardar(bodega("Bodega Mixco", "Mixco"));

        Producto martillo = guardarProducto(producto("HM-001", "Martillo de uña 16 oz", manuales, truper, unidad, "45", "75", 10));
        Producto desarmador = guardarProducto(producto("HM-002", "Desarmador Phillips #2", manuales, stanley, unidad, "12", "22", 20));
        Producto cinta = guardarProducto(producto("HM-003", "Cinta métrica 5 m", manuales, stanley, unidad, "28", "49", 10));
        Producto cemento = guardarProducto(producto("MC-001", "Cemento gris 42.5 kg", construccion, progreso, saco, "72", "89", 50));
        Producto tubo = guardarProducto(producto("PL-001", "Tubo PVC 1/2\" x 6 m", plomeria, tubovinil, unidad, "18", "32", 40));

        moverStock(martillo.getId(), principal.getId(), BigDecimal.valueOf(40));
        moverStock(desarmador.getId(), principal.getId(), BigDecimal.valueOf(100));
        moverStock(cinta.getId(), principal.getId(), BigDecimal.valueOf(30));
        moverStock(cemento.getId(), principal.getId(), BigDecimal.valueOf(300));
        moverStock(tubo.getId(), principal.getId(), BigDecimal.valueOf(150));
        moverStock(martillo.getId(), mixco.getId(), BigDecimal.valueOf(5));
        moverStock(cemento.getId(), mixco.getId(), BigDecimal.valueOf(40));

        clientes.guardar(persona(new Cliente(), "Consumidor Final", "CF"));
        clientes.guardar(persona(new Cliente(), "Constructora Los Álamos", "9988776-5"));
        proveedores.guardar(persona(new Proveedor(), "Distribuidora Ferromax S.A.", "1234567-8"));
    }

    private static <T extends CatalogoSimple> T cat(T c, String nombre) {
        c.setNombre(nombre);
        return c;
    }

    private static UnidadMedida unidad(String nombre, String abrev) {
        UnidadMedida u = new UnidadMedida();
        u.setNombre(nombre);
        u.setAbreviatura(abrev);
        return u;
    }

    private Bodega bodega(String nombre, String sucursal) {
        Bodega b = new Bodega();
        b.setNombre(nombre);
        b.setIdSucursal(1);
        b.setSucursal(sucursal);
        return b;
    }

    private static Producto producto(String codigo, String nombre, Categoria c, Marca m, UnidadMedida u,
                                     String costo, String precio, int minimo) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setIdCategoria(c.getId());
        p.setIdMarca(m.getId());
        p.setIdUnidad(u.getId());
        p.setPrecioCosto(new BigDecimal(costo));
        p.setPrecioVenta(new BigDecimal(precio));
        p.setStockMinimo(minimo);
        return p;
    }

    private static <T extends Persona> T persona(T p, String nombre, String nit) {
        p.setNombre(nombre);
        p.setNit(nit);
        return p;
    }
}
