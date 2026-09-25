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
