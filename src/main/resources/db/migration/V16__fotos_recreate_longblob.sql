DROP TABLE IF EXISTS fotos;
CREATE TABLE fotos (
    identificador INT NOT NULL AUTO_INCREMENT,
    foto          LONGBLOB NOT NULL,
    producto      INT NOT NULL,
    PRIMARY KEY (identificador),
    KEY fk_fotos_producto (producto)
);
