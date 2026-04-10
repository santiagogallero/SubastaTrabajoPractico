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

Responses:
- 200: {"status":"UP"}

### POST /api/notifications/test-email
Envia un correo de prueba (ADMIN/EMPLEADO).

Request body:
{
  "to": "destino@mail.com"
}

Responses:
- 200: "Email sent"
- 400: email invalido
- 401/403: sin autenticacion o permisos

## 4) CRUD base (personas, productos, subastas)

### Personas
- GET /api/personas
- GET /api/personas/{id}
- POST /api/personas
- PUT /api/personas/{id}
- DELETE /api/personas/{id}

Codigos esperados:
- 200: lecturas/actualizaciones exitosas
- 204: borrado exitoso
- 404: id inexistente (get by id, update, delete)

### Productos
- GET /api/productos
- GET /api/productos/{id}
- POST /api/productos
- PUT /api/productos/{id}
- DELETE /api/productos/{id}

Codigos esperados:
- 200: lecturas/actualizaciones exitosas
- 204: borrado exitoso
- 404: id inexistente (get by id, update, delete)

### Subastas
- GET /api/subastas
- GET /api/subastas/{id}
- POST /api/subastas
- PUT /api/subastas/{id}
- DELETE /api/subastas/{id}

Codigos esperados:
- 200: lecturas/actualizaciones exitosas
- 204: borrado exitoso
- 404: id inexistente (get by id, update, delete)

## 5) Motor de subasta y operaciones runtime

Base path: /api/auction-runtime

- POST /subasta/configurar-moneda (ADMIN/EMPLEADO)
- POST /subasta/configurar-duracion (ADMIN/EMPLEADO)
- GET /subasta/{subastaId}/timing (autenticado)
- POST /subasta/conectar (POSTOR)
- DELETE /subasta/desconectar (POSTOR)
- POST /pujas (POSTOR)
- GET /pujas/historial/{itemId} (POSTOR/ADMIN/EMPLEADO/SUBASTADOR)
- POST /comisiones/calcular (POSTOR/DUENIO/ADMIN/EMPLEADO/SUBASTADOR)

Codigos comunes:
- 200: operacion exitosa
- 400: regla de negocio invalida
- 401: no autenticado
- 403: sin rol suficiente

## 6) Compliance y pagos

Base path: /api/compliance

- POST /pagos/inicializar (ADMIN/EMPLEADO)
- POST /pagos/registrar (ADMIN/EMPLEADO)
- POST /multas/procesar (ADMIN/EMPLEADO)
- GET /mis-pagos (autenticado)

Codigos comunes:
- 200: operacion exitosa
- 400: validacion o datos invalidos
- 401: no autenticado
- 403: sin rol suficiente

## 7) Chat de verificacion

Base path: /api/verificacion-chat

- POST /conversaciones (DUENIO)
- POST /conversaciones/{conversacionId}/tomar (ADMIN/EMPLEADO)
- GET /conversaciones (DUENIO/ADMIN/EMPLEADO)
- GET /conversaciones/{conversacionId}/mensajes (DUENIO/ADMIN/EMPLEADO)
- POST /conversaciones/{conversacionId}/mensajes (DUENIO/ADMIN/EMPLEADO)

Codigos comunes:
- 200: operacion exitosa
- 400: datos invalidos
- 401: no autenticado
- 403: sin rol suficiente

## 8) Swagger para evidencia de entrega

Con la app levantada:
- Swagger UI: /swagger-ui/index.html
- OpenAPI JSON: /v3/api-docs

Sugerencia para la entrega:
- Adjuntar capturas de Swagger UI por modulo (auth, subastas, pagos).
- Alinear nombres de botones en wireframes con los endpoints de esta guia.
