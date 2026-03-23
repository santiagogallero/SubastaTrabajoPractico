CREATE TABLE IF NOT EXISTS rol (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS usuario_auth (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    persona_id INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS usuario_rol (
    usuario_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT fk_usuario_rol_usuario FOREIGN KEY (usuario_id) REFERENCES usuario_auth(id),
    CONSTRAINT fk_usuario_rol_rol FOREIGN KEY (rol_id) REFERENCES rol(id)
);

CREATE TABLE IF NOT EXISTS registro_postor (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    etapa VARCHAR(30) NOT NULL,
    categoria VARCHAR(30) NULL,
    doc_frente_url VARCHAR(300) NULL,
    doc_dorso_url VARCHAR(300) NULL,
    domicilio_legal VARCHAR(255) NULL,
    pais_origen VARCHAR(120) NULL,
    verificado_por VARCHAR(120) NULL,
    verificado_at TIMESTAMP NULL,
    motivo_rechazo VARCHAR(400) NULL,
    CONSTRAINT fk_registro_postor_usuario FOREIGN KEY (usuario_id) REFERENCES usuario_auth(id)
);

CREATE TABLE IF NOT EXISTS medio_pago (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    alias_descripcion VARCHAR(120) NOT NULL,
    moneda VARCHAR(10) NOT NULL,
    monto_garantia DECIMAL(18,2) NULL,
    verificado BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_medio_pago_usuario FOREIGN KEY (usuario_id) REFERENCES usuario_auth(id)
);

INSERT IGNORE INTO rol (nombre) VALUES
('ADMIN'),
('EMPLEADO'),
('POSTOR'),
('DUENIO'),
('SUBASTADOR');
