SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'subasta_config_ext'
      AND COLUMN_NAME = 'duracion_minutos'
);

SET @ddl = IF(
    @col_exists = 0,
    'ALTER TABLE subasta_config_ext ADD COLUMN duracion_minutos INT NULL',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
