# Agendo — Backend

API REST de **Agendo**, un organizador de eventos independientes: cada usuario crea sus eventos y lleva el control de las gestiones (tareas) que necesita para sacarlos adelante, con una vista "Hoy" que prioriza lo vencido, lo de hoy y lo próximo.

Proyecto del Miniproyecto 1 de Proyecto Integrador I (Universidad del Valle), desarrollado en equipo por sprints.

**Demo:** https://mini-proyecto-1-pi-1.inmemorialake.dev
**API:** https://backend-h0fc.onrender.com · **Swagger UI:** [`/swagger-ui.html`](https://backend-h0fc.onrender.com/swagger-ui.html) · **OpenAPI:** [`/v3/api-docs`](https://backend-h0fc.onrender.com/v3/api-docs)
**Frontend:** [Miniproyecto-1-PI-1/FrontEnd](https://github.com/Miniproyecto-1-PI-1/FrontEnd)

> **Cuenta demo:** `danna@gmail.com` / `Danna.2026` — ya tiene eventos y tareas de prueba (vencidas, de hoy, próximas, pospuestas y hechas).

> **Nota:** el backend está desplegado en el plan gratuito de Render, que se suspende tras un rato sin tráfico. La primera petición puede tardar cerca de un minuto mientras el servidor arranca; después responde con normalidad.

## Cómo probar la API

1. Despierta el servidor abriendo https://backend-h0fc.onrender.com/api/health hasta que responda `{"status":"ok"}`.
2. Abre Swagger UI: https://backend-h0fc.onrender.com/swagger-ui.html
3. En **Autenticación** → `POST /api/auth/login` pulsa **Try it out**, envía este cuerpo y copia el `token` de la respuesta:

   ```json
   { "email": "danna@gmail.com", "password": "Danna.2026" }
   ```

4. Pulsa **Authorize** (arriba a la derecha), pega el token **sin** el prefijo `Bearer` y confirma.
5. Ya puedes llamar las rutas protegidas, por ejemplo:
   - `GET /api/today`: tareas vencidas, de hoy y próximas.
   - `GET /api/progress`: avance general y por evento.
   - `PATCH /api/events/{id}/tasks/{taskId}/status` con `{ "status": "DONE" }`: marca una tarea como hecha.
   - `PUT /api/events/{id}/tasks/{taskId}` con una `dueDate` en un día que ya está lleno: responde **409** «Conflicto de sobrecarga» con el objeto `overload` (ver [Límite diario y sobrecarga](#endpoints)).

Con `curl`:

```bash
API=https://backend-h0fc.onrender.com
TOKEN=$(curl -s -X POST $API/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"danna@gmail.com","password":"Danna.2026"}' | jq -r .token)
curl -s $API/api/events -H "Authorization: Bearer $TOKEN" | jq
```

Si vas a modificar datos, mejor crea tu propia cuenta con `POST /api/auth/register` para no alterar la cuenta demo.

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

La cuenta demo de producción es `danna@gmail.com` / `Danna.2026`. En una base de datos nueva, `sql/002_demo_user_password.sql` deja además un usuario semilla (id 1, `valentina@eventosvv.co` / `valentina123`).

## Migraciones manuales

`spring.jpa.hibernate.ddl-auto=validate`: el esquema no se crea solo. Antes de desplegar una versión que agregue columnas, ejecuta en Supabase los scripts de `sql/` en orden:

| Script | Cambio |
| --- | --- |
| `sql/001_event_type.sql` | Columna `event.type` (tipo de evento, opcional) |
| `sql/002_demo_user_password.sql` | Contraseña BCrypt del usuario semilla (id 1) |
| `sql/003_user_avatar.sql` | Columna `app_user.avatar` (foto de perfil). **Sin ella el backend no arranca** |
| `sql/004_daily_hour_limit.sql` | Columna `app_user.daily_hour_limit` (límite diario de horas, 6 por defecto, rango 1–16) |
| `sql/005_task_note_completed_at.sql` | Columnas `task.note` (nota opcional) y `task.completed_at` (fecha de ejecución). **Sin ellas el backend no arranca** |

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
| `PATCH` | `/api/events/{id}/tasks/{taskId}/status` | 200, evento actualizado: marcar hecha, posponer (con nota opcional) o reabrir |
| `DELETE` | `/api/events/{id}/tasks/{taskId}` | 204 |
| `GET` | `/api/events/{id}/progress` | 200, avance del evento |
| `GET` | `/api/progress` | 200, avance general y por evento |
| `GET` | `/api/today?days=&incluirHechas=` | 200, gestiones vencidas/de hoy/próximas del usuario, en orden de prioridad |

**Límite diario y sobrecarga.** Al crear un evento con gestiones, añadir una gestión (`POST .../tasks`) o editarla (`PUT .../tasks/{taskId}`, también para reprogramarla) cuando cambian la fecha, las horas o se reabre una gestión hecha, el backend suma las horas no ejecutadas de ese día y las compara con el límite del usuario (`/api/users/me/planning-preferences`, 6 h por defecto). Si lo supera responde **409** con el formato de error común (`status`, `title: "Conflicto de sobrecarga"`, `detail` con las cifras —«Quedarías con 7h planificadas ese día (tu límite es 6h).»—, `errors: { "dueDate": "..." }`) y un objeto `overload` con `date`, `plannedHours`, `taskHours`, `resultingHours`, `limitHours`, `exceedsBy` (excedente), `availableHours` (horas que sí caben ese día) y hasta tres `suggestedDates`, cada una con su `date` y las `availableHours` libres ese día, donde la gestión sí cabe (vacío al crear un evento nuevo). No se guarda nada mientras haya sobrecarga; se resuelve repitiendo la petición con otra fecha o menos horas. Una fecha nueva anterior a hoy o posterior a la fecha del evento en `PUT` responde 400. `GET /api/today` incluye `eventDate` en cada gestión para que el cliente acote la reprogramación. Marcar una gestión como hecha o cambiar solo nombre o descripción no se revisa. **Cambiar el límite** (`PUT /api/users/me/planning-preferences`) responde **409** con `detail` («No puedes bajar el límite a 4h porque hay 1 día con más de 4h planificadas (el 12 de octubre tiene 5h).») si algún día desde hoy ya supera el nuevo valor, y no lo guarda; se resuelve moviendo o reduciendo gestiones de ese día.

**Registrar avance (`PATCH /api/events/{id}/tasks/{taskId}/status`).** Cambia el estado de una gestión y guarda una nota opcional (máx. 500 caracteres) dentro de la propia gestión (`note`). No exige reenviar el resto de campos.

| Estado | Qué hace |
|---|---|
| `DONE` | Marca la gestión como ejecutada, conserva su nota y registra `completedAt`. No revisa el límite diario. |
| `POSTPONED` | Guarda la nota (si viene) y mueve la gestión: a `dueDate` si viene (mismas reglas que reprogramar: no antes de hoy, no después del evento y, si el día se pasa del límite, 409 con `overload`) o, si no viene, a la **próxima fecha libre** antes del evento, contando desde el día siguiente a su fecha actual (o desde mañana si ya pasó), así que posponer nunca la adelanta. Si ningún día tiene espacio responde 409 sin cambiar nada. |
| `PENDING` | Reabre la gestión: borra `completedAt` y revisa el límite del día. |

Si `note` no viene, la nota existente se conserva; si viene vacía, se borra.

```
PATCH /api/events/42/tasks/101/status
{ "status": "POSTPONED", "note": "Esperando confirmación del salón" }

200 → el evento completo; la gestión ahora trae:
{ "id": 101, "name": "Confirmar catering", "dueDate": "2026-10-16", "status": "POSTPONED",
  "note": "Esperando confirmación del salón", "completedAt": null, ... }
```

Errores: `400` (estado ausente, nota de más de 500 caracteres o fecha inválida, con el formato `{status, title, detail, errors}`), `404` (el evento o la gestión no son del usuario) y `409` (sobrecarga con el objeto `overload`, o no hay un día libre antes del evento).

**Progreso (`GET /api/progress` y `GET /api/events/{id}/progress`).** Cuenta las gestiones en categorías que no se solapan: `done` (ejecutadas), `overdue` (no ejecutadas con fecha anterior a hoy), `postponed` (pospuestas con fecha de hoy o futura) y `pending` (el resto); `total` es su suma y `percent = done * 100 / total` redondeado (0 si no hay gestiones). El general trae además `events` con el mismo desglose por evento, ordenado por fecha.

```
GET /api/progress
{ "total": 8, "done": 3, "pending": 2, "postponed": 1, "overdue": 2, "percent": 38,
  "events": [ { "id": 42, "name": "Boda Andrea y Julián", "date": "2026-11-20",
                "total": 8, "done": 3, "pending": 2, "postponed": 1, "overdue": 2, "percent": 38 } ] }
```

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
