# Sistema de Subastas - Endpoints de la API REST (Primera Entrega)

Este documento describe la API REST del sistema de subastas definido en `TPO_DAI_1C2026.docx`. Cubre de punta a punta el comportamiento que la app movil necesita: registro de postores en dos etapas, gestion de medios de pago, subastas y catalogos, puja con la regla de +1% / +20%, cierre de subastas y compras, flujo de venta por parte del postor, colecciones, seguros, notificaciones y metricas de usuario.

## Convenciones

- Ruta base: `/api/v1`
- Todos los cuerpos de peticion y respuesta usan `application/json`, salvo la carga de imagenes / documentos que usa `multipart/form-data`.
- Autenticacion: JWT Bearer en el header `Authorization`. Los endpoints marcados como `Publico` no requieren token; los marcados con un rol (`Postor`, `Admin`, `Martillero`) requieren ese rol.
- Fechas y horas en ISO-8601 (`2026-04-19T15:30:00Z`).
- Moneda: numeros con dos decimales mas un campo `currency` (`ARS` o `USD`). Una subasta es monomoneda; no se permiten pagos bimonetarios.
- Paginacion (cuando aplica): parametros de query `page` (base 0) y `size`; la respuesta incluye `totalElements`, `totalPages`, `page`, `size`.
- Las rutas usan `kebab-case` y los campos JSON usan `camelCase`.

## Catalogo de codigos HTTP usados

Alineados con el registro IANA de codigos HTTP que cita la consigna.

- `200 OK` - lectura / accion exitosa
- `201 Created` - recurso creado
- `204 No Content` - eliminacion o transicion de estado exitosa sin cuerpo
- `400 Bad Request` - peticion mal formada o campo obligatorio faltante
- `401 Unauthorized` - token ausente, invalido o expirado
- `403 Forbidden` - usuario autenticado pero sin rol, no aprobado aun, sin medio de pago verificado, categoria insuficiente para la subasta, o cuenta bloqueada por multa impaga
- `404 Not Found` - el recurso objetivo no existe
- `409 Conflict` - estado conflictivo (por ejemplo, el usuario ya esta conectado a otra subasta en curso, o una puja concurrente invalido la actual)
- `422 Unprocessable Entity` - regla de negocio violada (puja fuera de la banda +1% / +20%, garantia superada, casillero de declaracion no aceptado, etc.)
- `500 Internal Server Error` - error inesperado

Las actualizaciones en tiempo real de la subasta (nueva mejor oferta, item cerrado, subasta terminada) se envian a los clientes conectados por un canal de tiempo real. Ese transporte no forma parte de esta especificacion REST; estos endpoints son la fuente autoritativa de verdad.

---

## 1. Autenticacion y registro en dos etapas

La consigna define un proceso de registro en dos etapas: el postor primero carga datos personales y fotos del documento para que la empresa realice la investigacion externa; una vez aceptado, se le envia un mail para que vuelva a la app a generar su clave personal y continuar. Un postor no puede pujar hasta que (a) su cuenta esta aprobada con categoria asignada y (b) tiene al menos un medio de pago verificado.

### POST /api/v1/auth/register/initial

Etapa 1 del registro. Carga datos personales y fotos del documento para la investigacion de la empresa. La cuenta queda en estado `PENDING_REVIEW`.

Auth: Publico.

Request body (multipart/form-data):

| Campo | Tipo | Requerido | Notas |
|---|---|---|---|
| `firstName` | string | si | Nombre del postor. |
| `lastName` | string | si | Apellido del postor. |
| `email` | string (email) | si | Unico en el sistema. |
| `legalAddress` | string | si | Domicilio legal declarado. |
| `countryCode` | string (ISO-3166 alpha-2) | si | Pais de origen. |
| `documentNumber` | string | si | Numero de documento, unico. |
| `documentFront` | file (image) | si | Foto del frente del documento. |
| `documentBack` | file (image) | si | Foto del dorso del documento. |

Response body (201):

```json
{
  "registrationId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "jane.doe@example.com",
  "status": "PENDING_REVIEW",
  "message": "Your information was received. We will contact you by email after the investigation is completed."
}
```

Codigos de estado:
- `201 Created` - registro recibido
- `400 Bad Request` - campo o imagen faltante o mal formada
- `409 Conflict` - el email o el numero de documento ya estan registrados

### POST /api/v1/auth/register/complete

Etapa 2 del registro. La invoca el postor luego de recibir el mail de aceptacion de la empresa. Define la clave personal usando el token de un solo uso recibido por mail.

Auth: Publico (el token `activationToken` actua como autenticacion).

Request body:

```json
{
  "activationToken": "a1b2c3d4-...",
  "password": "StrongP@ssw0rd",
  "passwordConfirmation": "StrongP@ssw0rd"
}
```

Codigos de estado:
- `200 OK` - clave definida, la cuenta queda `ACTIVE`
- `400 Bad Request` - claves no coinciden o no cumplen la politica
- `401 Unauthorized` - token invalido o expirado
- `409 Conflict` - la cuenta no esta en estado `APPROVED_AWAITING_PASSWORD`

### POST /api/v1/auth/login

Autentica a un usuario activo y devuelve un JWT.

Auth: Publico.

Request body:

```json
{
  "email": "jane.doe@example.com",
  "password": "StrongP@ssw0rd"
}
```

Response body (200):

```json
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "c7e3...",
    "firstName": "Jane",
    "lastName": "Doe",
    "category": "COMMON",
    "roles": ["BIDDER"]
  }
}
```

Codigos de estado:
- `200 OK` - login exitoso
- `400 Bad Request` - campos faltantes
- `401 Unauthorized` - credenciales invalidas
- `403 Forbidden` - cuenta no aprobada o bloqueada por multa impaga

### POST /api/v1/auth/logout

Invalida el token actual.

Auth: cualquier usuario autenticado.

Codigos de estado:
- `204 No Content` - token invalidado
- `401 Unauthorized` - sin token o token invalido

### POST /api/v1/auth/password/forgot

Inicia el flujo de recuperacion de clave enviando un token al mail del usuario.

Auth: Publico.

Request body:

```json
{ "email": "jane.doe@example.com" }
```

Codigos de estado:
- `202 Accepted` - se devuelve siempre, aunque el mail no exista, para evitar enumeracion de usuarios
- `400 Bad Request` - email faltante o mal formado

### POST /api/v1/auth/password/reset

Completa el flujo de recuperacion usando el token recibido por mail.

Auth: Publico.

Request body:

```json
{
  "resetToken": "f4c8...",
  "password": "NewP@ssw0rd",
  "passwordConfirmation": "NewP@ssw0rd"
}
```

Codigos de estado:
- `200 OK` - clave actualizada
- `400 Bad Request` - claves no coinciden o clave debil
- `401 Unauthorized` - token invalido o expirado

---

## 2. Perfil del usuario autenticado

### GET /api/v1/users/me

Devuelve el perfil del usuario autenticado, incluyendo la categoria asignada y los flags de estado.

Auth: cualquier usuario autenticado.

Response body (200):

```json
{
  "id": "c7e3...",
  "firstName": "Jane",
  "lastName": "Doe",
  "email": "jane.doe@example.com",
  "legalAddress": "Av. Siempre Viva 742",
  "countryCode": "AR",
  "category": "SILVER",
  "status": "ACTIVE",
  "blocked": false,
  "hasVerifiedPaymentMethod": true,
  "outstandingPenaltyAmount": 0.0,
  "createdAt": "2026-01-10T12:34:00Z"
}
```

Codigos de estado:
- `200 OK`
- `401 Unauthorized`

### PATCH /api/v1/users/me

Actualiza los campos editables del perfil. La categoria, el estado y el flag de bloqueo son de solo lectura desde el cliente.

Auth: Postor.

Request body:

```json
{
  "legalAddress": "Av. Corrientes 1234",
  "countryCode": "AR"
}
```

Codigos de estado:
- `200 OK` - devuelve el perfil actualizado
- `400 Bad Request`
- `401 Unauthorized`

### GET /api/v1/users/me/category

Devuelve la categoria actual (`COMMON`, `SPECIAL`, `SILVER`, `GOLD`, `PLATINUM`).

Auth: Postor.

Response body (200):

```json
{ "category": "SILVER", "assignedAt": "2026-02-03T10:00:00Z" }
```

Codigos de estado:
- `200 OK`
- `401 Unauthorized`

---

## 3. Admin - revision de registros y usuarios

Usados por la empresa para aprobar registros pendientes, asignar categorias y bloquear usuarios que incumplieron el pago.

### GET /api/v1/admin/registrations

Lista los registros pendientes con filtros opcionales.

Auth: Admin.

Query params:

| Param | Tipo | Notas |
|---|---|---|
| `status` | enum | `PENDING_REVIEW`, `APPROVED_AWAITING_PASSWORD`, `REJECTED` |
| `page` | int | |
| `size` | int | |

Response body (200): lista paginada con `registrationId`, `firstName`, `lastName`, `email`, `countryCode`, `documentNumber`, `status`, `submittedAt`.

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden`

### POST /api/v1/admin/registrations/{registrationId}/approve

Aprueba un registro y asigna categoria; dispara el mail de aceptacion con el token de activacion.

Auth: Admin.

Path params: `registrationId` (uuid).

Request body:

```json
{ "category": "COMMON", "notes": "Documentation verified." }
```

Codigos de estado:
- `200 OK` - devuelve el registro actualizado
- `400 Bad Request` - categoria invalida
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict` - ya aprobado o rechazado previamente

### POST /api/v1/admin/registrations/{registrationId}/reject

Rechaza un registro.

Auth: Admin.

Request body:

```json
{ "reason": "Document photos unreadable." }
```

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict`

### PATCH /api/v1/admin/users/{userId}/category

Cambia la categoria de un usuario existente (por ejemplo, mejora por diversidad de medios de pago o actividad).

Auth: Admin.

Request body:

```json
{ "category": "GOLD", "reason": "Sustained activity and multiple verified payment methods." }
```

Codigos de estado:
- `200 OK`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

### POST /api/v1/admin/users/{userId}/block

Bloquea a un usuario (por ejemplo, al derivarse el caso a la justicia por multa impaga). Un usuario bloqueado pierde acceso a todos los servicios.

Auth: Admin.

Request body:

```json
{ "reason": "Unpaid default forwarded to legal." }
```

Codigos de estado:
- `204 No Content`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

### POST /api/v1/admin/users/{userId}/unblock

Levanta el bloqueo.

Auth: Admin.

Codigos de estado:
- `204 No Content`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

---

## 4. Medios de pago (para pujar)

Un postor debe registrar al menos un medio de pago y al menos uno de ellos debe estar verificado por la empresa antes del inicio de la subasta. Tipos soportados: `BANK_ACCOUNT` (nacional o extranjera), `CREDIT_CARD` (nacional o internacional) y `CERTIFIED_CHECK` (con un monto de garantia que el postor no puede superar con sus pujas).

### GET /api/v1/payment-methods

Lista los medios de pago del postor autenticado.

Auth: Postor.

Response body (200):

```json
[
  {
    "id": "pm-1",
    "type": "CERTIFIED_CHECK",
    "alias": "Check Banco Nacion",
    "currency": "ARS",
    "guaranteeAmount": 500000.0,
    "verified": true,
    "foreign": false,
    "createdAt": "2026-03-01T09:00:00Z"
  }
]
```

Codigos de estado:
- `200 OK`
- `401 Unauthorized`

### POST /api/v1/payment-methods

Registra un nuevo medio de pago. Queda creado como no verificado (`verified=false`) hasta que la empresa lo valide.

Auth: Postor.

Request body (cuenta bancaria):

```json
{
  "type": "BANK_ACCOUNT",
  "alias": "Chase USD",
  "currency": "USD",
  "foreign": true,
  "bankName": "Chase",
  "accountNumber": "123456789",
  "holderName": "Jane Doe"
}
```

Request body (tarjeta de credito):

```json
{
  "type": "CREDIT_CARD",
  "alias": "Visa International",
  "currency": "USD",
  "foreign": true,
  "brand": "VISA",
  "cardNumber": "4111111111111111",
  "cardHolder": "JANE DOE",
  "expirationMonth": 12,
  "expirationYear": 2029
}
```

Request body (cheque certificado):

```json
{
  "type": "CERTIFIED_CHECK",
  "alias": "Check Banco Nacion",
  "currency": "ARS",
  "foreign": false,
  "guaranteeAmount": 500000.0,
  "issuingBank": "Banco Nacion",
  "checkNumber": "00123456"
}
```

Codigos de estado:
- `201 Created` - medio de pago creado (`verified=false`)
- `400 Bad Request` - campo faltante o invalido
- `401 Unauthorized`
- `422 Unprocessable Entity` - combinacion invalida (por ejemplo, `CERTIFIED_CHECK` sin `guaranteeAmount`)

### GET /api/v1/payment-methods/{paymentMethodId}

Auth: Postor (dueno) o Admin.

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden` - el usuario no es el dueno del medio
- `404 Not Found`

### PATCH /api/v1/payment-methods/{paymentMethodId}

Actualiza campos mutables como el alias. Un medio verificado no puede modificar campos sensibles (numero de cuenta, numero de tarjeta, monto de garantia) sin re-verificacion.

Auth: Postor (dueno).

Request body:

```json
{ "alias": "Chase principal" }
```

Codigos de estado:
- `200 OK`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `422 Unprocessable Entity` - intento de modificar un campo protegido de un medio ya verificado

### DELETE /api/v1/payment-methods/{paymentMethodId}

Elimina un medio de pago. No se puede eliminar si esta reservado para una subasta en curso.

Auth: Postor (dueno).

Codigos de estado:
- `204 No Content`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict` - medio comprometido en una subasta abierta

### POST /api/v1/admin/payment-methods/{paymentMethodId}/verify

Marca un medio como verificado (o rechaza la verificacion) luego de la validacion externa de la empresa.

Auth: Admin.

Request body:

```json
{ "verified": true, "notes": "Funds visible in escrow account." }
```

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

---

## 5. Subastas

Cada subasta tiene dia y horario, una categoria, un martillero, una moneda (`ARS` o `USD`), una ubicacion y un catalogo. Los catalogos son publicos pero el precio base de los items solo es visible para usuarios registrados.

### GET /api/v1/auctions

Lista subastas con filtros. Visible para cualquiera.

Auth: Publico (los usuarios autenticados ven el precio base en los endpoints de catalogo).

Query params:

| Param | Tipo | Notas |
|---|---|---|
| `status` | enum | `SCHEDULED`, `OPEN`, `CLOSED`, `CANCELLED` |
| `category` | enum | `COMMON`, `SPECIAL`, `SILVER`, `GOLD`, `PLATINUM` |
| `currency` | enum | `ARS`, `USD` |
| `fromDate` | datetime | |
| `toDate` | datetime | |
| `auctioneerId` | uuid | |
| `page`, `size` | int | |

Response body (200): lista paginada con `id`, `title`, `scheduledAt`, `status`, `category`, `currency`, `location`, resumen del `auctioneer` e `itemCount`.

Codigos de estado:
- `200 OK`

### GET /api/v1/auctions/{auctionId}

Detalle completo de la subasta.

Auth: Publico.

Path params: `auctionId` (uuid).

Response body (200):

```json
{
  "id": "a1...",
  "title": "Clasicos de diseno - Abril 2026",
  "scheduledAt": "2026-04-20T18:00:00Z",
  "status": "SCHEDULED",
  "category": "SILVER",
  "currency": "ARS",
  "location": "Sala Central, Buenos Aires",
  "auctioneer": { "id": "u2...", "firstName": "Carlos", "lastName": "Gomez" },
  "itemCount": 48,
  "streamingUrl": null
}
```

Codigos de estado:
- `200 OK`
- `404 Not Found`

### POST /api/v1/auctions

Crea una subasta.

Auth: Admin.

Request body:

```json
{
  "title": "Clasicos de diseno - Abril 2026",
  "scheduledAt": "2026-04-20T18:00:00Z",
  "category": "SILVER",
  "currency": "ARS",
  "location": "Sala Central, Buenos Aires",
  "auctioneerId": "u2..."
}
```

Codigos de estado:
- `201 Created`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `422 Unprocessable Entity` - combinacion invalida de moneda / categoria

### PATCH /api/v1/auctions/{auctionId}

Actualiza una subasta mientras sigue en estado `SCHEDULED`.

Auth: Admin.

Codigos de estado:
- `200 OK`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict` - subasta ya abierta o cerrada

### DELETE /api/v1/auctions/{auctionId}

Cancela una subasta programada.

Auth: Admin.

Codigos de estado:
- `204 No Content`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict`

### POST /api/v1/auctions/{auctionId}/open

Pasa una subasta programada a estado `OPEN`.

Auth: Martillero o Admin.

Codigos de estado:
- `200 OK` - devuelve la subasta actualizada
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict` - ya abierta o cerrada

### POST /api/v1/auctions/{auctionId}/close

Cierra una subasta abierta.

Auth: Martillero o Admin.

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict`

### POST /api/v1/auctions/{auctionId}/join

El postor autenticado se conecta a la subasta. El servidor valida:
- que el usuario este `ACTIVE` y no bloqueado;
- que la categoria del usuario sea `>=` a la de la subasta;
- que el usuario no este conectado a otra subasta en curso (solo una a la vez);
- si quiere pujar, debe tener al menos un medio de pago verificado; en caso contrario solo se puede conectar como `SPECTATOR`.

Auth: Postor.

Request body (opcional):

```json
{ "mode": "BIDDER" }
```

`mode` puede ser `BIDDER` (default si el usuario tiene medio de pago verificado) o `SPECTATOR`.

Response body (200):

```json
{
  "auctionId": "a1...",
  "mode": "BIDDER",
  "joinedAt": "2026-04-20T18:00:10Z",
  "streamingUrl": "https://streaming.example/live/a1"
}
```

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden` - categoria insuficiente, sin medio de pago verificado para `mode=BIDDER`, o usuario bloqueado
- `404 Not Found`
- `409 Conflict` - ya conectado a otra subasta

### POST /api/v1/auctions/{auctionId}/leave

Desconecta al usuario de la subasta.

Auth: Postor.

Codigos de estado:
- `204 No Content`
- `401 Unauthorized`
- `404 Not Found`
- `409 Conflict` - no estaba conectado a esta subasta

### GET /api/v1/auctions/{auctionId}/streaming

Devuelve la URL del servicio de streaming. Cualquier usuario registrado y aprobado puede acceder.

Auth: Postor.

Response body (200):

```json
{ "streamingUrl": "https://streaming.example/live/a1" }
```

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

---

## 6. Catalogo e items (piezas)

Cada item tiene numero de pieza, descripcion, precio base, dueno actual y hasta aproximadamente seis imagenes. Los items pueden estar formados por varios elementos (por ejemplo, un juego de te de 18 piezas). Para obras de arte o diseno se incluyen datos adicionales: artista, fecha e historia.

### GET /api/v1/auctions/{auctionId}/catalog

Devuelve el catalogo de la subasta. Es publico, pero `basePrice` solo se devuelve si el usuario esta autenticado y registrado.

Auth: Publico.

Query params: `page`, `size`.

Response body (200):

```json
{
  "auctionId": "a1...",
  "totalElements": 48,
  "items": [
    {
      "id": "i1...",
      "pieceNumber": "LOT-042",
      "description": "Silver tea set, 18 pieces.",
      "multiElement": true,
      "elementCount": 18,
      "basePrice": 250000.0,
      "currency": "ARS",
      "currentOwner": { "id": "u3...", "firstName": "Maria", "lastName": "Paz" },
      "artInfo": {
        "artist": "Julio Le Parc",
        "date": "1969",
        "history": "Exhibited at Venice Biennale..."
      },
      "coverImageUrl": "https://cdn.example/img/i1/cover.jpg"
    }
  ]
}
```

Para llamadores anonimos, `basePrice` se devuelve como `null`.

Codigos de estado:
- `200 OK`
- `404 Not Found`

### GET /api/v1/items/{itemId}

Detalle completo de un item.

Auth: Publico (el precio base queda oculto para llamadores anonimos).

Codigos de estado:
- `200 OK`
- `404 Not Found`

### POST /api/v1/items

Crea un item para incluir en el catalogo de una subasta (tipicamente despues de aceptar una solicitud de venta del postor).

Auth: Admin.

Request body:

```json
{
  "auctionId": "a1...",
  "pieceNumber": "LOT-042",
  "description": "Silver tea set, 18 pieces.",
  "multiElement": true,
  "elementCount": 18,
  "basePrice": 250000.0,
  "currency": "ARS",
  "currentOwnerId": "u3...",
  "artInfo": {
    "artist": "Julio Le Parc",
    "date": "1969",
    "history": "Exhibited at Venice Biennale..."
  }
}
```

Codigos de estado:
- `201 Created`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found` - la subasta o el dueno no existen
- `422 Unprocessable Entity` - la moneda no coincide con la de la subasta

### PATCH /api/v1/items/{itemId}

Actualiza campos del item antes de que la subasta este `OPEN`.

Auth: Admin.

Codigos de estado:
- `200 OK`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict` - subasta ya abierta o item ya vendido

### DELETE /api/v1/items/{itemId}

Retira un item del catalogo antes del inicio de la subasta.

Auth: Admin.

Codigos de estado:
- `204 No Content`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict`

### GET /api/v1/items/{itemId}/images

Lista las imagenes de un item (aproximadamente seis).

Auth: Publico.

Response body (200):

```json
[
  { "id": "img1", "url": "https://cdn.example/img/i1/1.jpg", "order": 1 },
  { "id": "img2", "url": "https://cdn.example/img/i1/2.jpg", "order": 2 }
]
```

Codigos de estado:
- `200 OK`
- `404 Not Found`

### POST /api/v1/items/{itemId}/images

Sube una imagen al item.

Auth: Admin.

Request body (multipart/form-data):

| Campo | Tipo | Requerido | Notas |
|---|---|---|---|
| `image` | file | si | Archivo de imagen. |
| `order` | int | no | Orden de visualizacion opcional. |

Codigos de estado:
- `201 Created`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `422 Unprocessable Entity` - se supero el limite de imagenes

### DELETE /api/v1/items/{itemId}/images/{imageId}

Auth: Admin.

Codigos de estado:
- `204 No Content`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

### GET /api/v1/items/{itemId}/bids

Historial cronologico de pujas de un item.

Auth: Postor (cualquier usuario registrado).

Query params: `page`, `size`.

Response body (200):

```json
{
  "itemId": "i1...",
  "bids": [
    {
      "id": "b1",
      "bidderId": "u5...",
      "bidderDisplayName": "Usuario 5",
      "amount": 260000.0,
      "currency": "ARS",
      "placedAt": "2026-04-20T18:05:12Z"
    }
  ],
  "totalElements": 12
}
```

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `404 Not Found`

### GET /api/v1/items/{itemId}/location

Ubicacion actual de la pieza en deposito. Solo la ve el dueno actual (el que entrego la pieza para la subasta).

Auth: Postor (dueno de la pieza).

Response body (200):

```json
{
  "itemId": "i1...",
  "warehouseName": "Deposito Central",
  "warehouseAddress": "Av. Industria 3200",
  "storedSince": "2026-03-15T10:00:00Z"
}
```

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden` - el llamador no es el dueno
- `404 Not Found`

### GET /api/v1/items/{itemId}/insurance

Devuelve la poliza de seguro contratada sobre la pieza. La ve el dueno actual. Incluye datos de contacto de la compania para que el dueno pueda aumentar el valor pagando la diferencia del premio.

Auth: Postor (dueno).

Response body (200):

```json
{
  "itemId": "i1...",
  "policyNumber": "POL-78910",
  "insurer": { "name": "Aseguradora SA", "phone": "+54 11 5555 5555", "email": "contacto@aseguradora.example" },
  "insuredAmount": 250000.0,
  "currency": "ARS",
  "validFrom": "2026-03-15T00:00:00Z",
  "validTo": "2026-05-15T00:00:00Z",
  "coversMultiplePieces": false
}
```

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

---

## 7. Pujas

Reglas segun la consigna:

- La puja debe ser estrictamente mayor a la mejor oferta actual.
- Puja minima: `highestBid + 1% * basePrice`.
- Puja maxima: `highestBid + 20% * basePrice`.
- Estos limites no aplican a subastas de categoria `GOLD` y `PLATINUM`.
- Si el usuario dejo un monto de garantia (por ejemplo, un cheque certificado), no puede comprometer mas de ese monto entre sus pujas abiertas.
- Una nueva puja solo se acepta luego de que el servidor confirmo la anterior. Las pujas concurrentes se rechazan con `409 Conflict`.

### GET /api/v1/auctions/{auctionId}/current-item

Devuelve el item que el martillero esta subastando actualmente en una subasta `OPEN`.

Auth: Postor.

Response body (200):

```json
{
  "itemId": "i1...",
  "pieceNumber": "LOT-042",
  "description": "Silver tea set, 18 pieces.",
  "basePrice": 250000.0,
  "currency": "ARS",
  "highestBid": {
    "amount": 260000.0,
    "bidderId": "u5...",
    "placedAt": "2026-04-20T18:05:12Z"
  },
  "minNextBid": 262500.0,
  "maxNextBid": 310000.0,
  "categoryBandApplies": true
}
```

`categoryBandApplies` es `false` para subastas `GOLD` y `PLATINUM`; en ese caso `minNextBid = highestBid + 0.01` y `maxNextBid` no se impone.

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `404 Not Found`
- `409 Conflict` - la subasta no esta `OPEN`

### POST /api/v1/auctions/{auctionId}/items/{itemId}/bids

Registra una puja sobre un item en la subasta indicada.

Auth: Postor. Requisitos:
- haberse unido a la subasta en modo `BIDDER`;
- tener un medio de pago verificado y garantia remanente suficiente;
- que el monto este dentro de la banda permitida (salvo `GOLD` y `PLATINUM`).

Request body:

```json
{
  "amount": 270000.0,
  "currency": "ARS",
  "paymentMethodId": "pm-1"
}
```

`paymentMethodId` es el medio de pago que el postor compromete para esta puja.

Response body (201):

```json
{
  "bidId": "b42",
  "itemId": "i1...",
  "amount": 270000.0,
  "currency": "ARS",
  "placedAt": "2026-04-20T18:06:01Z",
  "highestNow": true,
  "minNextBid": 272500.0,
  "maxNextBid": 320000.0
}
```

Codigos de estado:
- `201 Created` - puja aceptada
- `400 Bad Request` - campos faltantes o moneda incorrecta
- `401 Unauthorized`
- `403 Forbidden` - el usuario no se unio a la subasta, no tiene medio de pago verificado o esta bloqueado
- `404 Not Found` - subasta o item inexistentes
- `409 Conflict` - la mejor oferta cambio entre la lectura y el envio; hay que pujar de nuevo sobre el estado actualizado
- `422 Unprocessable Entity` - monto por debajo de `minNextBid`, por encima de `maxNextBid` o mas alla de la garantia remanente

### GET /api/v1/auctions/{auctionId}/items/{itemId}/bids/highest

Devuelve la mejor oferta actual para un item en una subasta.

Auth: Postor.

Response body (200):

```json
{
  "itemId": "i1...",
  "amount": 270000.0,
  "currency": "ARS",
  "bidderId": "u5...",
  "placedAt": "2026-04-20T18:06:01Z"
}
```

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `404 Not Found`

### GET /api/v1/users/me/bids

Historial propio de pujas del postor autenticado, en todas las subastas.

Auth: Postor.

Query params: `auctionId`, `itemId`, `fromDate`, `toDate`, `page`, `size`.

Codigos de estado:
- `200 OK`
- `401 Unauthorized`

---

## 8. Cierre de subastas y compras

Cuando ya nadie puja mas, el ultimo postor gana el item. Si no hay pujas, la empresa compra el bien al valor base. Al cerrar se crea una compra con el importe final, las comisiones y el costo de envio. Se envia un mensaje privado al comprador con el detalle del pago.

### POST /api/v1/auctions/{auctionId}/items/{itemId}/close

Cierra la puja sobre un item y genera la compra (o la compra por parte de la empresa al precio base).

Auth: Martillero o Admin.

Response body (200):

```json
{
  "itemId": "i1...",
  "outcome": "SOLD",
  "winningBid": {
    "bidId": "b42",
    "bidderId": "u5...",
    "amount": 270000.0,
    "currency": "ARS"
  },
  "purchaseId": "p1..."
}
```

`outcome` puede ser `SOLD` o `NO_BIDS` (comprado por la casa).

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict` - item ya cerrado o la subasta no esta `OPEN`

### GET /api/v1/purchases

Lista las compras del comprador autenticado.

Auth: Postor.

Query params: `status` (`PENDING_PAYMENT`, `PAID`, `DEFAULTED`, `DELIVERED`), `page`, `size`.

Response body (200): lista paginada de compras.

Codigos de estado:
- `200 OK`
- `401 Unauthorized`

### GET /api/v1/purchases/{purchaseId}

Detalle de la compra con el desglose.

Auth: Postor (dueno de la compra) o Admin.

Response body (200):

```json
{
  "id": "p1...",
  "auctionId": "a1...",
  "itemId": "i1...",
  "buyerId": "u5...",
  "winningAmount": 270000.0,
  "currency": "ARS",
  "commissionAmount": 27000.0,
  "shippingAmount": 12000.0,
  "totalAmount": 309000.0,
  "paymentMethodId": "pm-1",
  "deliveryOption": "SHIPPED",
  "status": "PENDING_PAYMENT",
  "paymentDueAt": "2026-04-23T18:00:00Z",
  "insuranceKeptAfterDelivery": true,
  "closedAt": "2026-04-20T18:45:00Z"
}
```

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

### POST /api/v1/purchases/{purchaseId}/select-payment-method

Selecciona cual de los medios de pago registrados y verificados se usa para esta compra. El monto de garantia del medio debe ser suficiente.

Auth: Postor (dueno).

Request body:

```json
{ "paymentMethodId": "pm-1" }
```

Codigos de estado:
- `200 OK`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `422 Unprocessable Entity` - medio no verificado, moneda incompatible, o garantia insuficiente

### POST /api/v1/purchases/{purchaseId}/select-delivery

Elige la modalidad de entrega: `SHIPPED` (default) o `PICKUP`. El retiro personal hace que la pieza pierda la cobertura del seguro una vez retirada.

Auth: Postor (dueno).

Request body:

```json
{
  "deliveryOption": "SHIPPED",
  "shippingAddress": "Av. Corrientes 1234, CABA, AR"
}
```

Cuando `deliveryOption` es `PICKUP`, se omite `shippingAddress` e `insuranceKeptAfterDelivery` pasa a `false`.

Codigos de estado:
- `200 OK`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

### GET /api/v1/purchases/{purchaseId}/invoice

Devuelve la factura (importe + comisiones + envio) en una estructura imprimible.

Auth: Postor (dueno) o Admin.

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

### POST /api/v1/purchases/{purchaseId}/pay

Confirma que se realizo el pago con el medio seleccionado. Mueve la compra a `PAID` y registra al nuevo dueno del item.

Auth: Postor (dueno).

Request body:

```json
{ "reference": "TX-789456" }
```

Codigos de estado:
- `200 OK`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict` - ya pagada o en default
- `422 Unprocessable Entity` - fondos insuficientes; esto dispara la multa del 10% (ver seccion de multas)

---

## 9. Multas

Si el ganador no puede pagar, se genera una multa del 10% sobre el importe ofertado. El postor tiene 72 hs para presentar los fondos. Hasta pagar la multa no puede unirse a otra subasta. Pasadas las 72 hs sin regularizar, la cuenta se bloquea y el caso se deriva a la justicia.

### GET /api/v1/users/me/penalties

Lista las multas del postor autenticado.

Auth: Postor.

Response body (200):

```json
[
  {
    "id": "pen1",
    "purchaseId": "p1...",
    "amount": 27000.0,
    "currency": "ARS",
    "status": "OUTSTANDING",
    "createdAt": "2026-04-23T18:00:00Z",
    "dueAt": "2026-04-26T18:00:00Z"
  }
]
```

Codigos de estado:
- `200 OK`
- `401 Unauthorized`

### POST /api/v1/users/me/penalties/{penaltyId}/pay

Paga una multa con uno de los medios de pago verificados.

Auth: Postor.

Request body:

```json
{ "paymentMethodId": "pm-1", "reference": "TX-PEN-123" }
```

Codigos de estado:
- `200 OK`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict` - multa ya pagada o expirada
- `422 Unprocessable Entity` - medio no verificado o garantia insuficiente

### POST /api/v1/admin/penalties

Crea una multa manualmente (por ejemplo, cuando el default se detecta fuera de la app).

Auth: Admin.

Request body:

```json
{
  "purchaseId": "p1...",
  "amount": 27000.0,
  "currency": "ARS",
  "dueAt": "2026-04-26T18:00:00Z"
}
```

Codigos de estado:
- `201 Created`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

---

## 10. Flujo de venta - solicitud de subasta de un bien

Un usuario puede solicitar que la empresa subaste un bien propio. Debe cargar los datos, al menos seis fotos, declarar que el bien le pertenece (casillero obligatorio) y opcionalmente adjuntar documentos de origen licito. La empresa inspecciona la pieza y la acepta o la rechaza. Si la acepta, propone fecha, lugar, valor base y comisiones; el usuario acepta o rechaza la oferta.

### POST /api/v1/submissions

Crea una solicitud de subasta.

Auth: Postor.

Request body:

```json
{
  "description": "Silver tea set, 18 pieces.",
  "history": "Family heirloom since 1920.",
  "multiElement": true,
  "elementCount": 18,
  "artInfo": {
    "artist": "Unknown",
    "date": "circa 1920"
  },
  "ownershipDeclaration": true,
  "hasLegalOriginDocuments": true
}
```

`ownershipDeclaration` debe ser `true`; si no, se rechaza con `422`.

Codigos de estado:
- `201 Created` - devuelve la solicitud con `status=DRAFT`
- `400 Bad Request`
- `401 Unauthorized`
- `422 Unprocessable Entity` - falta la declaracion de propiedad

### POST /api/v1/submissions/{submissionId}/photos

Sube fotos. Se requieren al menos seis fotos antes de enviar la solicitud a revision.

Auth: Postor (dueno de la solicitud).

Request body (multipart/form-data):

| Campo | Tipo | Requerido |
|---|---|---|
| `image` | file | si |

Response body (201):

```json
{ "id": "ph1", "url": "https://cdn.example/sub/s1/ph1.jpg", "order": 1 }
```

Codigos de estado:
- `201 Created`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `422 Unprocessable Entity` - formato no soportado

### POST /api/v1/submissions/{submissionId}/origin-documents

Sube documentos opcionales de origen / legalidad.

Auth: Postor (dueno).

Request body (multipart/form-data): archivo `document`.

Codigos de estado:
- `201 Created`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

### POST /api/v1/submissions/{submissionId}/submit

Envia la solicitud a revision de la empresa. Requiere al menos seis fotos y la declaracion de propiedad aceptada.

Auth: Postor (dueno).

Codigos de estado:
- `200 OK` - estado pasa a `UNDER_REVIEW`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `422 Unprocessable Entity` - menos de seis fotos o falta la declaracion

### GET /api/v1/submissions

Lista solicitudes. Los postores ven las propias; los admin ven todas.

Auth: Postor o Admin.

Query params: `status` (`DRAFT`, `UNDER_REVIEW`, `AWAITING_SHIPMENT`, `RECEIVED`, `REJECTED`, `OFFER_PROPOSED`, `OFFER_ACCEPTED`, `OFFER_REJECTED`, `LISTED`), `page`, `size`.

Codigos de estado:
- `200 OK`
- `401 Unauthorized`

### GET /api/v1/submissions/{submissionId}

Auth: Postor (dueno) o Admin.

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

### POST /api/v1/admin/submissions/{submissionId}/request-shipment

La empresa le indica al usuario que envie la pieza a una direccion de inspeccion. El estado pasa a `AWAITING_SHIPMENT`.

Auth: Admin.

Request body:

```json
{ "inspectionAddress": "Deposito Central, Av. Industria 3200" }
```

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict`

### POST /api/v1/admin/submissions/{submissionId}/mark-received

La empresa marca la pieza como recibida en el deposito de inspeccion.

Auth: Admin.

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict`

### POST /api/v1/admin/submissions/{submissionId}/inspection-result

Acepta o rechaza una solicitud recibida.

Auth: Admin.

Request body (aceptacion):

```json
{ "result": "ACCEPTED" }
```

Request body (rechazo):

```json
{
  "result": "REJECTED",
  "reason": "Signs of modern restoration invalidate authenticity.",
  "returnCost": 15000.0,
  "returnCurrency": "ARS"
}
```

Codigos de estado:
- `200 OK`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict`

### POST /api/v1/admin/submissions/{submissionId}/offer

Propone al vendedor el valor base, las comisiones, la fecha y el lugar de la subasta en la que se incluira la pieza.

Auth: Admin.

Request body:

```json
{
  "auctionId": "a1...",
  "proposedBasePrice": 250000.0,
  "currency": "ARS",
  "sellerCommissionPercent": 10.0,
  "scheduledAt": "2026-04-20T18:00:00Z",
  "location": "Sala Central, Buenos Aires"
}
```

Codigos de estado:
- `200 OK`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict`

### POST /api/v1/submissions/{submissionId}/offer/accept

El vendedor acepta la oferta. El item se incorpora al catalogo de la subasta.

Auth: Postor (dueno).

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict`

### POST /api/v1/submissions/{submissionId}/offer/reject

El vendedor rechaza la oferta. La empresa procedera a la devolucion con cargo.

Auth: Postor (dueno).

Request body:

```json
{ "reason": "Base price too low." }
```

Response body (200):

```json
{
  "submissionId": "s1",
  "status": "OFFER_REJECTED",
  "returnCost": 15000.0,
  "returnCurrency": "ARS"
}
```

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict`

---

## 11. Colecciones

Cuando un usuario envia muchas piezas, la empresa puede agruparlas como una coleccion con el nombre del usuario para una subasta dedicada.

### POST /api/v1/admin/collections

Crea una coleccion.

Auth: Admin.

Request body:

```json
{
  "name": "Coleccion Perez",
  "ownerId": "u7...",
  "auctionId": "a9...",
  "submissionIds": ["s1", "s2", "s3"]
}
```

Codigos de estado:
- `201 Created`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

### GET /api/v1/collections/{collectionId}

Auth: Publico.

Response body (200):

```json
{
  "id": "c1...",
  "name": "Coleccion Perez",
  "ownerId": "u7...",
  "auctionId": "a9...",
  "itemIds": ["i10", "i11", "i12"]
}
```

Codigos de estado:
- `200 OK`
- `404 Not Found`

### POST /api/v1/admin/collections/{collectionId}/items

Agrega una solicitud (ya aceptada) a la coleccion.

Auth: Admin.

Request body:

```json
{ "submissionId": "s4" }
```

Codigos de estado:
- `200 OK`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

### DELETE /api/v1/admin/collections/{collectionId}/items/{itemId}

Auth: Admin.

Codigos de estado:
- `204 No Content`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

---

## 12. Cuentas de cobro del vendedor

El vendedor debe declarar, antes del inicio de la subasta, la cuenta (posiblemente del exterior) a la que se enviaran los fondos resultantes de la venta.

### GET /api/v1/payout-accounts

Lista las cuentas de cobro del usuario autenticado.

Auth: Postor.

Codigos de estado:
- `200 OK`
- `401 Unauthorized`

### POST /api/v1/payout-accounts

Registra una cuenta de cobro.

Auth: Postor.

Request body:

```json
{
  "alias": "Chase USD personal",
  "currency": "USD",
  "foreign": true,
  "bankName": "Chase",
  "accountNumber": "123456789",
  "swiftCode": "CHASUS33",
  "holderName": "Jane Doe"
}
```

Codigos de estado:
- `201 Created`
- `400 Bad Request`
- `401 Unauthorized`

### PATCH /api/v1/payout-accounts/{payoutAccountId}

Auth: Postor (dueno).

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict` - la cuenta ya esta asociada a una subasta que comenzo

### DELETE /api/v1/payout-accounts/{payoutAccountId}

Auth: Postor (dueno).

Codigos de estado:
- `204 No Content`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict`

---

## 13. Notificaciones y mensajes privados

Se usan para el seguimiento del mail de aceptacion de la etapa 2, los mensajes privados con el detalle del pago al comprador, las causas de rechazo de una solicitud y las alertas generales en la app.

### GET /api/v1/notifications

Lista las notificaciones del usuario, ordenadas de la mas reciente a la mas antigua.

Auth: Postor.

Query params:

| Param | Tipo | Notas |
|---|---|---|
| `read` | boolean | Filtra por estado de lectura. |
| `category` | enum | `REGISTRATION`, `PAYMENT`, `AUCTION`, `SUBMISSION`, `PENALTY`, `GENERAL` |
| `page`, `size` | int | |

Response body (200):

```json
{
  "totalElements": 4,
  "notifications": [
    {
      "id": "n1",
      "category": "PAYMENT",
      "title": "Detalle de pago de tu compra",
      "body": "Total a pagar 309.000 ARS. Medio de pago: Check Banco Nacion.",
      "read": false,
      "createdAt": "2026-04-20T18:45:00Z",
      "relatedResourceType": "PURCHASE",
      "relatedResourceId": "p1..."
    }
  ]
}
```

Codigos de estado:
- `200 OK`
- `401 Unauthorized`

### GET /api/v1/notifications/{notificationId}

Auth: Postor (dueno).

Codigos de estado:
- `200 OK`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

### POST /api/v1/notifications/{notificationId}/read

Marca una notificacion como leida.

Auth: Postor (dueno).

Codigos de estado:
- `204 No Content`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

### POST /api/v1/notifications/read-all

Marca todas las notificaciones como leidas.

Auth: Postor.

Codigos de estado:
- `204 No Content`
- `401 Unauthorized`

---

## 14. Metricas del usuario

Las metricas agregadas que la consigna pide explicitamente: participaciones, veces que gano, importes ofertados y pagados, desglose por categoria de subasta.

### GET /api/v1/users/me/metrics

Metricas resumidas.

Auth: Postor.

Response body (200):

```json
{
  "totalAuctionsAttended": 12,
  "totalAuctionsWon": 4,
  "totalAmountBid": 1250000.0,
  "totalAmountPaid": 820000.0,
  "currency": "ARS",
  "winRate": 0.3333
}
```

Codigos de estado:
- `200 OK`
- `401 Unauthorized`

### GET /api/v1/users/me/metrics/by-category

Metricas discriminadas por categoria de subasta.

Auth: Postor.

Response body (200):

```json
[
  {
    "category": "SILVER",
    "auctionsAttended": 7,
    "auctionsWon": 2,
    "amountBid": 450000.0,
    "amountPaid": 320000.0
  },
  {
    "category": "GOLD",
    "auctionsAttended": 3,
    "auctionsWon": 1,
    "amountBid": 700000.0,
    "amountPaid": 500000.0
  }
]
```

Codigos de estado:
- `200 OK`
- `401 Unauthorized`

### GET /api/v1/users/me/metrics/history

Historial completo de participaciones: lista de subastas asistidas, items sobre los que pujo y desenlace.

Auth: Postor.

Query params: `fromDate`, `toDate`, `page`, `size`.

Codigos de estado:
- `200 OK`
- `401 Unauthorized`

---

## 15. Datos de referencia

Listas auxiliares que la app movil necesita al arrancar.

### GET /api/v1/reference/categories

Categorias de postor y de subasta.

Auth: Publico.

Response body (200):

```json
[
  { "code": "COMMON", "label": "Comun" },
  { "code": "SPECIAL", "label": "Especial" },
  { "code": "SILVER", "label": "Plata" },
  { "code": "GOLD", "label": "Oro" },
  { "code": "PLATINUM", "label": "Platino" }
]
```

Codigos de estado:
- `200 OK`

### GET /api/v1/reference/currencies

Monedas soportadas por la plataforma.

Auth: Publico.

Response body (200):

```json
[
  { "code": "ARS", "label": "Peso argentino" },
  { "code": "USD", "label": "Dolar estadounidense" }
]
```

Codigos de estado:
- `200 OK`

### GET /api/v1/reference/countries

Codigos de pais soportados (usados en el registro).

Auth: Publico.

Response body (200):

```json
[
  { "code": "AR", "label": "Argentina" },
  { "code": "US", "label": "United States" }
]
```

Codigos de estado:
- `200 OK`

### GET /api/v1/reference/auction-states

Estados posibles de una subasta.

Auth: Publico.

Response body (200):

```json
[
  { "code": "SCHEDULED", "label": "Programada" },
  { "code": "OPEN", "label": "Abierta" },
  { "code": "CLOSED", "label": "Cerrada" },
  { "code": "CANCELLED", "label": "Cancelada" }
]
```

Codigos de estado:
- `200 OK`
