-- Restore original schema: verificador NOT NULL in duenios
-- First assign first available empleado to any rows with null verificador (created by V13 seeder)
UPDATE duenios
SET verificador = (SELECT identificador FROM empleados ORDER BY identificador LIMIT 1)
WHERE verificador IS NULL;

ALTER TABLE duenios MODIFY COLUMN verificador INT NOT NULL;
