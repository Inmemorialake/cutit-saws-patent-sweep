# Cómo ejecutar y probar la API Patent Sweep

Backend Patent Sweep, versión `v1.0.0-beta.1` (alcance actual: Sprint 1; ver [`docs/progress.md`](../docs/progress.md)).
Curso 750021C Desarrollo de Software II, Universidad del Valle, 2026-2.

---

## 1. Qué trae esta versión

| Funcionalidad | Qué hace | Endpoint |
|---|---|---|
| Iniciar sesión | Valida un usuario existente y devuelve un JWT Bearer | `POST /api/v1/auth/login` |
| Interfaz web | Login, «Patentes > Búsqueda Manual» y «Patentes > Estado de Barrido» | `http://localhost:8085/` |
| Buscar conflictos (HU-02) | Ejecuta una búsqueda manual para una fórmula; requiere rol `ADMINISTRADOR` | `POST /api/v1/patentes/busqueda` |
| Consultar estado (HU-04) | Calcula el estado actual de una fórmula y sus alertas activas | `GET /api/v1/patentes/estado/{formulaId}` |
| Barrido automático (HU-01) | Revisa las fórmulas aprobadas 30 s después de arrancar y luego cada 24 h (`BARRIDO_AL_INICIAR`, `BARRIDO_RETRASO_INICIAL`) | Sin endpoint; se ve en Grafana |
| Operación | Health check público; Prometheus consulta métricas por el puerto interno de management | `GET /actuator/health`; `http://api-patent-sweep:9091/actuator/prometheus` (solo red Docker) |
| Monitoreo | Tableros de la API y del servidor en Grafana | `http://localhost:3000` |
| Documentación | Swagger UI y contrato OpenAPI | `/swagger-ui.html`, `/v3/api-docs` |

**Tecnología:** Java 21, Spring Boot 3.5, arquitectura multicapa, PostgreSQL 16 con Flyway, errores ProblemDetail
(RFC 9457) con `traceId`, Docker multi-etapa y Docker Compose.

**Qué no trae todavía:** registro o administración de usuarios ni las historias HU-05 a HU-12. El acceso a los
endpoints protegidos requiere un JWT obtenido mediante el login.

---

## 2. Requisitos

- Git
- Docker Desktop, o Docker Engine 24+ con Docker Compose v2.24+ (comando `docker compose`)

No hace falta instalar Java, Maven ni PostgreSQL para usar la API.

---

## 3. Ejecutar la API

```bash
git clone https://github.com/Inmemorialake/cutit-saws-patent-sweep.git
cd cutit-saws-patent-sweep/patent-sweep
cp .env.example .env
docker compose up -d --build --wait
```

En PowerShell, en lugar de `cp` use `Copy-Item .env.example .env`.

La primera vez tarda unos minutos porque compila el proyecto. Cuando `--wait` termina, la API está lista. Se levantan cinco servicios:

| Servicio | Para qué sirve |
|---|---|
| `postgres` | Base de datos PostgreSQL 16 (solo accesible dentro de la red de Docker) |
| `apis-simuladas` | WireMock con respuestas simuladas de Materiales/Fórmulas y de la fuente de patentes para HU-01/HU-02 |
| `api-patent-sweep` | La API Patent Sweep, expuesta en el puerto `API_PUERTO` |
| `prometheus` | Recolecta métricas de la API por la red interna de Docker; no publica puertos |
| `grafana` | Muestra los tableros en `http://localhost:3000`; requiere las credenciales de Grafana del `.env` |

`node-exporter` se inicia aparte con el perfil `servidor` y está pensado para Linux/EC2, no para Docker Desktop.

Para detener: `docker compose down` (con `-v` también borra los datos de la base).

---

## 4. Archivo `.env`

Copie `.env.example` como `.env`. Los valores que trae funcionan tal cual para pruebas locales.
**Nunca suba el `.env` al repositorio** (está en el `.gitignore`).

| Variable | Qué controla |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring: `dev` (más logs) o `prod` |
| `API_PUERTO` | Puerto de la API (8085 por defecto; se evita el 8080, por recomendación de OWASP) |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Conexión a PostgreSQL (la base se crea con estos datos) |
| `DB_POOL_MAX` | Máximo de conexiones del pool HikariCP |
| `JWT_SECRET`, `JWT_EXPIRACION_MINUTOS` | Firma y duración del JWT (60 minutos por defecto) |
| `ADMIN_USUARIO`, `ADMIN_CLAVE` | Usuario administrador inicial para el demo |
| `SERVICIO_USUARIO`, `SERVICIO_CLAVE` | Cuenta opcional para integraciones; no se usa en la GUI |
| `MANAGEMENT_PUERTO` | Puerto interno de Actuator para Prometheus (9091; no se publica y debe ser distinto de `API_PUERTO`) |
| `GRAFANA_ADMIN_USUARIO`, `GRAFANA_ADMIN_CLAVE` | Usuario administrador de Grafana |
| `API_DOMINIO`, `GRAFANA_DOMINIO` | Solo producción: subdominios que Caddy sirve por HTTPS |

Si el puerto `8085` está ocupado, cambie `API_PUERTO` y use ese puerto en todas las URL de esta guía.
Las cuentas se crean en PostgreSQL al iniciar la aplicación si no existen. En un `.env` de demo basta con la
cuenta `ADMINISTRADOR`; para no crear la cuenta técnica, deje vacías ambas variables `SERVICIO_*`. Cambiar una
contraseña en `.env` no actualiza la contraseña de un usuario ya creado en la base de datos.

---

## 5. Probar desde Swagger

Abra **http://localhost:8085/swagger-ui.html**, despliegue el endpoint y pulse *Try it out*.
Primero ejecute el login. Copie `accessToken` de la respuesta, pulse **Authorize** y pegue el token; Swagger
agregará `Bearer` al usar endpoints protegidos.

### `POST /api/v1/auth/login`

```json
{
  "username": "admin",
  "password": "<ADMIN_CLAVE configurada en .env>"
}
```

Respuesta `200 OK`:

```json
{
  "accessToken": "<JWT>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "username": "admin",
  "role": "ADMINISTRADOR"
}
```

Credenciales inválidas devuelven `401` con `ProblemDetail`; campos vacíos devuelven `400`.

### Datos de prueba (APIs simuladas)

Docker Compose levanta WireMock con los mocks de `mocks/mappings` (los mismos de producción). Fórmulas disponibles:

| Fórmula | Estado en Materiales | Búsqueda manual (HU-02) | Estado tras buscar o tras el barrido (HU-04) |
|---|---|---|---|
| `form-001` | `APROBADA` | **200** con 2 conflictos | `CONFLICTO_DETECTADO` con 2 alertas |
| `form-002` | `APROBADA` | **200** con 1 conflicto | `CONFLICTO_DETECTADO` con 1 alerta |
| `form-004` | `APROBADA` | **200** sin conflictos: «No se detectaron conflictos relacionados con su búsqueda» | `LIBRE` |
| `form-003` | `DESCONTINUADA` | **409** «No se puede buscar patentes para una fórmula DESCONTINUADA» | `NO_PROCESADO` |
| `form-999` | No existe | **404** «La fórmula consultada no fue encontrada» | **404** |

El listado de aprobadas que usa el barrido (`GET /formulas?estado=APROBADA&pagina=1`) devuelve `form-001`,
`form-002` y `form-004`; la página 2 viene vacía. Un barrido con estos datos termina `COMPLETADO` con 2 fórmulas
con conflicto y 1 sin conflicto. `MocksDelRepositorioIntegracionTest` lo comprueba en cada CI.

### `POST /api/v1/patentes/busqueda`

| Cuerpo | Resultado esperado |
|---|---|
| `{"formulaId":"form-001"}` | **200** con los conflictos de la tabla anterior |
| `{"formulaId":"abc"}` | **400** «El código de la fórmula es obligatorio y debe tener el formato form-NNN» |
| Sin token | **401**; con un usuario `SERVICIO`, **403** |

### `GET /api/v1/patentes/estado/{formulaId}`

| `formulaId` | Resultado esperado |
|---|---|
| `form-001` (después de buscarla) | **200** `CONFLICTO_DETECTADO` con `alertasActivas` |
| `form-004` (después de buscarla) | **200** `LIBRE` con `alertasActivas: []` |
| `abc` | **400** «El identificador de la fórmula no es válido» |

Todos los errores vienen en formato ProblemDetail con un `traceId`, que también aparece en el log
(`docker compose logs api-patent-sweep`).

### Alternativa con `curl`

```bash
curl -s -X POST http://localhost:8085/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"<ADMIN_CLAVE>"}'

# Copie accessToken de la respuesta en la variable TOKEN.
TOKEN="<JWT>"

curl -s -X POST http://localhost:8085/api/v1/patentes/busqueda \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"formulaId":"form-001"}'

curl -s http://localhost:8085/api/v1/patentes/estado/form-001 \
  -H "Authorization: Bearer $TOKEN"
```

En PowerShell use `curl.exe` y escape las comillas del JSON, o use `Invoke-RestMethod`.

---

## 6. Pruebas unitarias y cobertura (opcional)

Requiere JDK 21 y Maven 3.9 o superior (no requiere Docker):

```bash
cd backend
mvn clean verify
```

Ejecuta las pruebas, Checkstyle y JaCoCo. El reporte de cobertura queda en `backend/target/site/jacoco/index.html`.
La compilación falla si `service` o `exception` bajan del 80 % de líneas o ramas.

---

## 7. Si algo falla

| Problema | Qué hacer |
|---|---|
| La API no queda `healthy` | `docker compose logs api-patent-sweep` y revise el error |
| Puerto ocupado | Cambie `API_PUERTO` en `.env` y vuelva a ejecutar `docker compose up -d --wait` |
| Quiere empezar de cero | `docker compose down -v` y luego `docker compose up -d --build --wait` |
| `failed to add the host <=> sandbox pair interfaces: operation not supported` (Linux) | El kernel se actualizó y no se ha reiniciado: reinicie el equipo |
