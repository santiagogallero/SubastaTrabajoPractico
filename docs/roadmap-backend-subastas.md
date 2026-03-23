# Backend roadmap - sistema de subastas

## Restricciones acordadas
- No modificar tablas base del profesor.
- Se pueden agregar tablas nuevas para funcionalidades adicionales.
- SMTP debe ser lo mas real posible para despliegue final.

## Entregable 1 (ya iniciado)
- Infraestructura por perfiles (`dev`, `test`, `prod`).
- `docker-compose` con MySQL y Mailpit para desarrollo local.
- Integracion SMTP configurable por variables de entorno.
- Flyway habilitado para migraciones versionadas.
- OpenAPI/Swagger habilitado.

## Entregable 2
- Seguridad JWT con roles: `ADMIN`, `EMPLEADO`, `POSTOR`, `DUENIO`, `SUBASTADOR`.
- Registro de usuario en 2 etapas (alta + completitud y medios de pago).
- Endpoints de autenticacion: login, refresh, logout.

## Entregable 3
- CRUD subastas, catalogos e items con validaciones.
- Reglas de moneda por subasta (`ARS` o `USD`, no bimonetaria).
- Restriccion de acceso por categoria del usuario vs categoria subasta.

## Entregable 4
- Motor de pujas con reglas:
  - Minimo: mejor oferta + 1% valor base.
  - Maximo: mejor oferta + 20% valor base.
  - Excepcion categorias `ORO` y `PLATINO`.
- Historial de pujas ordenado y transaccional.
- WebSocket STOMP para actualizaciones en tiempo real.

## Entregable 5
- Flujo de solicitud de inclusion de articulo por dueno.
- Aceptacion/rechazo de inspeccion y trazabilidad.
- Metricas basicas de participacion, ganadas e importes.

## Criterio de implementacion
- Mantener compatibilidad con esquema base existente.
- Nuevas tablas prefijadas para no interferir con las originales.
- Validar endpoints de cada entregable antes de avanzar.
