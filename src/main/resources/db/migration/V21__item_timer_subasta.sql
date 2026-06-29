-- Extiende subasta_config_ext para el timer por item
ALTER TABLE subasta_config_ext
    ADD COLUMN item_actual_id   INT          NULL,
    ADD COLUMN item_iniciado_at TIMESTAMP    NULL,
    ADD COLUMN duracion_item_minutos INT     NOT NULL DEFAULT 5,
    ADD CONSTRAINT fk_config_item_actual
        FOREIGN KEY (item_actual_id) REFERENCES itemsCatalogo(identificador);

-- duracion_minutos pasa a ser duración total (legacy); el nuevo campo es duracion_item_minutos
