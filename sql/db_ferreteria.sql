-- =====================================================================
--  FERRETERÍA E INVENTARIO MULTISUCURSAL - Programación II (UMG)
--  Script de creación + datos de prueba (datos 100% ficticios)
--  Ejecutar completo en MySQL Workbench (Ctrl+Shift+Enter)
--  NOTA: los usuarios del sistema los crea la aplicación al arrancar
--        (contraseñas con hash PBKDF2), ver README.md
-- =====================================================================
DROP DATABASE IF EXISTS db_ferreteria;
CREATE DATABASE db_ferreteria CHARACTER SET utf8mb4 COLLATE utf8mb4_spanish_ci;
USE db_ferreteria;

-- ---------- Seguridad ----------
CREATE TABLE rol (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    nombre  VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(150)
);

CREATE TABLE sucursal (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    nombre    VARCHAR(80) NOT NULL UNIQUE,
    direccion VARCHAR(150),
    telefono  VARCHAR(20),
    activo    BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE bodega (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(80) NOT NULL,
    ubicacion   VARCHAR(150),
    id_sucursal INT NOT NULL,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_bodega_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursal(id),
    CONSTRAINT uq_bodega UNIQUE (id_sucursal, nombre)
);

CREATE TABLE usuario (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(40) NOT NULL UNIQUE,
    password_hash VARCHAR(200) NOT NULL,
    nombre        VARCHAR(100) NOT NULL,
    id_rol        INT NOT NULL,
    id_bodega     INT NULL,               -- bodega a cargo (rol ENCARGADO)
    activo        BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_usuario_rol    FOREIGN KEY (id_rol)    REFERENCES rol(id),
    CONSTRAINT fk_usuario_bodega FOREIGN KEY (id_bodega) REFERENCES bodega(id)
);

CREATE TABLE auditoria (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    fecha      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario INT NULL,
    accion     VARCHAR(40) NOT NULL,
    entidad    VARCHAR(40) NOT NULL,
    detalle    VARCHAR(500),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id)
);

-- ---------- Catálogos ----------
CREATE TABLE categoria (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE marca (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE unidad_medida (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(40) NOT NULL UNIQUE,
    abreviatura VARCHAR(10) NOT NULL
);

CREATE TABLE producto (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    codigo        VARCHAR(30)  NOT NULL UNIQUE,
    nombre        VARCHAR(120) NOT NULL,
    descripcion   VARCHAR(250),
    precio_costo  DECIMAL(10,2) NOT NULL CHECK (precio_costo >= 0),
    precio_venta  DECIMAL(10,2) NOT NULL CHECK (precio_venta >= 0),
    stock_minimo  INT NOT NULL DEFAULT 0 CHECK (stock_minimo >= 0),
    id_categoria  INT NOT NULL,
    id_marca      INT NOT NULL,
    id_unidad     INT NOT NULL,           -- unidad base de inventario
    activo        BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES categoria(id),
    CONSTRAINT fk_producto_marca     FOREIGN KEY (id_marca)     REFERENCES marca(id),
    CONSTRAINT fk_producto_unidad    FOREIGN KEY (id_unidad)    REFERENCES unidad_medida(id)
);

-- 1 [id_unidad] = factor unidades base del producto (ej. 1 Caja = 100 Unidades)
CREATE TABLE conversion_unidad (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    id_producto INT NOT NULL,
    id_unidad   INT NOT NULL,
    factor      DECIMAL(10,3) NOT NULL CHECK (factor > 0),
    CONSTRAINT fk_conv_producto FOREIGN KEY (id_producto) REFERENCES producto(id) ON DELETE CASCADE,
    CONSTRAINT fk_conv_unidad   FOREIGN KEY (id_unidad)   REFERENCES unidad_medida(id),
    CONSTRAINT uq_conversion UNIQUE (id_producto, id_unidad)
);

CREATE TABLE existencia (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    id_producto INT NOT NULL,
    id_bodega   INT NOT NULL,
    cantidad    DECIMAL(12,3) NOT NULL DEFAULT 0,
    CONSTRAINT chk_stock_no_negativo CHECK (cantidad >= 0),     -- regla: no stock negativo
    CONSTRAINT fk_exist_producto FOREIGN KEY (id_producto) REFERENCES producto(id),
    CONSTRAINT fk_exist_bodega   FOREIGN KEY (id_bodega)   REFERENCES bodega(id),
    CONSTRAINT uq_existencia UNIQUE (id_producto, id_bodega)
);

-- ---------- Terceros ----------
CREATE TABLE proveedor (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    nombre    VARCHAR(120) NOT NULL,
    nit       VARCHAR(20) NOT NULL UNIQUE,
    telefono  VARCHAR(20),
    correo    VARCHAR(100),
    direccion VARCHAR(150),
    contacto  VARCHAR(100)
);

CREATE TABLE cliente (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    nombre    VARCHAR(120) NOT NULL,
    nit       VARCHAR(20) NOT NULL UNIQUE,
    telefono  VARCHAR(20),
    correo    VARCHAR(100),
    direccion VARCHAR(150)
);

-- ---------- Compras ----------
CREATE TABLE compra (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    fecha         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_proveedor  INT NOT NULL,
    id_bodega     INT NOT NULL,
    id_usuario    INT NOT NULL,
    estado        VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',   -- PENDIENTE / RECIBIDA / ANULADA
    fecha_recepcion DATETIME NULL,
    total         DECIMAL(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_compra_proveedor FOREIGN KEY (id_proveedor) REFERENCES proveedor(id),
    CONSTRAINT fk_compra_bodega    FOREIGN KEY (id_bodega)    REFERENCES bodega(id),
    CONSTRAINT fk_compra_usuario   FOREIGN KEY (id_usuario)   REFERENCES usuario(id)
);

CREATE TABLE detalle_compra (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    id_compra   INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad    DECIMAL(12,3) NOT NULL CHECK (cantidad > 0),
    costo_unitario DECIMAL(10,2) NOT NULL CHECK (costo_unitario >= 0),
    CONSTRAINT fk_dcompra_compra   FOREIGN KEY (id_compra)   REFERENCES compra(id) ON DELETE CASCADE,
    CONSTRAINT fk_dcompra_producto FOREIGN KEY (id_producto) REFERENCES producto(id)
);

-- ---------- Ventas y cotizaciones ----------
CREATE TABLE venta (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    fecha       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_cliente  INT NOT NULL,
    id_bodega   INT NOT NULL,
    id_usuario  INT NOT NULL,
    estado      VARCHAR(15) NOT NULL,     -- COTIZACION / CONFIRMADA / ANULADA
    total       DECIMAL(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_venta_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id),
    CONSTRAINT fk_venta_bodega  FOREIGN KEY (id_bodega)  REFERENCES bodega(id),
    CONSTRAINT fk_venta_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id)
);

CREATE TABLE detalle_venta (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    id_venta        INT NOT NULL,
    id_producto     INT NOT NULL,
    id_unidad       INT NOT NULL,                 -- unidad en la que se vende
    cantidad        DECIMAL(12,3) NOT NULL CHECK (cantidad > 0),
    cantidad_base   DECIMAL(12,3) NOT NULL,       -- convertida a unidad base
    precio_unitario DECIMAL(10,2) NOT NULL,
    costo_unitario  DECIMAL(10,2) NOT NULL,       -- costo base al momento (rentabilidad)
    CONSTRAINT fk_dventa_venta    FOREIGN KEY (id_venta)    REFERENCES venta(id) ON DELETE CASCADE,
    CONSTRAINT fk_dventa_producto FOREIGN KEY (id_producto) REFERENCES producto(id),
    CONSTRAINT fk_dventa_unidad   FOREIGN KEY (id_unidad)   REFERENCES unidad_medida(id)
);

CREATE TABLE devolucion (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    fecha       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_venta    INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad_base DECIMAL(12,3) NOT NULL CHECK (cantidad_base > 0),
    motivo      VARCHAR(200) NOT NULL,
    id_usuario  INT NOT NULL,
    CONSTRAINT fk_dev_venta    FOREIGN KEY (id_venta)    REFERENCES venta(id),
    CONSTRAINT fk_dev_producto FOREIGN KEY (id_producto) REFERENCES producto(id),
    CONSTRAINT fk_dev_usuario  FOREIGN KEY (id_usuario)  REFERENCES usuario(id)
);

-- ---------- Transferencias ----------
CREATE TABLE transferencia (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    fecha_envio       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_recepcion   DATETIME NULL,
    id_bodega_origen  INT NOT NULL,
    id_bodega_destino INT NOT NULL,
    id_usuario_envia  INT NOT NULL,
    id_usuario_recibe INT NULL,
    estado            VARCHAR(15) NOT NULL DEFAULT 'EN_TRANSITO',  -- EN_TRANSITO / RECIBIDA / ANULADA
    observacion       VARCHAR(250),
    CONSTRAINT chk_bodegas_distintas CHECK (id_bodega_origen <> id_bodega_destino),
    CONSTRAINT fk_trf_origen   FOREIGN KEY (id_bodega_origen)  REFERENCES bodega(id),
    CONSTRAINT fk_trf_destino  FOREIGN KEY (id_bodega_destino) REFERENCES bodega(id),
    CONSTRAINT fk_trf_envia    FOREIGN KEY (id_usuario_envia)  REFERENCES usuario(id),
    CONSTRAINT fk_trf_recibe   FOREIGN KEY (id_usuario_recibe) REFERENCES usuario(id)
);

CREATE TABLE detalle_transferencia (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    id_transferencia INT NOT NULL,
    id_producto      INT NOT NULL,
    cantidad         DECIMAL(12,3) NOT NULL CHECK (cantidad > 0),
    CONSTRAINT fk_dtrf_trf      FOREIGN KEY (id_transferencia) REFERENCES transferencia(id) ON DELETE CASCADE,
    CONSTRAINT fk_dtrf_producto FOREIGN KEY (id_producto)      REFERENCES producto(id)
);

-- ---------- Ajustes y kardex ----------
CREATE TABLE ajuste_inventario (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    fecha                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_producto          INT NOT NULL,
    id_bodega            INT NOT NULL,
    cantidad             DECIMAL(12,3) NOT NULL,     -- positiva = entrada, negativa = salida
    motivo               VARCHAR(200) NOT NULL,
    id_usuario_solicita  INT NOT NULL,
    id_usuario_autoriza  INT NOT NULL,
    CONSTRAINT fk_aj_producto  FOREIGN KEY (id_producto)         REFERENCES producto(id),
    CONSTRAINT fk_aj_bodega    FOREIGN KEY (id_bodega)           REFERENCES bodega(id),
    CONSTRAINT fk_aj_solicita  FOREIGN KEY (id_usuario_solicita) REFERENCES usuario(id),
    CONSTRAINT fk_aj_autoriza  FOREIGN KEY (id_usuario_autoriza) REFERENCES usuario(id)
);

-- Cada cambio de existencia queda registrado (kardex). Base de rotación y "sin movimiento".
CREATE TABLE movimiento_inventario (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    fecha       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_producto INT NOT NULL,
    id_bodega   INT NOT NULL,
    tipo        VARCHAR(20) NOT NULL,   -- COMPRA, VENTA, DEVOLUCION, TRF_SALIDA, TRF_ENTRADA, AJUSTE
    cantidad    DECIMAL(12,3) NOT NULL, -- con signo
    saldo       DECIMAL(12,3) NOT NULL,
    referencia  VARCHAR(50),
    CONSTRAINT fk_mov_producto FOREIGN KEY (id_producto) REFERENCES producto(id),
    CONSTRAINT fk_mov_bodega   FOREIGN KEY (id_bodega)   REFERENCES bodega(id)
);
CREATE INDEX ix_mov_fecha ON movimiento_inventario(fecha);

-- =====================================================================
--  DATOS DE PRUEBA (ficticios)
-- =====================================================================
INSERT INTO rol (nombre, descripcion) VALUES
 ('ADMIN',      'Acceso total, administra usuarios'),
 ('SUPERVISOR', 'Autoriza ajustes y anulaciones'),
 ('VENDEDOR',   'Ventas, cotizaciones y clientes'),
 ('ENCARGADO',  'Encargado de bodega: recepciones y transferencias');

INSERT INTO sucursal (nombre, direccion, telefono) VALUES
 ('Central Zona 1',   '6a Avenida 10-20, Zona 1, Guatemala', '2222-1000'),
 ('Mixco',            'Calzada San Juan 15-40, Mixco',       '2433-2000'),
 ('Villa Nueva',      'Km 17.5 Carretera al Pacífico',       '6630-3000');

INSERT INTO bodega (nombre, ubicacion, id_sucursal) VALUES
 ('Bodega Principal', 'Sótano edificio central', 1),
 ('Bodega Mostrador', 'Área de ventas',          1),
 ('Bodega Mixco',     'Patio trasero',           2),
 ('Bodega Villa Nueva','Galera 3',               3);

INSERT INTO categoria (nombre) VALUES
 ('Herramientas Manuales'), ('Herramientas Eléctricas'), ('Materiales de Construcción'),
 ('Plomería'), ('Electricidad'), ('Pinturas'), ('Tornillería');

INSERT INTO marca (nombre) VALUES
 ('Truper'), ('Stanley'), ('DeWalt'), ('Cementos Progreso'), ('Tubovinil'), ('Sherwin-Williams'), ('Genérica');

INSERT INTO unidad_medida (nombre, abreviatura) VALUES
 ('Unidad','u'), ('Caja','cja'), ('Saco','sco'), ('Metro','m'), ('Rollo','rll'), ('Galón','gal'), ('Cubeta','cub');

INSERT INTO producto (codigo, nombre, descripcion, precio_costo, precio_venta, stock_minimo, id_categoria, id_marca, id_unidad) VALUES
 ('HM-001','Martillo de uña 16 oz','Mango de fibra de vidrio', 45.00, 75.00, 10, 1, 1, 1),
 ('HM-002','Desarmador Phillips #2','Punta magnética',          12.00, 22.00, 20, 1, 2, 1),
 ('HM-003','Cinta métrica 5 m','Carcasa de goma',               28.00, 49.00, 10, 1, 2, 1),
 ('HE-001','Taladro percutor 1/2"','750 W',                    480.00,799.00,  3, 2, 3, 1),
 ('HE-002','Esmeriladora angular 4 1/2"','850 W',              390.00,650.00,  3, 2, 3, 1),
 ('MC-001','Cemento gris 42.5 kg','Uso general',                72.00, 89.00, 50, 3, 4, 3),
 ('PL-001','Tubo PVC 1/2" x 6 m','160 PSI',                     18.00, 32.00, 40, 4, 5, 1),
 ('PL-002','Codo PVC 1/2" 90°','',                               1.50,  3.50,100, 4, 5, 1),
 ('EL-001','Cable THHN #12','Se vende por metro, rollo de 100 m', 4.50,  7.50,200, 5, 7, 4),
 ('PI-001','Pintura látex blanco','Interior/exterior',          95.00,155.00, 10, 6, 6, 6),
 ('TO-001','Tornillo drywall 6x1"','Punta fina',                 0.15,  0.35,500, 7, 7, 1),
 ('TO-002','Clavo 2 1/2"','Acero',                               0.10,  0.25,500, 7, 7, 1);

-- Conversiones: venta en unidades equivalentes
INSERT INTO conversion_unidad (id_producto, id_unidad, factor) VALUES
 (11, 2, 100),   -- 1 Caja de tornillos = 100 unidades
 (12, 2, 200),   -- 1 Caja de clavos    = 200 unidades
 ( 9, 5, 100),   -- 1 Rollo de cable    = 100 metros
 (10, 7, 5),     -- 1 Cubeta de pintura = 5 galones
 ( 8, 2, 50);    -- 1 Caja de codos     = 50 unidades

INSERT INTO proveedor (nombre, nit, telefono, correo, direccion, contacto) VALUES
 ('Distribuidora Ferromax S.A.', '1234567-8', '2345-6789', 'ventas@ferromax.test', 'Zona 12, Guatemala', 'Luis Pérez'),
 ('Importadora El Tornillo',     '7654321-0', '2456-7890', 'pedidos@eltornillo.test', 'Zona 9, Guatemala', 'Ana López'),
 ('Materiales del Sur',          '5556667-1', '6630-1111', 'info@matsur.test', 'Escuintla', 'Carlos Ruiz');

INSERT INTO cliente (nombre, nit, telefono, correo, direccion) VALUES
 ('Consumidor Final',             'CF',         '',          '',                     'Ciudad'),
 ('Constructora Los Álamos',      '9988776-5', '2200-3344', 'compras@alamos.test',  'Zona 10, Guatemala'),
 ('Juan Carlos Méndez',           '4455667-2', '5511-2233', 'jc.mendez@correo.test', 'Mixco'),
 ('Remodelaciones Express',       '3322110-9', '4433-2211', 'info@remexpress.test', 'Villa Nueva');

-- Existencias iniciales (el kardex se llena con un movimiento AJUSTE de "Inventario inicial")
INSERT INTO existencia (id_producto, id_bodega, cantidad) VALUES
 (1,1,40),(1,2,8),(1,3,15),
 (2,1,100),(2,2,30),(2,4,25),
 (3,1,30),(3,3,12),
 (4,1,6),(4,2,2),
 (5,1,5),
 (6,1,300),(6,3,120),(6,4,80),
 (7,1,150),(7,4,60),
 (8,1,800),(8,3,200),
 (9,1,1500),(9,2,250),
 (10,1,25),(10,3,8),
 (11,1,5000),(11,2,600),
 (12,1,8000);
-- Producto 12 (clavos) y otros quedan sin movimientos recientes a propósito para el reporte "sin movimiento"

INSERT INTO movimiento_inventario (fecha, id_producto, id_bodega, tipo, cantidad, saldo, referencia)
SELECT DATE_SUB(NOW(), INTERVAL 120 DAY), id_producto, id_bodega, 'AJUSTE', cantidad, cantidad, 'Inventario inicial'
FROM existencia;
