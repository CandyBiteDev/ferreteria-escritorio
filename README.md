# Ferretería e Inventario Multisucursal — Proyecto 05, Programación II (UMG)

Sistema de escritorio (Java Swing) + API REST + app Android (Java) para controlar productos,
existencias por bodega, compras, ventas/cotizaciones, devoluciones, transferencias y ajustes.

## 1. Cómo ejecutarlo

### Escritorio + API (IntelliJ)
1. MySQL 8 debe estar corriendo (usuario `root`).
2. **Configurar la contraseña de MySQL (obligatorio al descargar el proyecto):** copiar
   `src/main/resources/application-local.properties.example` como **`application-local.properties`**
   junto al `pom.xml` y cambiar el valor por la contraseña de tu MySQL. Ese archivo está en el
   `.gitignore` y nunca se sube. Si falta, al iniciar sesión aparece
   *"MySQL rechazó el usuario o la contraseña"*.
3. Abrir el proyecto en IntelliJ y ejecutar `FerreteriaApplication` (JDK 25).
4. **La primera vez** la app detecta que falta el esquema y ejecuta `sql/db_ferreteria.sql`
   (crea la base `db_ferreteria` con 22 tablas y datos de prueba). Después crea los usuarios
   y operaciones de demostración.
5. Para dejar la base como nueva antes de la defensa: agregar en *Run > Edit Configurations >
   Program arguments* el argumento `--reiniciar-bd`, ejecutar una vez y quitarlo.
6. Se abre el login. La ventana muestra la IP que hay que escribir en la app móvil.

Un solo proceso levanta la ventana Swing **y** la API en el puerto 8080.
Para correr solo la API: argumento `--sin-ventana`.

### App Android (Android Studio)
1. Abrir `AndroidStudioProjects/FerreteriaMovil` y ejecutar en el celular o el emulador.
2. En el login escribir como servidor la IP que muestra el login del escritorio, ej. `192.168.3.87:8080`.
   Sirve igual en el celular (misma red Wi-Fi que la PC) y en el emulador. Si no conecta, permitir
   Java en el Firewall de Windows para redes privadas.
3. Botón **Probar conexión** para verificar antes de ingresar.

## 2. Usuarios de prueba (datos ficticios)

| Usuario | Contraseña | Rol | Uso |
|---|---|---|---|
| `admin` | `Admin2026` | ADMIN | Todo, incluida administración de usuarios |
| `supervisor` | `Super2026` | SUPERVISOR | Autoriza ajustes, anula documentos |
| `vendedor` | `Vende2026` | VENDEDOR | Ventas, cotizaciones, devoluciones, clientes |
| `encargado.central` | `Bodega2026` | ENCARGADO (Bodega Principal) | Compras, transferencias, app móvil |
| `encargado.mixco` | `Bodega2026` | ENCARGADO (Bodega Mixco) | **App móvil**: tiene 1 transferencia por recibir |

Las contraseñas se guardan con hash **PBKDF2-HMAC-SHA256 + sal** (`seguridad/PasswordUtil`).

## 3. Arquitectura

```
com.umg.ferreteria
├── modelo/        Entidades (POO): EntidadBase, Persona, Documento<D>, DetalleDocumento...
├── dao/           Patrón DAO con JDBC puro (PreparedStatement). ConexionBD maneja commit/rollback
├── servicio/      Reglas de negocio y transacciones (Patrón FACADE)
├── seguridad/     Hash de contraseñas, SesionManager (Patrón SINGLETON), filtro de token
├── api/           Controladores REST que consume la app móvil (JSON)
├── excepcion/     Excepciones propias
└── vista/         Swing: login, ventana principal por rol, pantallas CRUD y reportes
```

**MVC:** Modelo = `modelo` · Vista = `vista` (Swing) y JSON · Controlador = `api` (REST) y `AppEscritorio`
(ciclo de sesión), que delegan en `servicio`.

### Conceptos de POO que pide la rúbrica
| Concepto | Dónde verlo |
|---|---|
| Clase abstracta | `EntidadBase`, `Persona`, `CatalogoSimple`, `Documento`, `DetalleDocumento`, `DaoBase`, `PanelCrud`, `PanelDocumentos`, `FerreteriaException` |
| Herencia | `Cliente`/`Proveedor` → `Persona`; `Compra`/`Venta`/`Transferencia` → `Documento`; `Categoria`/`Marca`/`UnidadMedida` → `CatalogoSimple` |
| Polimorfismo / sobrescritura | `validar()` en cada entidad; `getSubtotal()` en cada detalle; `getCodigoHttp()` en cada excepción; `mapear()`/`tabla()` en cada DAO; `Ui.error()` muestra un título distinto según el tipo de excepción |
| Composición | `Documento` es dueño de su `List<detalles>` (en BD: `ON DELETE CASCADE`) |
| Encapsulamiento | atributos privados/protegidos con getters/setters y validación |
| Genéricos | `Dao<T>`, `DaoBase<T>`, `PanelCrud<T>`, `Documento<D>` |
| Excepciones propias | `ReglaNegocioException`, `StockInsuficienteException`, `ValidacionException`, `AutenticacionException`, `AccesoDenegadoException`, `PersistenciaException` |

### Patrones
- **DAO** (`dao/`), **MVC**, **Singleton** (`SesionManager`, `ApiClient` en Android),
  **Facade** (`servicio/`), **Template Method** (`PanelCrud.inicializar()` + métodos abstractos).

## 4. Reglas de negocio y dónde se validan (en el servidor)
| Regla del enunciado | Implementación |
|---|---|
| No permitir stock negativo | `InventarioService.moverStock()` (con `SELECT ... FOR UPDATE`) + `CHECK (cantidad >= 0)` en la tabla |
| Transferencia: salida en origen y recepción en destino con estados separados | `enviarTransferencia()` → `EN_TRANSITO`; `recibirTransferencia()` → `RECIBIDA` (solo el encargado de la bodega destino) |
| No convertir cotización en venta si faltan existencias | `VentaService.convertirCotizacion()` → rollback completo |
| Ajustes con motivo y autorización | `InventarioService.registrarAjuste()`; en la vista se piden credenciales de supervisor si el usuario no puede autorizar |
| Unidades equivalentes solo con conversión definida | `VentaService.prepararDetalle()` + tabla `conversion_unidad` |

Toda operación encabezado-detalle o movimiento de inventario corre en `ConexionBD.transaccion()`:
`setAutoCommit(false)` → `commit()`, o `rollback()` ante cualquier error.

## 5. Reportes (pestaña Reportes, exportables a CSV)
Obligatorios: Rotación de inventario · Productos sin movimiento · Existencias por bodega ·
Rentabilidad estimada por categoría · Transferencias pendientes.
Adicionales: Bajo stock mínimo · Ventas por período y vendedor · Compras por proveedor ·
Devoluciones · Ajustes · Bitácora de auditoría.

## 6. Guion sugerido para la defensa
1. **Login** con `vendedor` → solo ve sus módulos (control de permisos).
2. **Ventas → seleccionar la cotización de "Constructora Los Álamos" (5 taladros) → Convertir en venta**:
   falla por stock insuficiente (*operación inválida por regla de negocio*).
3. **Convertir la otra cotización** (Remodelaciones Express) → éxito, baja el stock (*operación válida*).
4. **Existencias → Ajuste de inventario** como vendedor: pide usuario y contraseña de supervisor.
5. **Nueva venta** de 1 *Caja* de tornillos → descuenta 100 unidades (conversión de unidades).
6. **Falla controlada de persistencia**: Catálogos → Categorías → eliminar "Herramientas Manuales"
   → la BD lo impide por llave foránea y se muestra un mensaje claro.
7. **App móvil** con `encargado.mixco`: ver stock de Bodega Mixco, abrir *Transferencias por recibir*,
   confirmar. En el escritorio, *Transferencias* muestra la #1 como RECIBIDA y *Existencias* el nuevo stock
   (sincronización).
8. **Reportes**: generar los 5 obligatorios y exportar uno a CSV.
9. **Auditoría** (como admin o supervisor): reporte "Bitácora de auditoría" con todo lo anterior.

## 7. Pruebas automatizadas
`src/test/java/com/umg/ferreteria/ReglasNegocioTest.java`: 16 pruebas JUnit 5 de las reglas de negocio,
permisos, rollback y la falla controlada de persistencia (+1 de arranque). Corren contra la base
`db_ferreteria_test`, que se recrea en cada ejecución; `db_ferreteria` no se toca.
En IntelliJ: clic derecho en `src/test/java` › *Run 'All Tests'*, o en la terminal `mvnw test`.

## 8. Entregables
- Script SQL: `sql/db_ferreteria.sql` (22 tablas con PK/FK y datos de prueba).
- Respaldo: desde MySQL Workbench *Server > Data Export* de `db_ferreteria`.
- Código fuente: este proyecto + `FerreteriaMovil`.
