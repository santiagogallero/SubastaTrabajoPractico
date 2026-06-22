ALTER TABLE verificacion_chat_conversacion
    ADD COLUMN producto_id INT NULL,
    ADD CONSTRAINT fk_chat_conv_producto FOREIGN KEY (producto_id) REFERENCES productos(identificador);
