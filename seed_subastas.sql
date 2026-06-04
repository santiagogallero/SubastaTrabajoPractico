-- Script de seed manual para la tabla `subastas` (JPA-managed, no puede ir en Flyway).
-- Ejecutar UNA VEZ después de que el backend haya levantado y Hibernate haya creado la tabla.
--
-- Uso: docker exec -i auction-mysql mysql --default-character-set=utf8mb4 -u root -proot_pass auctiondb < seed_subastas.sql
--      IMPORTANTE: usar --default-character-set=utf8mb4 o los acentos se guardan doble-encodificados.
--      (o desde el directorio SubastaTrabajoPractico/)

INSERT IGNORE INTO subastas (identificador, fecha, hora, estado, subastador, ubicacion, capacidad_asistentes, tiene_deposito, seguridad_propia, categoria)
VALUES
  (1, '2026-06-10', '10:00:00', 'ACTIVA',    NULL, 'Salón Principal - Buenos Aires',         150, 'SI', 'NO', 'Oro'),
  (2, '2026-06-12', '14:30:00', 'ACTIVA',    NULL, 'Sede Córdoba - Centro de Convenciones',   80, 'SI', 'SI', 'Plata'),
  (3, '2026-06-20', '11:00:00', 'PENDIENTE', NULL, 'Palacio San Martín - Buenos Aires',      200, 'SI', 'SI', 'Platino'),
  (4, '2026-06-25', '09:00:00', 'PENDIENTE', NULL, 'Salón de Exposiciones - Mendoza',         50, 'SI', 'SI', 'Común'),
  (5, '2026-06-28', '16:00:00', 'PENDIENTE', NULL, 'Centro Cultural Rosario',                120, 'SI', 'NO', 'Plata');
