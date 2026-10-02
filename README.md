# Backend

Backend del Miniproyecto 1 del curso Proyecto Integrador I.

## Variables de entorno

| Variable | Descripción |
| --- | --- |
| `DB_URL` | JDBC URL de PostgreSQL (Supabase), por ejemplo `jdbc:postgresql://host:5432/postgres` |
| `DB_USER` | Usuario de la base de datos |
| `DB_PASSWORD` | Contraseña de la base de datos |
| `PORT` | Puerto HTTP (por defecto `8080`) |
| `JWT_SECRET` | Clave para firmar los JWT (HS256), mínimo 32 caracteres. Genera una con `openssl rand -base64 48` |

No guardes credenciales reales en el repositorio. Usa variables de entorno o un archivo local ignorado (`.env` / `application-local.properties`).

## Autenticación

Registro e inicio de sesión devuelven un JWT (HS256, válido 8 horas) con el id del usuario como `sub`.
Salvo `/api/auth/register`, `/api/auth/login`, `/api/health` y Swagger, todas las rutas exigen la cabecera:

```
Authorization: Bearer <token>
```

Cada usuario solo ve y modifica sus propios eventos. Sin token, o con un token inválido o vencido, la respuesta es 401.

El usuario demo (id 1, `valentina@eventosvv.co`) entra con la contraseña `valentina123` tras ejecutar `sql/002_demo_user_password.sql`.

## Migraciones manuales

`spring.jpa.hibernate.ddl-auto=validate`: el esquema no se crea solo. Antes de desplegar una versión que agregue columnas, ejecuta en Supabase los scripts de `sql/` en orden:

| Script | Cambio |
| --- | --- |
| `sql/001_event_type.sql` | Columna `event.type` (tipo de evento, opcional) |
| `sql/002_demo_user_password.sql` | Contraseña BCrypt del usuario demo (id 1) |
| `sql/003_user_avatar.sql` | Columna `app_user.avatar` (foto de perfil). **Sin ella el backend no arranca** |

## Endpoints

Todas las rutas aceptan también la barra final (`/api/events/`).

| Método | Ruta | Respuesta |
| --- | --- | --- |
| `POST` | `/api/auth/register` | 201, `{ token, user }`; 409 si el correo ya existe |
| `POST` | `/api/auth/login` | 200, `{ token, user }`; 401 si las credenciales no coinciden |
| `GET` | `/api/auth/me` | 200, `{ id, name, email, avatar }` del token actual |
| `PUT` | `/api/users/me` | 200, usuario actualizado. Cambiar el correo exige `currentPassword`; 409 si el correo ya existe |
| `PUT` | `/api/users/me/password` | 204. Cuerpo `{ currentPassword, newPassword }` |
| `PUT` | `/api/users/me/avatar` | 200, usuario actualizado. Cuerpo `{ image }` con data URL JPG/PNG/WebP (máx. ~300 KB) |
| `DELETE` | `/api/users/me` | 204. Cuerpo `{ password }`; borra la cuenta con sus eventos, gestiones y clientes |
| `GET` | `/api/events?q=` | 200, lista de eventos con progreso |
| `POST` | `/api/events` | 201 + `Location`, evento con sus gestiones |
| `GET` | `/api/events/{id}` | 200, detalle con gestiones |
| `PUT` | `/api/events/{id}` | 200, evento actualizado (no toca las gestiones) |
| `DELETE` | `/api/events/{id}` | 204, borra el evento y sus gestiones |
| `POST` | `/api/events/{id}/tasks` | 201, evento actualizado |
| `PUT` | `/api/events/{id}/tasks/{taskId}` | 200, evento actualizado |
| `DELETE` | `/api/events/{id}/tasks/{taskId}` | 204 |

Todos los errores (400, 401, 404, 405, 409, 500) tienen la misma forma:

```json
{
  "status": 400,
  "title": "Solicitud inválida",
  "detail": "Revisa los campos marcados",
  "errors": { "name": "El nombre es obligatorio." }
}
```

`errors` solo trae contenido en los 400 (por ejemplo `{ "currentPassword": "La contraseña actual no es correcta." }`); en el resto es `{}`.
