-- Colecciones (consigna §11), campos de subasta y modalidad de entrega en adjudicaciones.

CREATE TABLE IF NOT EXISTS colecciones (
    identificador INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    duenio INT NOT NULL,
    subasta INT NOT NULL,
    fechaCreacion DATETIME NULL,
    CONSTRAINT fk_coleccion_duenio FOREIGN KEY (duenio) REFERENCES duenios(identificador),
    CONSTRAINT fk_coleccion_subasta FOREIGN KEY (subasta) REFERENCES subastas(identificador),
    CONSTRAINT uq_coleccion_subasta UNIQUE (subasta)
);

CREATE TABLE IF NOT EXISTS coleccion_productos (
    coleccion INT NOT NULL,
    producto INT NOT NULL,
    PRIMARY KEY (coleccion, producto),
    CONSTRAINT fk_cp_coleccion FOREIGN KEY (coleccion) REFERENCES colecciones(identificador) ON DELETE CASCADE,
    CONSTRAINT fk_cp_producto FOREIGN KEY (producto) REFERENCES productos(identificador)
);

ALTER TABLE subastas
    ADD COLUMN IF NOT EXISTS streamingUrl VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS depositoNombre VARCHAR(200) NULL,
    ADD COLUMN IF NOT EXISTS depositoDireccion VARCHAR(350) NULL;

ALTER TABLE registroDeSubasta
    ADD COLUMN IF NOT EXISTS modalidadEntrega VARCHAR(20) NULL,
    ADD COLUMN IF NOT EXISTS direccionEnvio VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS seguroVigenteTrasEntrega TINYINT(1) NULL DEFAULT 1;
