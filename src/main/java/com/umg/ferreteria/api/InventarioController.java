package com.umg.ferreteria.api;

import com.umg.ferreteria.modelo.*;
import com.umg.ferreteria.seguridad.AuthInterceptor;
import com.umg.ferreteria.servicio.CatalogoService;
import com.umg.ferreteria.servicio.InventarioService;
import com.umg.ferreteria.servicio.ReporteService;
import com.umg.ferreteria.servicio.TipoReporte;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Endpoints que consume la app móvil de los encargados de bodega. */
@RestController
@RequestMapping("/api")
public class InventarioController {

    private final InventarioService inventario;
    private final CatalogoService catalogo;
    private final ReporteService reportes;

    public InventarioController(InventarioService inventario, CatalogoService catalogo, ReporteService reportes) {
        this.inventario = inventario;
        this.catalogo = catalogo;
        this.reportes = reportes;
    }

    @GetMapping("/bodegas")
    public List<Bodega> bodegas() {
        return catalogo.listar(catalogo.bodegas()).stream().filter(Bodega::isActivo).toList();
    }

    @GetMapping("/productos")
    public List<Producto> productos() {
        return catalogo.productosActivos();
    }

    /** Stock por bodega con búsqueda opcional: /api/existencias?bodega=3&q=cemento */
    @GetMapping("/existencias")
    public List<Existencia> existencias(@RequestParam(required = false) Integer bodega,
                                        @RequestParam(required = false) String q) {
        return inventario.existencias(bodega, q);
    }

    /** /api/transferencias?destino=3&estado=EN_TRANSITO */
    @GetMapping("/transferencias")
    public List<Transferencia> transferencias(@RequestParam(required = false) Integer destino,
                                              @RequestParam(required = false) String estado) {
        return inventario.listarTransferencias(destino, estado);
    }

    @GetMapping("/transferencias/{id}")
    public Transferencia transferencia(@PathVariable int id) {
        return inventario.buscarTransferencia(id);
    }

    /** Confirmar recepción en la bodega destino. Body opcional: {"observacion": "..."} */
    @PostMapping("/transferencias/{id}/recibir")
    public Transferencia recibir(@PathVariable int id,
                                 @RequestBody(required = false) Map<String, String> body,
                                 @RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario u) {
        String obs = body == null ? null : body.get("observacion");
        return inventario.recibirTransferencia(id, u, obs);
    }

    @GetMapping("/reportes/{tipo}")
    public ResultadoReporte reporte(@PathVariable TipoReporte tipo,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                    @RequestParam(required = false) Integer bodega,
                                    @RequestParam(defaultValue = "30") int dias) {
        return reportes.generar(tipo, desde, hasta, bodega, dias);
    }
}
