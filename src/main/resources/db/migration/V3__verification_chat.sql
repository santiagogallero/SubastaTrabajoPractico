CREATE TABLE IF NOT EXISTS verificacion_chat_conversacion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    duenio_usuario_id BIGINT NOT NULL,
    empleado_usuario_id BIGINT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTA',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_conv_duenio FOREIGN KEY (duenio_usuario_id) REFERENCES usuario_auth(id),
    CONSTRAINT fk_chat_conv_empleado FOREIGN KEY (empleado_usuario_id) REFERENCES usuario_auth(id)
);

CREATE TABLE IF NOT EXISTS verificacion_chat_mensaje (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversacion_id BIGINT NOT NULL,
    remitente_usuario_id BIGINT NOT NULL,
    texto VARCHAR(2000) NOT NULL,
    enviado_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_msg_conv FOREIGN KEY (conversacion_id) REFERENCES verificacion_chat_conversacion(id),
    CONSTRAINT fk_chat_msg_remitente FOREIGN KEY (remitente_usuario_id) REFERENCES usuario_auth(id)
);
