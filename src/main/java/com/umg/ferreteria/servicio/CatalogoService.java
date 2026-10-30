package com.umg.ferreteria.servicio;

import com.umg.ferreteria.dao.*;
import com.umg.ferreteria.excepcion.ValidacionException;
import com.umg.ferreteria.modelo.*;
import com.umg.ferreteria.seguridad.PasswordUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * PATRÓN FACADE: la vista (Swing) y la API usan este servicio sin conocer
 * conexiones, transacciones ni SQL. CRUD genérico para todos los catálogos.
 */
@Service
public class CatalogoService {

    private final ConexionBD db;
    private final AuditoriaDao auditoria;
    private final CategoriaDao categorias;
    private final MarcaDao marcas;
    private final UnidadMedidaDao unidades;
    private final SucursalDao sucursales;
    private final BodegaDao bodegas;
    private final ClienteDao clientes;
    private final ProveedorDao proveedores;
    private final ProductoDao productos;
    private final ConversionUnidadDao conversiones;
    private final UsuarioDao usuarios;
    private final RolDao roles;

    public CatalogoService(ConexionBD db, AuditoriaDao auditoria, CategoriaDao categorias, MarcaDao marcas,
                           UnidadMedidaDao unidades, SucursalDao sucursales, BodegaDao bodegas, ClienteDao clientes,
                           ProveedorDao proveedores, ProductoDao productos, ConversionUnidadDao conversiones,
                           UsuarioDao usuarios, RolDao roles) {
        this.db = db;
        this.auditoria = auditoria;
        this.categorias = categorias;
        this.marcas = marcas;
        this.unidades = unidades;
        this.sucursales = sucursales;
        this.bodegas = bodegas;
        this.clientes = clientes;
        this.proveedores = proveedores;
        this.productos = productos;
        this.conversiones = conversiones;
        this.usuarios = usuarios;
        this.roles = roles;
    }

    // ---------- CRUD genérico ----------
    public <T extends EntidadBase> List<T> listar(Dao<T> dao) {
        return db.consultar(dao::listar);
    }

    public <T extends EntidadBase> T guardar(Dao<T> dao, T entidad, Usuario actor) {
        entidad.validar();
        return db.transaccion(cn -> {
            String accion;
            if (entidad.esNuevo()) {
                entidad.setId(dao.insertar(cn, entidad));
                accion = "CREAR";
            } else {
                dao.actualizar(cn, entidad);
                accion = "MODIFICAR";
            }
            auditoria.registrar(cn, actor.getId(), accion, entidad.getClass().getSimpleName(), "#" + entidad.getId() + " " + entidad);
            return entidad;
        });
    }

    public <T extends EntidadBase> void eliminar(Dao<T> dao, T entidad, Usuario actor) {
        Permisos.requiere(actor, "eliminar registros", Rol.ADMIN, Rol.SUPERVISOR);
        db.transaccion(cn -> {
            dao.eliminar(cn, entidad.getId());
            auditoria.registrar(cn, actor.getId(), "ELIMINAR", entidad.getClass().getSimpleName(), "#" + entidad.getId() + " " + entidad);
            return null;
        });
    }

    // ---------- Accesos específicos ----------
    public List<Producto> productosActivos() {
        return db.consultar(productos::listarActivos);
    }

    public List<ConversionUnidad> conversionesDe(int idProducto) {
        return db.consultar(cn -> conversiones.listarPorProducto(cn, idProducto));
    }

    public List<Usuario> usuariosQueAutorizan() {
        return db.consultar(cn -> usuarios.listarPorRol(cn, Rol.ADMIN, Rol.SUPERVISOR));
    }

    public void guardarConversion(ConversionUnidad c, Usuario actor) {
        Producto p = db.consultar(cn -> productos.buscarPorId(cn, c.getIdProducto())).orElseThrow();
        if (p.getIdUnidad().equals(c.getIdUnidad())) {
            throw new ValidacionException("La unidad base del producto no necesita conversión.");
        }
        guardar(conversiones, c, actor);
    }

    /** Alta/edición de usuario. passwordNueva es obligatoria al crear; al editar, vacía = no cambiar. */
    public Usuario guardarUsuario(Usuario u, String passwordNueva, Usuario actor) {
        Permisos.requiere(actor, "administrar usuarios", Rol.ADMIN);
        u.validar();
        boolean cambiarPass = passwordNueva != null && !passwordNueva.isEmpty();
        if (u.esNuevo() && !cambiarPass) throw new ValidacionException("La contraseña es obligatoria para un usuario nuevo.");
        if (cambiarPass && (passwordNueva.length() < 6 || !passwordNueva.matches(".*\\d.*"))) {
            throw new ValidacionException("La contraseña debe tener al menos 6 caracteres e incluir un número.");
        }
        Rol rol = listar(roles).stream().filter(r -> r.getId().equals(u.getIdRol())).findFirst().orElseThrow();
        if (Rol.ENCARGADO.equals(rol.getNombre()) && u.getIdBodega() == null) {
            throw new ValidacionException("Un encargado debe tener una bodega asignada.");
        }
        if (u.getId() != null && u.getId().equals(actor.getId()) && !u.isActivo()) {
            throw new ValidacionException("No puede desactivar su propio usuario.");
        }
        return db.transaccion(cn -> {
            boolean nuevo = u.esNuevo();
            if (nuevo) u.setId(usuarios.insertar(cn, u));
            else usuarios.actualizar(cn, u);
            if (cambiarPass) usuarios.cambiarPassword(cn, u.getId(), PasswordUtil.hash(passwordNueva));
            auditoria.registrar(cn, actor.getId(), nuevo ? "CREAR" : "MODIFICAR", "Usuario",
                    u.getUsername() + " rol=" + rol.getNombre() + (cambiarPass ? " (contraseña actualizada)" : ""));
            return u;
        });
    }

    // DAO expuestos como "repositorios" para el CRUD genérico de la vista
    public CategoriaDao categorias() { return categorias; }
    public MarcaDao marcas() { return marcas; }
    public UnidadMedidaDao unidades() { return unidades; }
    public SucursalDao sucursales() { return sucursales; }
    public BodegaDao bodegas() { return bodegas; }
    public ClienteDao clientes() { return clientes; }
    public ProveedorDao proveedores() { return proveedores; }
    public ProductoDao productos() { return productos; }
    public ConversionUnidadDao conversiones() { return conversiones; }
    public UsuarioDao usuarios() { return usuarios; }
    public RolDao roles() { return roles; }
}
