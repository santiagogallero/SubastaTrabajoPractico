# Primera entrega - API REST (resumen para documentacion)

## 1) Criterio general de codigos HTTP

- 200 OK: operacion exitosa.
- 400 Bad Request: validacion de datos o regla de negocio incumplida.
- 401 Unauthorized: no autenticado o token invalido.
- 403 Forbidden: autenticado pero sin permisos, o usuario no habilitado.
- 404 Not Found: recurso inexistente (solo en endpoints que buscan por id).
- 500 Internal Server Error: error no controlado.

Nota: Los codigos siguen el catalogo IANA:
https://www.iana.org/assignments/http-status-codes/http-status-codes.xhtml

## 2) Endpoints de autenticacion y registro

### POST /api/auth/register/stage1
Registra datos base del usuario.

Request body:
{
  "email": "user@mail.com",
  "password": "123456",
  "documento": "12345678",
  "nombre": "Juan Perez",
  "domicilioLegal": "Calle 123",
  "paisOrigen": "Argentina"
}

Responses:
- 200: "Registro etapa 1 completado"
- 400: datos invalidos, usuario ya registrado, documento ya existente

### POST /api/auth/register/stage2
Carga documentacion e identidad (etapa documental).

Request body:
{
  "email": "user@mail.com",
  "request": {
    "numeroTramite": "1234567890",
    "docFrenteUrl": "https://...",
    "docDorsoUrl": "https://..."
  }
}

Responses:
- 200: "Registro etapa 2 completado"
- 400: usuario inexistente, etapa 1 faltante, validacion externa fallida

### POST /api/auth/email/send-code
Reenvia codigo de verificacion por correo.

Request body:
{
  "email": "user@mail.com"
}

Responses:
- 200: "Codigo enviado"
- 400: usuario inexistente o estado no compatible

### POST /api/auth/email/verify-code
Valida codigo recibido por correo.

Request body:
{
  "email": "user@mail.com",
  "code": "123456"
}

Responses:
- 200: "Correo verificado. Tu cuenta queda en revision."
- 400: codigo invalido, expirado, ya usado, o usuario en estado incorrecto

### POST /api/auth/login
Autentica usuario y devuelve JWT.

Request body:
{
  "email": "user@mail.com",
  "password": "123456"
}

Response body (200):
{
  "token": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "roles": ["POSTOR"]
}

Responses:
- 200: login exitoso
- 401: credenciales invalidas
- 403: usuario pendiente de aprobacion/habilitacion

### GET /api/auth/me
Devuelve datos del usuario autenticado (user auth).

Response body (200):
{
  "id": 12,
  "email": "user@mail.com",
  "estado": "ACTIVO",
  "personaId": 34,
  "roles": ["POSTOR"]
}

Responses:
- 200: usuario autenticado
- 401: sin token o token invalido

### POST /api/auth/payment-methods
Registra medios de pago luego de login (requiere token).

Request body:
{
  "mediosPago": [
    {
      "tipo": "TARJETA",
      "aliasDescripcion": "Visa BBVA",
      "moneda": "ARS",
      "montoGarantia": 10000
    }
  ]
}

Responses:
- 200: "Medios de pago registrados"
- 400: usuario no encontrado o no habilitado para registrar medios
- 401: sin token o token invalido

### POST /api/auth/admin/aprobar
Aprobacion administrativa de cuenta (ADMIN/EMPLEADO).

Request body:
{
  "email": "user@mail.com",
  "categoria": "COMUN"
}

Responses:
- 200: "Usuario aprobado"
- 400: usuario o registro no encontrado
- 401: no autenticado
- 403: sin rol suficiente

### POST /api/auth/admin/verificar-medio-pago
Verifica un medio de pago (ADMIN/EMPLEADO).

Request body:
{
  "medioPagoId": 1,
  "verificado": true
}

Responses:
- 200: "Medio de pago actualizado"
- 400: medio de pago inexistente
- 401: no autenticado
- 403: sin rol suficiente

## 3) Endpoints funcionales del sistema

### GET /api/health
Healthcheck de backend.

Response body (200):
{
  "status": "UP"
}

Responses:
- 200: backend activo

### POST /api/notifications/test-email
Envia un correo de prueba.

Request body:
{
  "to": "destino@mail.com"
}

Responses:
- 200: "Email sent"
- 400: email invalido

## 4) CRUD base (personas, productos, subastas)

### Personas

### GET /api/personas
Lista todas las personas.

Responses:
- 200: lista de personas

### GET /api/personas/{id}
Obtiene una persona por id.

Responses:
- 200: persona encontrada
- 404: persona no encontrada

### POST /api/personas
Crea una persona.

Request body:
{
  "...": "campos de Persona"
}

Responses:
- 200: persona creada

### PUT /api/personas/{id}
Actualiza una persona existente.

Request body:
{
  "...": "campos de Persona"
}

Responses:
- 200: persona actualizada
- 404: persona no encontrada

### DELETE /api/personas/{id}
Elimina una persona.

Responses:
- 204: persona eliminada
- 404: persona no encontrada

### Productos

### GET /api/productos
Lista todos los productos.

Responses:
- 200: lista de productos

### GET /api/productos/{id}
Obtiene un producto por id.

Responses:
- 200: producto encontrado
- 404: producto no encontrado

### POST /api/productos
Crea un producto.

Request body:
{
  "...": "campos de Producto"
}

Responses:
- 200: producto creado

### PUT /api/productos/{id}
Actualiza un producto existente.

Request body:
{
  "...": "campos de Producto"
}

Responses:
- 200: producto actualizado
- 404: producto no encontrado

### DELETE /api/productos/{id}
Elimina un producto.

Responses:
- 204: producto eliminado
- 404: producto no encontrado

### Subastas

### GET /api/subastas
Lista todas las subastas.

Responses:
- 200: lista de subastas

### GET /api/subastas/{id}
Obtiene una subasta por id.

Responses:
- 200: subasta encontrada
- 404: subasta no encontrada

### POST /api/subastas
Crea una subasta.

Request body:
{
  "...": "campos de Subasta"
}

Responses:
- 200: subasta creada

### PUT /api/subastas/{id}
Actualiza una subasta existente.

Request body:
{
  "...": "campos de Subasta"
}

Responses:
- 200: subasta actualizada
- 404: subasta no encontrada

### DELETE /api/subastas/{id}
Elimina una subasta.

Responses:
- 204: subasta eliminada
- 404: subasta no encontrada

## 5) Motor de subasta y operaciones runtime

### POST /api/auction-runtime/subasta/configurar-moneda
Configura moneda de subasta (ADMIN/EMPLEADO).

Request body:
{
  "subastaId": 1,
  "moneda": "ARS"
}

Responses:
- 200: "Moneda de subasta configurada"
- 400: subasta no encontrada o moneda invalida
- 401/403: sin autenticacion o permisos

### POST /api/auction-runtime/subasta/configurar-duracion
Configura duracion de subasta (ADMIN/EMPLEADO).

Request body:
{
  "subastaId": 1,
  "duracionMinutos": 120
}

Responses:
- 200: "Duracion de subasta configurada"
- 400: subasta no encontrada o duracion invalida
- 401/403: sin autenticacion o permisos

### GET /api/auction-runtime/subasta/{subastaId}/timing
Obtiene estado temporal de una subasta (autenticado).

Response body (200):
{
  "subastaId": 1,
  "duracionMinutos": 120,
  "inicio": "2026-04-19T10:00:00",
  "fin": "2026-04-19T12:00:00",
  "estadoTemporal": "EN_CURSO",
  "minutosRestantes": 35
}

Responses:
- 200: timing obtenido
- 400: subasta no encontrada
- 401: no autenticado

### POST /api/auction-runtime/subasta/conectar
Conecta al usuario postor a una subasta.

Request body:
{
  "subastaId": 1
}

Responses:
- 200: "Conexion a subasta realizada"
- 400: regla de negocio invalida (categoria, estado, ventana temporal, etc.)
- 401/403: sin autenticacion o sin rol POSTOR

### DELETE /api/auction-runtime/subasta/desconectar
Desconecta al postor de su subasta activa.

Responses:
- 200: "Desconexion de subasta realizada"
- 400: no estaba conectado
- 401/403: sin autenticacion o sin rol POSTOR

### POST /api/auction-runtime/pujas
Registra una puja del postor.

Request body:
{
  "subastaId": 1,
  "itemId": 10,
  "importe": 15000.00,
  "moneda": "ARS"
}

Response body (200):
{
  "pujoId": 123,
  "itemId": 10,
  "ofertaAnterior": 12000.00,
  "ofertaActual": 15000.00,
  "minimoPermitido": 12100.00,
  "maximoPermitido": 14400.00,
  "mensaje": "Puja registrada correctamente"
}

Responses:
- 200: puja registrada
- 400: regla de negocio invalida (sin medio de pago verificado, categoria no habilitada, moneda, monto, etc.)
- 401/403: sin autenticacion o sin rol POSTOR

### GET /api/auction-runtime/pujas/historial/{itemId}
Devuelve historial de pujas por item.

Responses:
- 200: lista de historial de pujas
- 401/403: sin autenticacion o sin rol habilitado

### POST /api/auction-runtime/comisiones/calcular
Calcula comisiones de comprador, vendedor y casa de subasta.

Request body:
{
  "importeFinal": 15000.00
}

Response body (200):
{
  "importeFinal": 15000.00,
  "porcentajeComprador": 10.00,
  "porcentajeVendedor": 8.00,
  "comisionComprador": 1500.00,
  "comisionVendedor": 1200.00,
  "totalPagaComprador": 16500.00,
  "netoRecibeVendedor": 13800.00,
  "ingresoCasaSubasta": 2700.00
}

Responses:
- 200: calculo exitoso
- 400: importe invalido
- 401/403: sin autenticacion o sin rol habilitado

Reglas relevantes para participar:
- Se puede ver la subasta sin restricciones.
- Para conectarse a una subasta, el usuario debe estar admitido y habilitado por categoria.
- Para pujar, ademas de estar conectado, debe tener al menos un medio de pago verificado y activo.
- Si la categoria del cliente no habilita la subasta, la respuesta es 400 con mensaje "La categoria del cliente no habilita esta subasta".

## 6) Compliance y pagos

### POST /api/compliance/pagos/inicializar
Inicializa estado de pago de un registro de subasta (ADMIN/EMPLEADO).

Request body:
{
  "registroSubastaId": 1
}

Response body (200):
{
  "registroSubastaId": 1,
  "estadoPago": "PENDIENTE",
  "montoOfertado": 10000.00,
  "montoMulta": 0.00,
  "fechaVencimiento": "2026-04-20T10:00:00",
  "fechaLimiteRegularizacion": "2026-04-23T10:00:00",
  "bloqueado": false
}

Responses:
- 200: inicializacion exitosa
- 400: registro invalido o inexistente
- 401/403: sin autenticacion o permisos

### POST /api/compliance/pagos/registrar
Marca pago como registrado (ADMIN/EMPLEADO).

Request body:
{
  "registroSubastaId": 1
}

Responses:
- 200: estado de pago actualizado
- 400: registro invalido o inexistente
- 401/403: sin autenticacion o permisos

### POST /api/compliance/multas/procesar
Procesa moras pendientes (ADMIN/EMPLEADO).

Responses:
- 200: "Moras procesadas: N"
- 401/403: sin autenticacion o permisos

### GET /api/compliance/mis-pagos
Lista estados de pago del usuario autenticado.

Responses:
- 200: lista de PagoEstadoDto
- 401: no autenticado

## 7) Chat de verificacion

### POST /api/verificacion-chat/conversaciones
Crea conversacion para DUENIO.

Response body (200):
{
  "conversacionId": 1,
  "estado": "ABIERTA"
}

Responses:
- 200: conversacion creada
- 401/403: sin autenticacion o rol incorrecto

### POST /api/verificacion-chat/conversaciones/{conversacionId}/tomar
Asigna/toma una conversacion (ADMIN/EMPLEADO).

Responses:
- 200: conversacion actualizada
- 400: conversacion inexistente o no disponible
- 401/403: sin autenticacion o rol incorrecto

### GET /api/verificacion-chat/conversaciones
Lista conversaciones visibles para el usuario.

Responses:
- 200: lista de ConversacionDto
- 400: rol no habilitado
- 401: no autenticado

### GET /api/verificacion-chat/conversaciones/{conversacionId}/mensajes
Lista mensajes de una conversacion.

Responses:
- 200: lista de MensajeChatDto
- 400: conversacion inexistente o sin permisos
- 401: no autenticado

### POST /api/verificacion-chat/conversaciones/{conversacionId}/mensajes
Envia un mensaje en una conversacion.

Request body:
{
  "texto": "Hola, necesito ayuda con mi verificacion"
}

Responses:
- 200: mensaje enviado
- 400: conversacion inexistente, texto invalido o sin permisos
- 401: no autenticado

## 8) Swagger para evidencia de entrega

Con la app levantada:
- Swagger UI: /swagger-ui/index.html
- OpenAPI JSON: /v3/api-docs

Sugerencia para la entrega:
- Adjuntar capturas de Swagger UI por modulo (auth, subastas, pagos).
- Alinear nombres de botones en wireframes con los endpoints de esta guia.
