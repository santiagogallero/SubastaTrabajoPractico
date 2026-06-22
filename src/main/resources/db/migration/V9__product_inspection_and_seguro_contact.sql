-- Campos del flujo de venta/inspeccion de articulos propios y contacto de aseguradora.
-- Las tablas de dominio (productos, seguros) pueden existir via Hibernate; esta migracion
-- documenta el esquema esperado en entornos donde Flyway es la fuente de verdad.

ALTER TABLE productos
    ADD COLUMN titulo VARCHAR(200) NULL,
    ADD COLUMN categoria VARCHAR(100) NULL,
    ADD COLUMN historia VARCHAR(2000) NULL,
    ADD COLUMN estadoInspeccion VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    ADD COLUMN motivoRechazo VARCHAR(500) NULL,
    ADD COLUMN fechaInspeccion DATETIME(6) NULL,
    ADD COLUMN revisor INT NULL,
    ADD COLUMN declaraPropiedad TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN origenLicit TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE seguros
    ADD COLUMN contactoTelefono VARCHAR(40) NULL,
    ADD COLUMN contactoEmail VARCHAR(150) NULL;
