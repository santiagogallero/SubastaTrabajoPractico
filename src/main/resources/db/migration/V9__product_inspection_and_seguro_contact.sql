-- Campos del flujo de venta/inspeccion de articulos propios y contacto de aseguradora.
-- Las tablas de dominio (productos, seguros) pueden existir via Hibernate; esta migracion
-- documenta el esquema esperado en entornos donde Flyway es la fuente de verdad.

ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS titulo VARCHAR(200) NULL,
    ADD COLUMN IF NOT EXISTS categoria VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS historia VARCHAR(2000) NULL,
    ADD COLUMN IF NOT EXISTS estadoInspeccion VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    ADD COLUMN IF NOT EXISTS motivoRechazo VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS fechaInspeccion DATETIME(6) NULL,
    ADD COLUMN IF NOT EXISTS revisor INT NULL,
    ADD COLUMN IF NOT EXISTS declaraPropiedad TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS origenLicit TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE seguros
    ADD COLUMN IF NOT EXISTS contactoTelefono VARCHAR(40) NULL,
    ADD COLUMN IF NOT EXISTS contactoEmail VARCHAR(150) NULL;
