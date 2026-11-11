# Cómo ejecutar y probar la API Patent Sweep (Equipo 2)

Versión 1.0.0 del arquetipo — rama actual `develop-v2`. Curso 750021C Desarrollo de Software II, Universidad del Valle, 2026-2.

---

## 1. Qué trae esta versión

Esta versión es el **arquetipo (línea base)** del backend: la estructura del DAS V3.0 hecha código, con un ejemplo
transaccional completo y sus pruebas.

### Funcionalidad implementada

| Historia | Qué hace | Endpoint |
|---|---|---|
| Acceso (DA-11) | Login básico que emite un JWT; roles `ADMINISTRADOR` y `SERVICIO` | `POST /api/v1/auth/login` |
| **HU-02** Búsqueda manual | Consulta la fórmula en Materiales, busca patentes, detecta conflictos, guarda historial y estado de la fórmula | `POST /api/v1/patentes/busqueda` |
| **HU-12** Notificación (parcial) | Al detectar un conflicto nuevo envía la alerta a la API de Notificaciones en segundo plano, con 3 reintentos | interno (evento asíncrono) |
| Operación | Health check y métricas para Prometheus | `GET /actuator/health`, `GET /actuator/prometheus` |
| Documentación | Swagger UI y contrato OpenAPI | `/swagger-ui.html`, `/v3/api-docs` |

### Tecnología

Java 21, Spring Boot 3.5, arquitectura multicapa (controller, service, integration, repository/model, config/exception),
PostgreSQL 16 con migraciones Flyway, Spring Security con JWT, Resilience4j (reintentos y Circuit Breaker), Caffeine,
errores en formato ProblemDetail (RFC 9457) con `traceId`, Docker multi-etapa y Docker Compose.

### Calidad

- Pruebas automatizadas con JUnit 5 y Mockito, más una prueba de integración con PostgreSQL embebido.
- JaCoCo exige al menos 81 % de cobertura de líneas y ramas en los paquetes `service` y `service.event`.
- El workflow de GitHub Actions ejecuta pruebas, cobertura, Checkstyle y empaqueta el backend en cada PR a las ramas configuradas.

### Qué **no** trae todavía

HU-01 (barrido automático y `BarridoScheduler`), HU-03 a HU-11 (evaluación de fórmulas, estado, historial, conflictos y
configuración), integración de SonarCloud, despliegue automatizado y monitoreo con Grafana. Los demás detalles están en el `README.md`.

---

## 2. Requisitos

- Git
- Docker Desktop, o Docker Engine 24.0+ con Docker Compose v2.20+ (comando `docker compose`)

No hace falta instalar Java, Maven ni PostgreSQL para usar la API.

---

## 3. Ejecutar la API

```bash
git clone https://github.com/Inmemorialake/Desarrollo-II.git
cd Desarrollo-II
git checkout develop-v2
cd patent-sweep
cp .env.example .env
docker compose up --build
```

En PowerShell, en lugar de `cp` use: `Copy-Item .env.example .env`.

La primera vez tarda unos minutos porque compila el proyecto. Cuando el contenedor `api-patent-sweep` esté `healthy`
la API está lista. Se levantan tres servicios:

| Servicio | Para qué sirve |
|---|---|
| `postgres` | Base de datos PostgreSQL 16 (solo accesible dentro de la red de Docker) |
| `apis-simuladas` | WireMock que simula las APIs de Materiales/Fórmulas y Notificaciones de otros equipos |
| `api-patent-sweep` | La API Patent Sweep, expuesta en el puerto `API_PUERTO` |

Para detener: `docker compose down` (con `-v` también borra los datos de la base).

---

## 4. Archivo `.env` (datos de configuración)

Copie `.env.example` como `.env`. Estos son los valores que trae; funcionan tal cual para pruebas locales.
**Nunca suba el `.env` al repositorio** (ya está en el `.gitignore`).

```dotenv
# Perfil de Spring activo: dev (más logs) o prod.
SPRING_PROFILES_ACTIVE=dev

# Puerto en el que se expone la API. Se evita el 8080 por defecto (recomendación OWASP).
API_PUERTO=8085

# Base de datos PostgreSQL. DB_HOST es el nombre del servicio en docker-compose.yml.
DB_HOST=postgres
DB_PORT=5432
DB_NAME=patent_sweep
DB_USER=patent_sweep_usuario
# Cambie esta clave.
DB_PASSWORD=cambiar_clave_de_la_base_de_datos
# Máximo de conexiones del pool HikariCP.
DB_POOL_MAX=10

# Clave con la que se firman los JWT. Debe tener al menos 32 caracteres. Cámbiela.
JWT_SECRET=cambiar-por-un-secreto-aleatorio-de-al-menos-32-caracteres
# Minutos de vigencia del token de sesión.
JWT_EXPIRACION_MINUTOS=60

# Usuarios iniciales que se crean al arrancar (si la variable está vacía, el usuario no se crea).
ADMIN_USUARIO=admin
# Cambie esta clave.
ADMIN_CLAVE=cambiar_clave_del_administrador
SERVICIO_USUARIO=servicio
# Cambie esta clave.
SERVICIO_CLAVE=cambiar_clave_del_servicio

# URL base de las APIs de otros equipos. Por defecto apuntan a las APIs simuladas (WireMock).
MATERIALES_URL=http://apis-simuladas:8080
NOTIFICACIONES_URL=http://apis-simuladas:8080
FUENTE_PATENTES_URL=http://apis-simuladas:8080
```

| Variable | Qué controla |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring (`dev` o `prod`) |
| `API_PUERTO` | Puerto de la API (Swagger y endpoints se abren en ese puerto) |
| `DB_*` | Conexión a PostgreSQL (la base se crea con estos datos) |
| `JWT_SECRET`, `JWT_EXPIRACION_MINUTOS` | Firma y vigencia del token de sesión |
| `ADMIN_*`, `SERVICIO_*` | Usuarios iniciales: `admin` (rol ADMINISTRADOR) y `servicio` (rol SERVICIO) |
| `MATERIALES_URL`, `NOTIFICACIONES_URL`, `FUENTE_PATENTES_URL` | Dónde están las APIs externas (por defecto, la API simulada). Cámbielas por las reales cuando existan |

Si el puerto `8085` está ocupado, cambie `API_PUERTO` y use ese puerto en todas las URL de esta guía.
Si despliega en un servidor compartido, cambie siempre `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_CLAVE` y `SERVICIO_CLAVE`.

---

## 5. Probar desde Swagger

Abra **http://localhost:8085/swagger-ui.html**

1. En `POST /api/v1/auth/login` pulse *Try it out* y envíe:
   ```json
   { "usuario": "admin", "clave": "cambiar_clave_del_administrador" }
   ```
   (usuario y clave son `ADMIN_USUARIO` y `ADMIN_CLAVE` del `.env`). Copie el `token` de la respuesta.
2. Pulse **Authorize** (arriba a la derecha), pegue el token sin escribir "Bearer" y confirme.
3. En `POST /api/v1/patentes/busqueda` pruebe estos casos:

| `formulaId` | Resultado esperado |
|---|---|
| `form-001` | 200 con un conflicto: patente "Aleación para cadenas de corte" (`conflictoId` de 6 dígitos, desde 100000) |
| `form-002` | 200 con lista vacía y el mensaje "No se detectaron conflictos relacionados con su búsqueda" |
| `form-003` | 409: "No se puede buscar patentes para una fórmula DESCONTINUADA" |
| `form-999` | 404: "La fórmula consultada no fue encontrada" |
| `FORM-1` o vacío | 400: "El código de la fórmula es obligatorio y debe tener el formato form-NNN" |
| (sin token) | 401 |

Repetir `form-001` devuelve el mismo conflicto y no crea otro. La notificación se envía en segundo plano a la API
simulada; se puede ver en el log con `docker compose logs api-patent-sweep`.

### Alternativa con `curl`

```bash
TOKEN=$(curl -s -X POST http://localhost:8085/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usuario":"admin","clave":"cambiar_clave_del_administrador"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

curl -s -X POST http://localhost:8085/api/v1/patentes/busqueda \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"formulaId":"form-001"}'
```

En PowerShell use `curl.exe` y escriba el token manualmente en el encabezado `Authorization`.

---

## 6. Pruebas unitarias y cobertura (opcional)

Requiere JDK 21 y Maven 3.9 o superior (no requiere Docker):

```bash
cd backend
mvn verify
```

El reporte de cobertura queda en `backend/target/site/jacoco/index.html`. La compilación falla si la capa `service`
baja del 80 % de líneas o ramas.

---

## 7. Si algo falla

| Problema | Qué hacer |
|---|---|
| La API no queda `healthy` | `docker compose logs api-patent-sweep` y revise el error |
| Puerto ocupado | Cambie `API_PUERTO` en `.env` y vuelva a ejecutar `docker compose up` |
| Quiere empezar de cero | `docker compose down -v` y luego `docker compose up --build` |
| Error de clave JWT | `JWT_SECRET` debe tener al menos 32 caracteres |
| 401 en Swagger | El token venció (60 min) o no pulsó **Authorize**; vuelva a iniciar sesión |
