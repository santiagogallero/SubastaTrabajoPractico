-- =============================================================================
-- SEED DE DATOS DE PRUEBA - Sistema de Subastas
-- =============================================================================
-- Este migration inserta datos mínimos para poder probar el frontend
-- sin depender de un backend mock.
-- 
-- Credenciales de prueba:
--   postor@auction.com / Postor123!  (rol: POSTOR, categoría: PLATA)
--   admin@auction.com  / Admin123!   (rol: ADMIN)
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. USUARIOS AUTH (passwords hasheados con BCrypt, rounds 10)
--    Postor123! -> $2b$10$qAcpwpEDhNPXROGd1trqU.eNNpwu6d7YCjGXLQ5O1HRUhoVeQ6sMC
--    Admin123!  -> $2b$10$ZlW8vX3/iIkJM3.Ro/ziKOMXpQEfUuKON1EKsKisItJEJpoFQJxeC
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO usuario_auth (id, email, password_hash, estado, persona_id, created_at, updated_at)
VALUES
  (1, 'admin@auction.com',  '$2b$10$ZlW8vX3/iIkJM3.Ro/ziKOMXpQEfUuKON1EKsKisItJEJpoFQJxeC', 'ACTIVO', NULL, NOW(), NOW()),
  (2, 'postor@auction.com', '$2b$10$qAcpwpEDhNPXROGd1trqU.eNNpwu6d7YCjGXLQ5O1HRUhoVeQ6sMC', 'ACTIVO', NULL, NOW(), NOW());

-- ---------------------------------------------------------------------------
-- 2. ROLES
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id
FROM usuario_auth u
JOIN rol r ON (
  (u.email = 'admin@auction.com'  AND r.nombre = 'ADMIN') OR
  (u.email = 'postor@auction.com' AND r.nombre = 'POSTOR')
);

-- ---------------------------------------------------------------------------
-- 3. REGISTRO POSTOR (etapa completada, categoría asignada)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO registro_postor (id, usuario_id, etapa, categoria, doc_frente_url, doc_dorso_url, domicilio_legal, pais_origen, numero_tramite, verificado_por, verificado_at, motivo_rechazo)
VALUES
  (1, 2, 'COMPLETADO', 'PLATA', 'https://example.com/doc-frente.jpg', 'https://example.com/doc-dorso.jpg', 'Calle Falsa 123', 'AR', '00123456789', 'admin@auction.com', NOW(), NULL);

-- ---------------------------------------------------------------------------
-- 4. MEDIOS DE PAGO (verificados, para que el postor pueda pujar)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO medio_pago (id, usuario_id, tipo, alias_descripcion, moneda, monto_garantia, verificado, activo)
VALUES
  (1, 2, 'CREDIT_CARD', 'Visa terminada en 4242', 'ARS', NULL, TRUE, TRUE),
  (2, 2, 'BANK_ACCOUNT', 'Cuenta Chase terminada en 7856', 'USD', NULL, TRUE, TRUE),
  (3, 2, 'CERTIFIED_CHECK', 'Cheque certificado Banco Nación', 'ARS', 500000.00, TRUE, TRUE);

-- ---------------------------------------------------------------------------
-- 5. SUBASTAS
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO subastas (identificador, fecha, hora, estado, subastador, ubicacion, capacidad_asistentes, tiene_deposito, seguridad_propia, categoria)
VALUES
  (1, '2026-06-15', '10:00:00', 'ACTIVA', NULL, 'Salón Principal - Buenos Aires', 150, 'SI', 'NO', 'Oro'),
  (2, '2026-06-18', '14:30:00', 'PENDIENTE', NULL, 'Sede Córdoba - Centro de Convenciones', 80, 'SI', 'SI', 'Plata'),
  (3, '2026-06-20', '11:00:00', 'ACTIVA', NULL, 'Online - Streaming', 500, 'NO', 'NO', 'Común'),
  (4, '2026-06-25', '09:00:00', 'PENDIENTE', NULL, 'Salón VIP - Mendoza', 50, 'SI', 'SI', 'Platino'),
  (5, '2026-06-22', '16:00:00', 'CERRADA', NULL, 'Rosario - Centro Cultural', 120, 'SI', 'NO', 'Oro');

-- ---------------------------------------------------------------------------
-- 6. CONFIGURACIÓN DE MONEDA POR SUBASTA (monomoneda)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO subasta_config_ext (subasta_id, moneda)
VALUES
  (1, 'ARS'),
  (2, 'USD'),
  (3, 'ARS'),
  (4, 'USD'),
  (5, 'ARS');

-- ---------------------------------------------------------------------------
-- 7. PAGOS / COMPLIANCE (estados variados para testing)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO pago_subasta_ext (id, registro_subasta_id, usuario_auth_id, fecha_vencimiento, estado_pago, monto_ofertado, monto_multa, multa_aplicada, fecha_limite_regularizacion, fecha_pago)
VALUES
  (1, 101, 2, '2026-06-20 23:59:59', 'PENDIENTE', 150000.00, 0.00, FALSE, '2026-06-23 23:59:59', NULL),
  (2, 102, 2, '2026-05-15 23:59:59', 'PAGADO',    85000.00,  0.00, FALSE, NULL,                   NOW()),
  (3, 103, 2, '2026-04-10 23:59:59', 'VENCIDO',   320000.00, 32000.00, TRUE,  '2026-04-13 23:59:59', NULL),
  (4, 104, 2, '2026-07-01 23:59:59', 'PENDIENTE', 45000.00,  0.00, FALSE, '2026-07-04 23:59:59', NULL);

-- ---------------------------------------------------------------------------
-- NOTA: Las tablas de productos, catálogos, items, personas, empleados,
-- dueños y subastadores requieren relaciones complejas (FKs obligatorias).
-- Se recomienda usar el DataSeeder Java (TestDataSeeder) para poblar
-- esas tablas, o probar el frontend con el mock-server incluido.
-- ---------------------------------------------------------------------------
