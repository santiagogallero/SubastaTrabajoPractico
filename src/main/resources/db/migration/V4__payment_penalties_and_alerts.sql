CREATE TABLE IF NOT EXISTS pago_subasta_ext (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    registro_subasta_id INT NOT NULL UNIQUE,
    usuario_auth_id BIGINT NOT NULL,
    fecha_vencimiento TIMESTAMP NOT NULL,
    estado_pago VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    monto_ofertado DECIMAL(18,2) NOT NULL,
    monto_multa DECIMAL(18,2) NOT NULL DEFAULT 0,
    multa_aplicada BOOLEAN NOT NULL DEFAULT FALSE,
    notificado_vencimiento BOOLEAN NOT NULL DEFAULT FALSE,
    notificado_mora BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_limite_regularizacion TIMESTAMP NULL,
    fecha_pago TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS bloqueo_participacion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    usuario_auth_id BIGINT NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    motivo VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
