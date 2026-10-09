# API — Información para la entrega del Sprint 2

Documento generado a partir del código del repositorio `Miniproyecto-1-PI-1/BackEnd` (rama `main`, commit `a80b97c`, 2026-10-02).
Todo lo que aparece aquí sale del código, del historial de git o de los README. Lo que no se pudo comprobar está marcado como **NO CONFIRMADO**.

Stack: Spring Boot 4.1.1, Java 21, Spring Security + OAuth2 Resource Server (JWT HS256 propio), Spring Data JPA sobre PostgreSQL (Supabase), springdoc-openapi 3.1.1, Jackson 3.

---

## 1. Cambios desde el Sprint 1

**Línea base del Sprint 1:** merge del PR #5 `feature/sprint1-cierre` (commit `8df4b01`, 2026-09-25).
Diferencia `8df4b01..HEAD`: 43 archivos, +1513 / −15 líneas.

### 1.1 Commits del Sprint 2

| Commit | Fecha | Contenido |
| --- | --- | --- |
| `b8031d5` | 2026-09-25 | Documentación Swagger/OpenAPI (springdoc 3.1.1), `@Tag`/`@Operation`, respuestas 400/404 documentadas |
| `3cb7371` | 2026-09-30 | Autenticación JWT, registro/login/me, perfil, contraseña, avatar, eliminar cuenta, aislamiento por usuario, CORS sin Netlify, migraciones 002 y 003 |
| `89894c1` | 2026-10-02 | Endpoint `GET /api/today` (gestiones vencidas, de hoy y próximas) — PR #6 |
| `302b6a7` | 2026-10-01 | `/api/today`: añade `description`, `startTime`, `endTime` y el parámetro `incluirHechas` — PR #8 |
| `a80b97c` | 2026-10-02 | "Solve supabase problem": pool de Hikari (`maximum-pool-size=4`, `minimum-idle=1`) |

### 1.2 Endpoints nuevos

| Método | Ruta | Controller |
| --- | --- | --- |
| `POST` | `/api/auth/register` | `AuthController.register` |
| `POST` | `/api/auth/login` | `AuthController.login` |
| `GET` | `/api/auth/me` | `AuthController.me` |
| `PUT` | `/api/users/me` | `UserController.updateProfile` |
| `PUT` | `/api/users/me/password` | `UserController.changePassword` |
| `PUT` | `/api/users/me/avatar` | `UserController.updateAvatar` |
| `DELETE` | `/api/users/me` | `UserController.deleteAccount` |
| `GET` | `/api/today` | `TodayController.today` |

Además (no son endpoints de la API, pero son rutas nuevas): Swagger UI `/swagger-ui.html` y especificación `/v3/api-docs`.

### 1.3 Endpoints modificados

Los 8 endpoints de `/api/events/**` existían en el Sprint 1 con las mismas rutas, firmas y DTOs. Cambios:

- **Ahora exigen autenticación** (`Authorization: Bearer <token>`). En el Sprint 1 no había seguridad.
- **Aislamiento por usuario:** en el Sprint 1 `CurrentUser.id()` devolvía siempre `1L` (usuario fijo); ahora devuelve el `sub` del JWT, así que cada usuario solo ve y modifica sus propios eventos/gestiones.
- Anotaciones de Swagger (`@Operation`, `@ApiResponse`); sin cambios de comportamiento.
- `GET /api/health`: solo se añadieron anotaciones de Swagger; sigue siendo público.

### 1.4 Endpoints eliminados

Ninguno (comparado `EventController`/`HealthController` en `8df4b01` contra `HEAD`).

### 1.5 Otros cambios relevantes

- `GlobalExceptionHandler`: nuevos manejadores para `FieldErrorException` (400), `UnauthorizedException` (401) y `ConflictException` (409). Ver §4.
- CORS: se quitó el origen `https://magenta-gumdrop-40d40f.netlify.app`.
- Entidad `AppUser`: nueva columna `avatar` (text). Requiere `sql/003_user_avatar.sql`; sin ella el backend no arranca (`ddl-auto=validate`).
- `sql/002_demo_user_password.sql`: asigna al usuario demo (id 1, `valentina@eventosvv.co`) la contraseña `valentina123` (hash BCrypt) y normaliza su correo.
- `application.properties`: `app.jwt.secret=${JWT_SECRET}`, `app.jwt.expiration=PT8H`, pool de Hikari.
- `docker-compose.yml` nuevo (puerto 8080, variables desde `.env`).
- Tests nuevos: `AuthServiceTest`, `UserServiceTest`, `TodayServiceTest`; `EventServiceTest` usa un `SecurityContext` con JWT.

### 1.6 Historias de usuario

| Historia | Fuente | Contenido |
| --- | --- | --- |
| US-01 | commit `a83e1b5` (Sprint 1) | Tipo de evento; crear evento |
| US-02 | commit `1e1d2f3` (Sprint 1) | Crear/editar/eliminar gestiones |
| US-03 | commits `1e1d2f3`, `a83e1b5` (Sprint 1) | Editar/eliminar evento y gestiones |
| TS-03 | commit `6e26d8e` (Sprint 1) | Formato de error uniforme |
| TS-02 | commit `0c27335` del repo FrontEnd (Sprint 1) | Usar el backend real por defecto |

**Sprint 2 — NO CONFIRMADO:** ningún commit, README ni archivo del repo del Sprint 2 (autenticación, cuenta de usuario, `/api/today`, Swagger) cita un identificador US-xx. No hay `gh` instalado para consultar issues de GitHub. Hay que tomar la correspondencia del backlog del equipo.
La única referencia adicional encontrada es un comentario en el prototipo del frontend (`FrontEnd/docs/prototype/index.html:1365`): `posponer con fecha explícita (US-08 + US-06)`. En el backend no existe un endpoint específico de "posponer"; solo puede hacerse con `PUT /api/events/{id}/tasks/{taskId}` enviando `status: "POSTPONED"` y/o una nueva `dueDate`. Que esto cubra US-06/US-08 está **NO CONFIRMADO**.

---

## 2. Endpoints

Convenciones comunes a todos los endpoints:

- Base URL: ver §6. Todas las rutas aceptan también la barra final (`/api/events/`), ver §6.4.
- Cuerpos en JSON (`Content-Type: application/json`).
- Todos los errores tienen la forma `ApiErrorResponse` (§4): `{ status, title, detail, errors }`.
- **Errores comunes a todo endpoint protegido** (no se repiten en cada tabla):
  - `401` `{"status":401,"title":"No autenticado","detail":"Inicia sesión para continuar","errors":{}}` — sin token, token inválido, mal firmado o vencido.
  - `401` `{"status":401,"title":"No autenticado","detail":"Sesión no válida","errors":{}}` — token válido cuyo `sub` no es numérico, o (en `/api/auth/me` y `/api/users/me/**`) cuyo usuario ya no existe.
- **Errores comunes a todo endpoint con cuerpo:**
  - `400` `{"status":400,"title":"Solicitud inválida","detail":"El cuerpo de la petición no es un JSON válido o tiene un formato incorrecto","errors":{}}` — JSON mal formado, cuerpo ausente, fecha/hora con formato incorrecto, enum desconocido, número inválido.
  - `400` de validación: `{"status":400,"title":"Solicitud inválida","detail":"Revisa los campos marcados","errors":{"<campo>":"<mensaje>"}}`. Si un campo tiene varias violaciones, solo se devuelve una (`putIfAbsent`); cuál de ellas es **NO CONFIRMADO** (el orden de Bean Validation no está garantizado).
- **Errores comunes a todo endpoint con path params `{id}`/`{taskId}`:**
  - `400` `{"status":400,"title":"Solicitud inválida","detail":"El parámetro 'id' tiene un valor inválido","errors":{}}` (o `'taskId'`) si no es un número.
- **Errores genéricos:** `404` "La ruta solicitada no existe", `405` "El método X no está soportado en esta ruta", `500` "Ha ocurrido un error interno" (ver §4).

---

### 2.1 Salud

#### `GET /api/health` — `HealthController.health`

- **Autenticación:** pública.
- **Parámetros:** ninguno.
- **Respuesta:** `200 OK`

```json
{ "status": "ok" }
```

- **Errores:** solo los genéricos (500).

---

### 2.2 Autenticación (`AuthController`, `/api/auth`)

#### `POST /api/auth/register` — `AuthController.register`

- **Autenticación:** pública.
- **Request DTO `RegisterRequest`:**

| Campo | Tipo | Validaciones | Mensaje |
| --- | --- | --- | --- |
| `name` | String | `@NotBlank` | `El nombre es obligatorio.` |
| `email` | String | `@NotBlank` | `El correo es obligatorio.` |
| | | `@Email` | `Ingresa un correo válido.` |
| `password` | String | `@NotBlank` | `La contraseña es obligatoria.` |
| | | `@Size(min = 6, max = 72)` | `La contraseña debe tener entre 6 y 72 caracteres.` |

- **Respuesta:** `201 Created` (sin cabecera `Location`), cuerpo `AuthResponse`.
- **Errores:**

| Código | title | detail | errors |
| --- | --- | --- | --- |
| 400 | Solicitud inválida | Revisa los campos marcados | campo → mensaje de la tabla |
| 400 | Solicitud inválida | El cuerpo de la petición no es un JSON válido o tiene un formato incorrecto | `{}` |
| 409 | Conflicto | Ese correo ya está registrado | `{}` |

Request:

```json
{ "name": "Camila Torres", "email": "Camila.Torres@Gmail.com ", "password": "fiesta2026" }
```

Respuesta `201`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3IiwiZW1haWwiOiJjYW1pbGEudG9ycmVzQGdtYWlsLmNvbSIsIm5hbWUiOiJDYW1pbGEgVG9ycmVzIiwiaWF0IjoxNzkxMDAwMDAwLCJleHAiOjE3OTEwMjg4MDB9.xxxxx",
  "user": { "id": 7, "name": "Camila Torres", "email": "camila.torres@gmail.com", "avatar": null }
}
```

> Nota: `@Email` se valida sobre el valor original, antes del `trim`. Si un correo con espacios al final pasa o no la validación `@Email` es **NO CONFIRMADO**.

#### `POST /api/auth/login` — `AuthController.login`

- **Autenticación:** pública.
- **Request DTO `LoginRequest`:**

| Campo | Tipo | Validaciones | Mensaje |
| --- | --- | --- | --- |
| `email` | String | `@NotBlank` | `El correo es obligatorio.` |
| `password` | String | `@NotBlank` | `La contraseña es obligatoria.` |

- **Respuesta:** `200 OK`, cuerpo `AuthResponse`.
- **Errores:**

| Código | title | detail | errors |
| --- | --- | --- | --- |
| 400 | Solicitud inválida | Revisa los campos marcados | campo → mensaje |
| 400 | Solicitud inválida | El cuerpo de la petición no es un JSON válido o tiene un formato incorrecto | `{}` |
| 401 | No autenticado | Correo o contraseña incorrectos | `{}` |

El mismo 401 se devuelve tanto si el correo no existe como si la contraseña no coincide.

Request:

```json
{ "email": "valentina@eventosvv.co", "password": "valentina123" }
```

Respuesta `200`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJ2YWxlbnRpbmFAZXZlbnRvc3Z2LmNvIiwibmFtZSI6IlZhbGVudGluYSIsImlhdCI6MTc5MTAwMDAwMCwiZXhwIjoxNzkxMDI4ODAwfQ.xxxxx",
  "user": { "id": 1, "name": "Valentina", "email": "valentina@eventosvv.co", "avatar": null }
}
```

(El nombre real del usuario demo en la base de datos es **NO CONFIRMADO**.)

#### `GET /api/auth/me` — `AuthController.me`

- **Autenticación:** requiere Bearer.
- **Parámetros:** ninguno.
- **Respuesta:** `200 OK`, cuerpo `UserResponse`.
- **Errores:** 401 comunes; 401 `Sesión no válida` si el usuario del token ya no existe.

Respuesta `200`:

```json
{ "id": 1, "name": "Valentina", "email": "valentina@eventosvv.co", "avatar": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ..." }
```

---

### 2.3 Usuario (`UserController`, `/api/users/me`)

Todos requieren Bearer. Si el usuario del token ya no existe: 401 `Sesión no válida`.

#### `PUT /api/users/me` — `UserController.updateProfile`

- **Request DTO `UpdateProfileRequest`:**

| Campo | Tipo | Validaciones | Mensaje |
| --- | --- | --- | --- |
| `name` | String | `@NotBlank` | `El nombre es obligatorio.` |
| | | `@Size(max = 100)` | `El nombre no puede superar 100 caracteres.` |
| `email` | String | `@NotBlank` | `El correo es obligatorio.` |
| | | `@Email` | `Ingresa un correo válido.` |
| `currentPassword` | String | ninguna (obligatoria solo si cambia el correo) | — |

- **Respuesta:** `200 OK`, cuerpo `UserResponse` (no se emite un token nuevo; el claim `email` del token actual queda desactualizado si cambió el correo).
- **Errores:**

| Código | title | detail | errors | Cuándo |
| --- | --- | --- | --- | --- |
| 400 | Solicitud inválida | Revisa los campos marcados | campo → mensaje | Bean Validation |
| 400 | Solicitud inválida | Revisa los campos marcados | `{"currentPassword":"Ingresa tu contraseña para cambiar el correo."}` | Cambia el correo y `currentPassword` es null o vacía |
| 400 | Solicitud inválida | Revisa los campos marcados | `{"currentPassword":"La contraseña actual no es correcta."}` | Cambia el correo y la contraseña no coincide |
| 409 | Conflicto | Ese correo ya está registrado | `{}` | El nuevo correo pertenece a otro usuario |

Request:

```json
{ "name": "Valentina Vélez", "email": "valentina.velez@eventosvv.co", "currentPassword": "valentina123" }
```

Respuesta `200`:

```json
{ "id": 1, "name": "Valentina Vélez", "email": "valentina.velez@eventosvv.co", "avatar": null }
```

#### `PUT /api/users/me/password` — `UserController.changePassword`

- **Request DTO `ChangePasswordRequest`:**

| Campo | Tipo | Validaciones | Mensaje |
| --- | --- | --- | --- |
| `currentPassword` | String | `@NotBlank` | `La contraseña actual es obligatoria.` |
| `newPassword` | String | `@NotBlank` | `La nueva contraseña es obligatoria.` |
| | | `@Size(min = 6, max = 72)` | `La contraseña debe tener entre 6 y 72 caracteres.` |

- **Respuesta:** `204 No Content`, sin cuerpo.
- **Errores:**

| Código | title | detail | errors | Cuándo |
| --- | --- | --- | --- | --- |
| 400 | Solicitud inválida | Revisa los campos marcados | campo → mensaje | Bean Validation |
| 400 | Solicitud inválida | Revisa los campos marcados | `{"currentPassword":"La contraseña actual no es correcta."}` | Contraseña actual incorrecta (se comprueba primero) |
| 400 | Solicitud inválida | Revisa los campos marcados | `{"newPassword":"La nueva contraseña debe ser distinta a la actual."}` | Nueva igual a la actual |

Request:

```json
{ "currentPassword": "valentina123", "newPassword": "bodas&fiestas26" }
```

Respuesta: `204` sin cuerpo. Los tokens emitidos antes siguen siendo válidos hasta que expiren (no hay revocación).

#### `PUT /api/users/me/avatar` — `UserController.updateAvatar`

- **Request DTO `AvatarRequest`:**

| Campo | Tipo | Validaciones | Mensaje |
| --- | --- | --- | --- |
| `image` | String (data URL) | `@NotBlank` | `La imagen es obligatoria.` |

- **Respuesta:** `200 OK`, cuerpo `UserResponse` con el nuevo `avatar`.
- **Errores:**

| Código | title | detail | errors | Cuándo |
| --- | --- | --- | --- | --- |
| 400 | Solicitud inválida | Revisa los campos marcados | `{"image":"La imagen es obligatoria."}` | Vacía |
| 400 | Solicitud inválida | Revisa los campos marcados | `{"image":"La imagen es demasiado grande."}` | Más de 400 000 caracteres tras el `trim` (se comprueba primero) |
| 400 | Solicitud inválida | Revisa los campos marcados | `{"image":"El archivo debe ser una imagen JPG, PNG o WebP."}` | No cumple `^data:image/(jpeg\|png\|webp);base64,[A-Za-z0-9+/]+={0,2}$` |

Request:

```json
{ "image": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/wAALCAABAAEBAREA/8QAFAABAAAAAAAAAAAAAAAAAAAACf/EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAD8AKp//2Q==" }
```

Respuesta `200`:

```json
{ "id": 1, "name": "Valentina", "email": "valentina@eventosvv.co", "avatar": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBD..." }
```

No existe endpoint para quitar la foto (volver a `null`).

#### `DELETE /api/users/me` — `UserController.deleteAccount`

- **Request DTO `DeleteAccountRequest`** (sí lleva cuerpo, aunque sea un DELETE):

| Campo | Tipo | Validaciones | Mensaje |
| --- | --- | --- | --- |
| `password` | String | `@NotBlank` | `La contraseña es obligatoria.` |

- **Respuesta:** `204 No Content`.
- **Errores:**

| Código | title | detail | errors | Cuándo |
| --- | --- | --- | --- | --- |
| 400 | Solicitud inválida | Revisa los campos marcados | `{"password":"La contraseña es obligatoria."}` | Vacía |
| 400 | Solicitud inválida | Revisa los campos marcados | `{"password":"La contraseña actual no es correcta."}` | No coincide |
| 400 | Solicitud inválida | El cuerpo de la petición no es un JSON válido o tiene un formato incorrecto | `{}` | Sin cuerpo |

Request:

```json
{ "password": "valentina123" }
```

Respuesta: `204` sin cuerpo. Borra los eventos (y sus gestiones por cascada), los clientes y el usuario.

---

### 2.4 Eventos (`EventController`, `/api/events`)

Todos requieren Bearer. "Evento no encontrado" se devuelve tanto si el id no existe como si pertenece a otro usuario.

#### `POST /api/events` — `EventController.create`

- **Request DTO `CreateEventRequest`:**

| Campo | Tipo | Obligatorio | Validaciones | Mensaje |
| --- | --- | --- | --- | --- |
| `name` | String | sí | `@NotBlank` | `El nombre es obligatorio.` |
| `type` | String | no | `@Size(max = 50)` | `El tipo no puede superar 50 caracteres.` |
| `date` | LocalDate `yyyy-MM-dd` | sí | `@NotNull` | `La fecha es obligatoria.` |
| `time` | LocalTime `HH:mm` | no (por defecto `00:00`) | `@JsonFormat(pattern = "HH:mm")` | — |
| `place` | String | sí | `@NotBlank` | `El lugar es obligatorio.` |
| `description` | String | no | — | — |
| `client` | `ClientRequest` | no | `@Valid` (ClientRequest no tiene restricciones) | — |
| `tasks` | `List<TaskRequest>` | no | `@Valid` en la lista y en cada elemento | ver `TaskRequest` |

`ClientRequest`: `name` (String), `phone` (String), `email` (String), sin validaciones.

`TaskRequest`:

| Campo | Tipo | Obligatorio | Validaciones | Mensaje |
| --- | --- | --- | --- | --- |
| `name` | String | sí | `@NotBlank` | `El nombre de la gestión es obligatorio.` |
| `description` | String | no | — | — |
| `dueDate` | LocalDate `yyyy-MM-dd` | no (al crear: fecha actual del servidor) | — | — |
| `startTime` | LocalTime `HH:mm` | no | `@JsonFormat(pattern = "HH:mm")` | — |
| `endTime` | LocalTime `HH:mm` | no | `@JsonFormat(pattern = "HH:mm")` | — |
| `estimatedHours` | BigDecimal | no | `@Positive` | `Las horas estimadas deben ser mayores que 0.` |
| `status` | `TaskStatus` | no (ignorado al crear) | — | — |
| *(`timeRangeValid`)* | validación de clase | — | `@AssertTrue`: ambas horas nulas, o ambas presentes y fin > inicio | `La hora de fin debe ser posterior a la de inicio.` |

En los errores de validación, los campos anidados se devuelven con la ruta completa, por ejemplo `tasks[0].name` o `tasks[1].timeRangeValid` (por `FieldError.getField()`; la ruta exacta con índice es **NO CONFIRMADO**, no hay test).

- **Respuesta:** `201 Created`, cabecera `Location: /api/events/{id}` (ruta relativa), cuerpo `EventDetailResponse`.
- **Errores:**

| Código | title | detail | errors |
| --- | --- | --- | --- |
| 400 | Solicitud inválida | Revisa los campos marcados | campo → mensaje |
| 400 | Solicitud inválida | El cuerpo de la petición no es un JSON válido o tiene un formato incorrecto | `{}` |
| 404 | Recurso no encontrado | Usuario no encontrado | `{}` (token válido de un usuario ya eliminado) |

Request:

```json
{
  "name": "Boda Andrea y Julián",
  "type": "Boda",
  "date": "2026-11-21",
  "time": "17:30",
  "place": "Hacienda El Paraíso, Cali",
  "description": "Ceremonia al aire libre y recepción para 120 invitados.",
  "client": { "name": "Andrea Muñoz", "phone": "3104567890", "email": "andrea.munoz@gmail.com" },
  "tasks": [
    { "name": "Confirmar catering", "dueDate": "2026-10-10", "estimatedHours": 2 },
    { "name": "Prueba de sonido", "dueDate": "2026-11-20", "startTime": "15:00", "endTime": "16:30" }
  ]
}
```

Respuesta `201` (`Location: /api/events/42`):

```json
{
  "id": 42,
  "name": "Boda Andrea y Julián",
  "type": "Boda",
  "description": "Ceremonia al aire libre y recepción para 120 invitados.",
  "date": "2026-11-21",
  "time": "17:30",
  "place": "Hacienda El Paraíso, Cali",
  "client": { "id": 9, "name": "Andrea Muñoz", "phone": "3104567890", "email": "andrea.munoz@gmail.com" },
  "tasks": [
    { "id": 101, "name": "Confirmar catering", "description": null, "dueDate": "2026-10-10", "startTime": null, "endTime": null, "estimatedHours": 2.00, "status": "PENDING" },
    { "id": 102, "name": "Prueba de sonido", "description": null, "dueDate": "2026-11-20", "startTime": "15:00", "endTime": "16:30", "estimatedHours": 1.50, "status": "PENDING" }
  ],
  "totalTasks": 2,
  "doneTasks": 0,
  "progress": 0
}
```

#### `GET /api/events` — `EventController.list`

- **Query params:**

| Nombre | Tipo | Obligatorio | Por defecto | Descripción |
| --- | --- | --- | --- | --- |
| `q` | String | no | — | Filtra por nombre del evento, sin distinguir mayúsculas, "contiene". Se hace `trim`; vacío = sin filtro |

- **Respuesta:** `200 OK`, `List<EventSummaryResponse>` ordenada por fecha de creación descendente (más reciente primero). Lista vacía si no hay resultados.
- **Errores:** solo los comunes (401).

Request: `GET /api/events?q=boda`

Respuesta `200`:

```json
[
  { "id": 42, "name": "Boda Andrea y Julián", "type": "Boda", "date": "2026-11-21", "clientName": "Andrea Muñoz", "totalTasks": 4, "doneTasks": 1, "progress": 25 },
  { "id": 17, "name": "Boda Sara y Mateo", "type": null, "date": "2026-10-18", "clientName": null, "totalTasks": 0, "doneTasks": 0, "progress": 0 }
]
```

#### `GET /api/events/{id}` — `EventController.getById`

- **Path params:** `id` (Long, obligatorio).
- **Respuesta:** `200 OK`, `EventDetailResponse` (misma forma que en el `POST`). El orden de `tasks` no está definido explícitamente en el código (no hay `@OrderBy` ni `ORDER BY`): **NO CONFIRMADO**.
- **Errores:**

| Código | title | detail |
| --- | --- | --- |
| 400 | Solicitud inválida | El parámetro 'id' tiene un valor inválido |
| 404 | Recurso no encontrado | Evento no encontrado |

Request: `GET /api/events/42` → respuesta igual al ejemplo del `POST`.

#### `PUT /api/events/{id}` — `EventController.update`

- **Path params:** `id` (Long, obligatorio).
- **Request DTO `UpdateEventRequest`:** mismos campos y validaciones que `CreateEventRequest` **sin** `tasks` (`name`, `type`, `date`, `time`, `place`, `description`, `client`). Reemplaza todos los datos; las gestiones no se tocan.
- **Respuesta:** `200 OK`, `EventDetailResponse`.
- **Errores:**

| Código | title | detail | errors |
| --- | --- | --- | --- |
| 400 | Solicitud inválida | Revisa los campos marcados | campo → mensaje |
| 400 | Solicitud inválida | El cuerpo de la petición no es un JSON válido o tiene un formato incorrecto | `{}` |
| 400 | Solicitud inválida | El parámetro 'id' tiene un valor inválido | `{}` |
| 404 | Recurso no encontrado | Evento no encontrado | `{}` |

Request:

```json
{
  "name": "Boda Andrea y Julián",
  "type": "Boda",
  "date": "2026-11-28",
  "time": "18:00",
  "place": "Hacienda El Paraíso, Cali",
  "description": "Se movió una semana por disponibilidad del lugar.",
  "client": { "name": "Andrea Muñoz" }
}
```

Respuesta `200`: `EventDetailResponse` con los datos nuevos y las mismas `tasks`.

#### `DELETE /api/events/{id}` — `EventController.delete`

- **Path params:** `id` (Long, obligatorio).
- **Respuesta:** `204 No Content`. Borra el evento y sus gestiones (cascade). El cliente no se borra.
- **Errores:** 400 `El parámetro 'id' tiene un valor inválido`; 404 `Evento no encontrado`.

#### `POST /api/events/{id}/tasks` — `EventController.addTask`

- **Path params:** `id` (Long, obligatorio).
- **Request DTO:** `TaskRequest` (tabla de arriba). `status` se ignora: la gestión nueva siempre empieza en `PENDING`.
- **Respuesta:** `201 Created` (sin `Location`), cuerpo: el `EventDetailResponse` completo del evento actualizado.
- **Errores:**

| Código | title | detail | errors |
| --- | --- | --- | --- |
| 400 | Solicitud inválida | Revisa los campos marcados | p. ej. `{"name":"El nombre de la gestión es obligatorio."}`, `{"estimatedHours":"Las horas estimadas deben ser mayores que 0."}`, `{"timeRangeValid":"La hora de fin debe ser posterior a la de inicio."}` |
| 400 | Solicitud inválida | El cuerpo de la petición no es un JSON válido o tiene un formato incorrecto | `{}` |
| 400 | Solicitud inválida | El parámetro 'id' tiene un valor inválido | `{}` |
| 404 | Recurso no encontrado | Evento no encontrado | `{}` |

Request:

```json
{ "name": "Reservar fotógrafo", "description": "Paquete de 6 horas", "dueDate": "2026-10-15", "startTime": "09:00", "endTime": "11:00" }
```

Respuesta `201`: `EventDetailResponse` con la nueva gestión (`estimatedHours: 2.00`, `status: "PENDING"`) añadida a `tasks` y `totalTasks`/`progress` recalculados.

#### `PUT /api/events/{id}/tasks/{taskId}` — `EventController.updateTask`

- **Path params:** `id` (Long), `taskId` (Long), ambos obligatorios.
- **Request DTO:** `TaskRequest`. Semántica de cada campo al editar (ver §5.3): `name`, `description`, `startTime`, `endTime` se reemplazan (null borra); `dueDate` y `status` solo cambian si vienen; `estimatedHours` se recalcula si no viene.
- **Respuesta:** `200 OK`, `EventDetailResponse` del evento actualizado.
- **Errores:**

| Código | title | detail |
| --- | --- | --- |
| 400 | Solicitud inválida | Revisa los campos marcados (con `errors`) |
| 400 | Solicitud inválida | El cuerpo de la petición no es un JSON válido o tiene un formato incorrecto (incluye `status` con un valor fuera del enum) |
| 400 | Solicitud inválida | El parámetro 'id' tiene un valor inválido / El parámetro 'taskId' tiene un valor inválido |
| 404 | Recurso no encontrado | Evento no encontrado |
| 404 | Recurso no encontrado | Gestión no encontrada (la gestión no existe o no pertenece a ese evento) |

Request (marcar como hecha):

```json
{ "name": "Reservar fotógrafo", "description": "Paquete de 6 horas", "startTime": "09:00", "endTime": "11:00", "status": "DONE" }
```

Respuesta `200`: `EventDetailResponse` con esa gestión en `"status": "DONE"` y `doneTasks`/`progress` actualizados.

#### `DELETE /api/events/{id}/tasks/{taskId}` — `EventController.deleteTask`

- **Path params:** `id` (Long), `taskId` (Long).
- **Respuesta:** `204 No Content`.
- **Errores:** 400 por parámetros no numéricos; 404 `Evento no encontrado`; 404 `Gestión no encontrada`.

---

### 2.5 Hoy (`TodayController`, `/api/today`)

#### `GET /api/today` — `TodayController.today`

- **Autenticación:** requiere Bearer.
- **Query params:**

| Nombre | Tipo | Obligatorio | Por defecto | Descripción |
| --- | --- | --- | --- | --- |
| `days` | Integer | no | `7` | Ventana para `UPCOMING`, en días. Se ajusta a `[0, 60]` (negativos → 0, > 60 → 60) |
| `incluirHechas` | Boolean | no | `false` | Si es `true`, incluye también gestiones `DONE` |

- **Respuesta:** `200 OK`, `TodayResponse`.
- **Errores:**

| Código | title | detail |
| --- | --- | --- |
| 400 | Solicitud inválida | El parámetro 'days' tiene un valor inválido |
| 400 | Solicitud inválida | El parámetro 'incluirHechas' tiene un valor inválido |

Request: `GET /api/today?days=7`

Respuesta `200` (suponiendo que hoy es 2026-10-02):

```json
{
  "today": "2026-10-02",
  "overdueCount": 1,
  "todayCount": 1,
  "upcomingCount": 1,
  "tasks": [
    {
      "id": 88, "name": "Enviar cotización de flores", "description": null, "status": "POSTPONED",
      "estimatedHours": 0.50, "dueDate": "2026-09-29", "startTime": null, "endTime": null,
      "category": "OVERDUE", "daysFromToday": -3,
      "eventId": 17, "eventName": "Boda Sara y Mateo", "clientName": null
    },
    {
      "id": 95, "name": "Llamar al DJ", "description": "Confirmar lista de canciones", "status": "PENDING",
      "estimatedHours": 1.00, "dueDate": "2026-10-02", "startTime": "10:00:00", "endTime": "11:00:00",
      "category": "TODAY", "daysFromToday": 0,
      "eventId": 42, "eventName": "Boda Andrea y Julián", "clientName": "Andrea Muñoz"
    },
    {
      "id": 101, "name": "Confirmar catering", "description": null, "status": "PENDING",
      "estimatedHours": 2.00, "dueDate": "2026-10-08", "startTime": null, "endTime": null,
      "category": "UPCOMING", "daysFromToday": 6,
      "eventId": 42, "eventName": "Boda Andrea y Julián", "clientName": "Andrea Muñoz"
    }
  ]
}
```

> Ojo: en esta respuesta `startTime`/`endTime` salen como `"HH:mm:ss"` (no tienen `@JsonFormat`), a diferencia de `TaskResponse` en `/api/events`, que usa `"HH:mm"`. Ver §7.

---

## 3. Autenticación

### 3.1 Mecanismo

- **No es JWT de Supabase.** Supabase solo se usa como base de datos PostgreSQL. Los tokens los emite y valida el propio backend.
- **Spring Security** (`SecurityConfig`) con **OAuth2 Resource Server** (`oauth2ResourceServer().jwt()`), sin filtro propio.
- Tokens **JWT HS256** firmados con la clave simétrica `JWT_SECRET` (`app.jwt.secret`). Si la clave tiene menos de 32 bytes, la aplicación no arranca (`IllegalStateException: JWT_SECRET debe tener al menos 32 bytes`).
- Emisión: `TokenService.issue` (en `register` y `login`). Claims:
  - `sub`: id del usuario (como string, p. ej. `"7"`)
  - `email`, `name`
  - `iat`, `exp` (= `iat` + `app.jwt.expiration` = `PT8H`, 8 horas)
- Sesión `STATELESS`, CSRF deshabilitado. Contraseñas con `BCryptPasswordEncoder`.
- No hay refresh token, logout en servidor ni revocación: un token es válido hasta su `exp` aunque se cambie la contraseña.

### 3.2 Validación del token

- `NimbusJwtDecoder.withSecretKey(key).macAlgorithm(HS256)`: verifica firma HS256 y los validadores por defecto de Spring Security (`exp`/`nbf`, con la tolerancia de reloj por defecto de 60 s). No se valida `iss` ni `aud`.
- El token se lee de la cabecera `Authorization: Bearer <token>` (resolver por defecto).

### 3.3 Id del usuario

`CurrentUser.id()` toma el `Jwt` del `SecurityContextHolder` y hace `Long.parseLong(jwt.getSubject())`. Si no hay JWT o el `sub` no es numérico → `UnauthorizedException("Sesión no válida")` → 401. Todos los services filtran por ese id (§5).

### 3.4 Rutas públicas

| Ruta | Motivo |
| --- | --- |
| `OPTIONS /**` | Preflight CORS |
| `/api/auth/register`, `/api/auth/login` | Autenticación |
| `/api/health` | Salud |
| `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | Swagger |

Todo lo demás: `authenticated()`.

> **NO CONFIRMADO (comportamiento estándar de Spring Security, no probado):** si una petición a una ruta pública lleva una cabecera `Authorization: Bearer` con un token inválido o vencido, el filtro de Resource Server intenta autenticarla igualmente y responde 401. El frontend no debería enviar el token viejo en `login`/`register`.
>
> **NO CONFIRMADO:** el filtro de barra final (`UrlHandlerFilter`) es un bean de filtro normal y probablemente se ejecuta después de la cadena de seguridad, así que `/api/auth/login/` (con barra) podría no coincidir con el `permitAll` y devolver 401.

### 3.5 Cuerpo de los errores 401 / 403

401 desde la cadena de seguridad (sin token, token mal formado, firma inválida, vencido) — `AuthenticationEntryPoint` propio:

```json
{ "status": 401, "title": "No autenticado", "detail": "Inicia sesión para continuar", "errors": {} }
```

`Content-Type: application/json`, UTF-8. No se envía cabecera `WWW-Authenticate` (el entry point propio reemplaza al de Spring).

401 desde el código (vía `GlobalExceptionHandler`):

```json
{ "status": 401, "title": "No autenticado", "detail": "Sesión no válida", "errors": {} }
```

```json
{ "status": 401, "title": "No autenticado", "detail": "Correo o contraseña incorrectos", "errors": {} }
```

**403:** no hay roles, `AccessDeniedHandler` propio ni reglas que puedan negar acceso a un usuario autenticado, y CSRF está deshabilitado. En la práctica la API **no devuelve 403**. Si llegara a producirse, el cuerpo sería el por defecto de Spring Security, no `ApiErrorResponse` (**NO CONFIRMADO**).

---

## 4. Manejo de errores (`GlobalExceptionHandler`)

Cuerpo único `ApiErrorResponse`:

```json
{ "status": 400, "title": "Solicitud inválida", "detail": "Revisa los campos marcados", "errors": { "name": "El nombre es obligatorio." } }
```

`errors` es un mapa `campo → mensaje`; solo tiene contenido en los 400 de validación y de `FieldErrorException`; en el resto es `{}`.

| Excepción | Código | title | detail | Nuevo en Sprint 2 |
| --- | --- | --- | --- | --- |
| `MethodArgumentNotValidException` | 400 | Solicitud inválida | Revisa los campos marcados | |
| `FieldErrorException` | 400 | Solicitud inválida | Revisa los campos marcados (`errors` con un solo campo) | **Sí** |
| `HttpMessageNotReadableException` | 400 | Solicitud inválida | El cuerpo de la petición no es un JSON válido o tiene un formato incorrecto | |
| `MethodArgumentTypeMismatchException` | 400 | Solicitud inválida | El parámetro '`<nombre>`' tiene un valor inválido | |
| `UnauthorizedException` | 401 | No autenticado | mensaje de la excepción (`Sesión no válida` / `Correo o contraseña incorrectos`) | **Sí** |
| `NotFoundException` | 404 | Recurso no encontrado | mensaje (`Evento no encontrado` / `Gestión no encontrada` / `Usuario no encontrado`) | |
| `NoResourceFoundException` | 404 | Recurso no encontrado | La ruta solicitada no existe | |
| `HttpRequestMethodNotSupportedException` | 405 | Método no permitido | El método `<MÉTODO>` no está soportado en esta ruta | |
| `ConflictException` | 409 | Conflicto | mensaje (`Ese correo ya está registrado`) | **Sí** |
| `Exception` (resto) | 500 | Error interno | Ha ocurrido un error interno | |

Además, fuera del handler: el `AuthenticationEntryPoint` de §3.5 (401 "Inicia sesión para continuar"), también nuevo en Sprint 2.

Observaciones (deducidas del código, no probadas):

- Una petición con `Content-Type` distinto de JSON (`HttpMediaTypeNotSupportedException`) no tiene manejador propio y cae en el genérico → **500** "Ha ocurrido un error interno".
- Un registro simultáneo con el mismo correo que pase la comprobación `existsByEmail` chocaría con la restricción `unique` → 500.

---

## 5. Reglas de negocio

### 5.1 Aislamiento por usuario

- Eventos: `findDetailByIdAndUserId` / `findSummariesByUserId` filtran por `CurrentUser.id()`. Un evento de otro usuario responde **404 "Evento no encontrado"** (no 403).
- Gestiones: se buscan dentro de la colección del evento ya filtrado por usuario; si no pertenece → 404 "Gestión no encontrada".
- Clientes: se buscan por `user_id` + nombre.
- `/api/today`: `WHERE e.user.id = :userId`.
- Usuario: `/api/users/me` y `/api/auth/me` solo operan sobre el usuario del token.

### 5.2 Autenticación y usuario (`AuthService`, `UserService`)

- Correo normalizado: `trim()` + `toLowerCase(Locale.ROOT)` en registro, login y cambio de perfil.
- Nombre: `trim()` en registro y perfil.
- Registro: hash BCrypt, `lastLoginAt = now`, `dailyHourLimit = 6` por defecto (no se expone en la API). Correo duplicado → 409.
- Login: actualiza `lastLoginAt`.
- Perfil: si el correo normalizado es igual al actual, no se pide contraseña; si cambia, `currentPassword` es obligatoria y debe coincidir, y el correo no puede pertenecer a otro usuario.
- Cambio de contraseña: primero se verifica la actual, luego que la nueva sea distinta.
- Avatar: `trim()`, máximo 400 000 caracteres (comentario: "unos 300 KB de base64"), solo `data:image/jpeg|png|webp;base64,...`. Se guarda tal cual en `app_user.avatar` (text).
- Eliminar cuenta: verifica contraseña; borra eventos (gestiones por cascada), clientes y el usuario.

### 5.3 Eventos y gestiones (`EventService`, `ClientService`)

- Evento: `name` y `place` con `trim()`; `type` vacío/en blanco → `null`, si no `trim()`; `description` se guarda tal cual (sin trim); `time` ausente → `00:00` (`LocalTime.MIDNIGHT`).
- `PUT /api/events/{id}` reemplaza todo, incluido el cliente: si `client` es `null` o su `name` está en blanco, el evento queda **sin cliente**; `description` ausente → `null`.
- Cliente (`findOrCreate`): busca un cliente del usuario con el mismo nombre (sin distinguir mayúsculas, tras `trim`). Si existe, lo reutiliza y actualiza `phone`/`email` solo si vienen con texto (afecta a todos los eventos que comparten ese cliente). Si no existe, lo crea. Los clientes no se borran al borrar un evento.
- Gestión nueva: `name` con `trim()`, `status = PENDING` siempre, `dueDate` ausente → `LocalDate.now()` (zona horaria del servidor).
- `estimatedHours`:
  1. Si viene, se redondea a 2 decimales (`HALF_UP`).
  2. Si no, se calcula del horario: `(endTime − startTime)` en minutos / 60, 2 decimales `HALF_UP` (p. ej. 15:00–16:30 → `1.50`).
  3. Si no hay horario completo → `1.00`.
- Editar gestión: `name` (trim), `description`, `startTime`, `endTime` se reemplazan (null los borra); `estimatedHours` se recalcula con las reglas anteriores si no viene; `dueDate` y `status` solo se cambian si vienen no nulos.
- Progreso: `progress = round(doneTasks × 100 / totalTasks)`; `0` si no hay gestiones. Solo cuenta `DONE` (`POSTPONED` no cuenta como hecha).
- Listado: búsqueda `q` con `trim`, `LOWER(name) LIKE %q%` (los caracteres `%` y `_` de `q` no se escapan); orden `createdAt DESC`.

### 5.4 Hoy (`TodayService`, `TaskRepository.findTodayTasks`)

- `today = LocalDate.now()` del servidor (no hay zona horaria configurada; la del servidor en producción es **NO CONFIRMADO**).
- Ventana: `days` null → 7; si no, `max(0, min(days, 60))`.
- Filtro: gestiones de eventos del usuario con `dueDate <= today + ventana`; sin límite inferior (todas las vencidas aparecen). Excluye `DONE` salvo `incluirHechas=true`. `POSTPONED` se incluye siempre.
- Orden: `dueDate ASC`, luego `estimatedHours ASC` (menor esfuerzo primero), luego `id ASC`.
- Categoría por `daysFromToday = días entre today y dueDate`: `< 0` → `OVERDUE`, `= 0` → `TODAY`, `> 0` → `UPCOMING`.
- Contadores `overdueCount`, `todayCount`, `upcomingCount` = número de elementos de `tasks` en cada categoría (incluyen las `DONE` si `incluirHechas=true`).

---

## 6. Configuración

### 6.1 Base URL

- Local: `http://localhost:8080` (`server.port=${PORT:8080}`; `docker-compose.yml` publica `8080:8080`). Es también el valor de `VITE_API_URL` en `FrontEnd/.env.example`.
- Producción: **NO CONFIRMADO.** El commit `5bc9ee2` ("Add dockerfile for Render Deploy") sugiere Render, pero la URL no aparece en ningún archivo del repo.
- No hay `server.servlet.context-path`; todas las rutas empiezan en `/api`.

### 6.2 CORS (`CorsConfig`)

- Mapeo: `/api/**`.
- Orígenes permitidos: `http://localhost:5173`, `https://mini-proyecto-1-pi-1.inmemorialake.dev`.
- Métodos: `GET, POST, PUT, PATCH, DELETE, OPTIONS`.
- Cabeceras permitidas: valor por defecto de Spring (todas). Credenciales: no habilitadas (no hacen falta, el token va en cabecera).
- **No se exponen cabeceras** (`exposedHeaders` no está configurado): desde el navegador en otro origen **no se puede leer `Location`** del `POST /api/events`; el frontend debe usar el `id` del cuerpo.
- Spring Security usa esta configuración vía `.cors(Customizer.withDefaults())` y deja pasar `OPTIONS /**` sin token.

### 6.3 Swagger / OpenAPI (`OpenApiConfig`)

- Swagger UI: `/swagger-ui.html` (redirige a `/swagger-ui/index.html`).
- Especificación: `/v3/api-docs`.
- Ambas públicas.
- **Esquema de seguridad: sí está configurado.** `components.securitySchemes.bearerAuth` = `type: http`, `scheme: bearer`, `bearerFormat: JWT`, y se añade como requisito global (`addSecurityItem(bearerAuth)`), así que aparece el botón **Authorize** y el candado en todos los endpoints. `register`, `login` y `health` llevan `@SecurityRequirements` vacío, por lo que salen sin candado.
- `OperationCustomizer` (`errorResponses`): a cada operación le añade, si no las declara ya con `@ApiResponse`:
  - `400` si tiene cuerpo, path params o query params;
  - `401` si la ruta exige token (todas menos las que llevan `@SecurityRequirements` vacío);
  - `404` ("Evento o gestión no encontrados") si tiene path params.

  Además, a toda respuesta 4xx/5xx le asigna el esquema `ApiErrorResponse`, también a las declaradas a mano.
- Cada endpoint declara con `@ApiResponse` sus errores específicos: 401 de `login` y `me`, 409 de `register` y `PUT /api/users/me`, 400 de negocio de `/api/users/me/**` y 404 "El usuario del token ya no existe" en `POST /api/events`. El 201 de `POST /api/events` documenta la cabecera `Location`.
- Los DTOs tienen `@Schema` con descripción y ejemplo en cada campo. Las horas se documentan como `string` con formato `HH:mm` (`HH:mm:ss` en `TodayTaskResponse`). `/api/today` muestra el valor por defecto y el rango de `days` y el valor por defecto de `incluirHechas`.
- Versión: "Sprint 2". La descripción indica que `/api/auth/register`, `/api/auth/login` y `/api/health` son públicas.
- Comprobado levantando la app y leyendo `/v3/api-docs` (2026-10-02). No se documentan los 405 ni los 500 genéricos. No se comprobó en el navegador si Swagger UI muestra el cuerpo de `DELETE /api/users/me` (la especificación sí incluye `requestBody`).

### 6.4 Otros

- `TrailingSlashConfig`: `UrlHandlerFilter` para `/api/**`, `/api/events/` se atiende igual que `/api/events` (ver la salvedad de §3.4).
- Base de datos: `DB_URL`, `DB_USER`, `DB_PASSWORD`; `ddl-auto=validate` (el esquema se migra a mano con `sql/001..003`); `open-in-view=false`; Hikari `maximum-pool-size=4`, `minimum-idle=1`.
- Variables de entorno obligatorias: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (≥ 32 bytes). Opcional: `PORT`. Se puede usar un `.env` en la raíz (`spring.config.import=optional:file:.env[.properties]`).

---

## 7. Formatos de datos

| Dato | Formato | Dónde |
| --- | --- | --- |
| Fecha (`LocalDate`) | `yyyy-MM-dd` (ISO), p. ej. `"2026-11-21"` | `date`, `dueDate`, `today` |
| Hora del evento y de gestiones en `/api/events` (`LocalTime` con `@JsonFormat(pattern = "HH:mm")`) | `"HH:mm"`, p. ej. `"17:30"` | `time` (request y `EventDetailResponse`), `startTime`/`endTime` (`TaskRequest`, `TaskResponse`) |
| Hora en `/api/today` (`TodayTaskResponse`, sin `@JsonFormat`) | `"HH:mm:ss"`, p. ej. `"10:00:00"` | `startTime`, `endTime` |
| Horas estimadas (`BigDecimal`, escala 2) | número JSON, p. ej. `1.50`, `2.00` | `estimatedHours` |
| Ids | número entero (`Long`) | todos los `id` |
| Progreso | entero 0–100 | `progress` |
| Avatar | data URL `data:image/(jpeg\|png\|webp);base64,...` o `null` | `avatar` |
| Token | JWT compacto (`xxx.yyy.zzz`) | `token` |

Sobre las horas:

- El formato `"HH:mm:ss"` de `/api/today` se comprobó serializando un `LocalTime` con Jackson 3.1.5 con la configuración por defecto (`{"t":"08:00:00"}`); no se probó con la aplicación levantada, así que la configuración de Jackson de Spring Boot podría cambiarlo (**NO CONFIRMADO** contra la app real).
- Si los campos con `@JsonFormat(pattern = "HH:mm")` aceptan también `"HH:mm:ss"` en el request es **NO CONFIRMADO**; enviar siempre `"HH:mm"`.

Enums:

| Enum | Valores | Uso |
| --- | --- | --- |
| `TaskStatus` | `PENDING`, `DONE`, `POSTPONED` | `status` en `TaskRequest`, `TaskResponse`, `TodayTaskResponse`. Se serializa por nombre (`@Enumerated(STRING)` en BD). Un valor distinto en el request → 400 "El cuerpo de la petición no es un JSON válido o tiene un formato incorrecto" |
| `TodayCategory` | `OVERDUE`, `TODAY`, `UPCOMING` | `category` en `TodayTaskResponse` (solo salida) |

`type` del evento **no es un enum**: es un String libre de hasta 50 caracteres. El script `sql/001_event_type.sql` menciona como valores previstos "Boda, Social, Corporativo, Cumpleaños, Otro", pero el backend no los restringe.

### DTOs de respuesta (resumen)

- `AuthResponse`: `{ token: string, user: UserResponse }`
- `UserResponse`: `{ id, name, email, avatar }`
- `ClientResponse`: `{ id, name, phone, email }`
- `TaskResponse`: `{ id, name, description, dueDate, startTime, endTime, estimatedHours, status }`
- `EventDetailResponse`: `{ id, name, type, description, date, time, place, client: ClientResponse|null, tasks: TaskResponse[], totalTasks, doneTasks, progress }`
- `EventSummaryResponse`: `{ id, name, type, date, clientName, totalTasks, doneTasks, progress }`
- `TodayResponse`: `{ today, overdueCount, todayCount, upcomingCount, tasks: TodayTaskResponse[] }`
- `TodayTaskResponse`: `{ id, name, description, status, estimatedHours, dueDate, startTime, endTime, category, daysFromToday, eventId, eventName, clientName }` (`daysFromToday` negativo = días de atraso)
- `ApiErrorResponse`: `{ status, title, detail, errors }`

Los campos `null` se serializan como `null` (no hay `@JsonInclude(NON_NULL)`).
