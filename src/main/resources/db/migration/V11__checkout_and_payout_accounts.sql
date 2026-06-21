-- Campos de checkout en pagos y cuentas de cobro del vendedor (consigna §12).

ALTER TABLE pago_subasta_ext
    ADD COLUMN monto_total DECIMAL(18,2) NULL,
    ADD COLUMN moneda VARCHAR(10) NULL,
    ADD COLUMN producto_descripcion VARCHAR(255) NULL,
    ADD COLUMN medio_pago_id BIGINT NULL,
    ADD COLUMN transaccion_id VARCHAR(64) NULL;

CREATE TABLE IF NOT EXISTS cuenta_cobro (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    duenio_id INT NOT NULL,
    alias VARCHAR(120) NOT NULL,
    moneda VARCHAR(10) NOT NULL,
    extranjera BOOLEAN NOT NULL DEFAULT FALSE,
    banco VARCHAR(120) NOT NULL,
    numero_cuenta VARCHAR(64) NOT NULL,
    swift_code VARCHAR(32) NULL,
    titular VARCHAR(120) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cuenta_cobro_duenio ON cuenta_cobro (duenio_id);
