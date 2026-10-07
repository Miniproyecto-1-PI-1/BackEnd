# Agendo — Backend

API REST de **Agendo**, un organizador de eventos independientes: cada usuario crea sus eventos y lleva el control de las gestiones (tareas) que necesita para sacarlos adelante, con una vista "Hoy" que prioriza lo vencido, lo de hoy y lo próximo.

Proyecto del Miniproyecto 1 de Proyecto Integrador I (Universidad del Valle), desarrollado en equipo por sprints.

**Demo:** https://mini-proyecto-1-pi-1.inmemorialake.dev
**API:** https://backend-h0fc.onrender.com · **Swagger UI:** [`/swagger-ui.html`](https://backend-h0fc.onrender.com/swagger-ui.html) · **OpenAPI:** [`/v3/api-docs`](https://backend-h0fc.onrender.com/v3/api-docs)
**Frontend:** [Miniproyecto-1-PI-1/FrontEnd](https://github.com/Miniproyecto-1-PI-1/FrontEnd)

> **Nota:** el backend está desplegado en el plan gratuito de Render, que se suspende tras un rato sin tráfico. La primera petición puede tardar cerca de un minuto mientras el servidor arranca; después responde con normalidad.

## Stack

| Capa | Tecnología |
| --- | --- |
| Lenguaje y framework | Java 21, Spring Boot 4.1 (Spring Web MVC) |
| Seguridad | Spring Security + OAuth2 Resource Server, JWT HS256 propio, contraseñas con BCrypt |
| Persistencia | Spring Data JPA (Hibernate) sobre PostgreSQL en Supabase |
| Validación | Jakarta Bean Validation |
| Documentación | springdoc-openapi (OpenAPI 3.1 + Swagger UI) |
| Pruebas | JUnit 5, Spring Boot Test, Spring Security Test |
| Despliegue | Docker (build multi-etapa) en Render |

## Estructura

```
src/main/java/com/miniproyecto/backend/
├── controller/   Endpoints REST
├── service/      Lógica de negocio
├── repository/   Repositorios Spring Data JPA
├── entity/       Entidades JPA
├── dto/          Objetos de entrada y salida de la API
├── security/     Configuración de Spring Security y emisión/validación de JWT
├── config/       CORS, OpenAPI y soporte de barra final en las rutas
└── exception/    Manejo uniforme de errores
sql/              Migraciones manuales (ver más abajo)
```

## Ejecutar en local

Requisitos: Java 21 y una base de datos PostgreSQL (o el proyecto de Supabase).

1. Crea un archivo `.env` (o define las variables en tu entorno) con los valores de la sección [Variables de entorno](#variables-de-entorno).
2. Ejecuta las migraciones de `sql/` sobre la base de datos (ver [Migraciones manuales](#migraciones-manuales)).
3. Levanta el servidor:

```bash
./mvnw spring-boot:run          # con Maven Wrapper
# o bien
docker compose up --build       # con Docker; lee las variables de .env
```

La API queda en `http://localhost:8080` y la documentación en `http://localhost:8080/swagger-ui.html`.

Para correr las pruebas:

```bash
./mvnw test
```

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

Todas las rutas aceptan también la barra final (`/api/events/`). La referencia completa, con esquemas de entrada y salida, está en Swagger UI.

| Método | Ruta | Respuesta |
| --- | --- | --- |
| `GET` | `/api/health` | 200, `{ "status": "ok" }` (no requiere token) |
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
| `GET` | `/api/today?days=&incluirHechas=` | 200, gestiones vencidas/de hoy/próximas del usuario, en orden de prioridad |

`GET /api/today` reúne en una sola lista las gestiones no ejecutadas de todos los eventos del usuario, clasificadas en `OVERDUE` (vencidas), `TODAY` (hoy) o `UPCOMING` (próximas dentro de la ventana), ordenadas por fecha límite y, en caso de empate, por menor esfuerzo estimado. `days` (por defecto 7, máximo 60) define hasta cuántos días a futuro se incluyen en `UPCOMING`; valores fuera de rango se ajustan al límite más cercano. `incluirHechas=true` agrega también las gestiones con estado `DONE`, excluidas por defecto.

## Formato de errores

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

## Equipo

| Integrante | GitHub |
| --- | --- |
| Andrés Gerardo González Rosero | [@Inmemorialake](https://github.com/Inmemorialake) |
| Victoria Yuan Chen | [@ycvictoria](https://github.com/ycvictoria) |
| Freddy Alexander Melo Buitrago | [@Alexander-Motion](https://github.com/Alexander-Motion) |
| Andrés Felipe Narváez Bolaños | [@andres042](https://github.com/andres042) |
