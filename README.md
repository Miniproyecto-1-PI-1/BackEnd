# Backend

Backend del Miniproyecto 1 del curso Proyecto Integrador I.

## Variables de entorno

| Variable | Descripción |
| --- | --- |
| `DB_URL` | JDBC URL de PostgreSQL (Supabase), por ejemplo `jdbc:postgresql://host:5432/postgres` |
| `DB_USER` | Usuario de la base de datos |
| `DB_PASSWORD` | Contraseña de la base de datos |
| `PORT` | Puerto HTTP (por defecto `8080`) |

No guardes credenciales reales en el repositorio. Usa variables de entorno o un archivo local ignorado (`.env` / `application-local.properties`).

## Usuario fijo (id 1)

El backend usa por ahora el usuario con `id = 1`. Créalo en Supabase si aún no existe:

```sql
INSERT INTO app_user (name, email, password_hash)
VALUES ('Valentina', 'valentina@eventosvv.co', 'demo');
```

Ese insert debe dejar el registro con `id` 1 (tabla vacía o secuencia en 1).

## Migraciones manuales

`spring.jpa.hibernate.ddl-auto=validate`: el esquema no se crea solo. Antes de desplegar una versión que agregue columnas, ejecuta en Supabase los scripts de `sql/` en orden:

| Script | Cambio |
| --- | --- |
| `sql/001_event_type.sql` | Columna `event.type` (tipo de evento, opcional) |

## Endpoints

Todas las rutas aceptan también la barra final (`/api/events/`).

| Método | Ruta | Respuesta |
| --- | --- | --- |
| `GET` | `/api/events?q=` | 200, lista de eventos con progreso |
| `POST` | `/api/events` | 201 + `Location`, evento con sus gestiones |
| `GET` | `/api/events/{id}` | 200, detalle con gestiones |
| `PUT` | `/api/events/{id}` | 200, evento actualizado (no toca las gestiones) |
| `DELETE` | `/api/events/{id}` | 204, borra el evento y sus gestiones |
| `POST` | `/api/events/{id}/tasks` | 201, evento actualizado |
| `PUT` | `/api/events/{id}/tasks/{taskId}` | 200, evento actualizado |
| `DELETE` | `/api/events/{id}/tasks/{taskId}` | 204 |

Todos los errores (400, 404, 405, 500) tienen la misma forma:

```json
{
  "status": 400,
  "title": "Solicitud inválida",
  "detail": "Revisa los campos marcados",
  "errors": { "name": "El nombre es obligatorio." }
}
```

`errors` solo trae contenido en los 400 de validación; en el resto es `{}`.
