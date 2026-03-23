ALTER TABLE registro_postor
    ADD COLUMN numero_tramite VARCHAR(40) NULL,
    ADD COLUMN verificacion_externa_estado VARCHAR(30) NULL,
    ADD COLUMN verificacion_externa_fuente VARCHAR(60) NULL,
    ADD COLUMN verificacion_externa_detalle VARCHAR(400) NULL;
