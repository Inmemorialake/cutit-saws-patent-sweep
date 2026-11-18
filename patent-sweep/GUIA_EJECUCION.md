# Cómo ejecutar y probar la API Patent Sweep

Arquetipo Sprint 0 (versión 1.0.0), rama `main`. Curso 750021C Desarrollo de Software II, Universidad del Valle, 2026-2.

---

## 1. Qué trae esta versión

| Funcionalidad | Qué hace | Endpoint |
|---|---|---|
| Registrar patente | Valida los datos, verifica que el código no exista y guarda la patente en estado `EN_TRAMITE` | `POST /api/v1/patentes/busqueda` |
| Consultar patente | Devuelve la patente y su estado de vigencia | `GET /api/v1/patentes/estado/{id}` |
| Operación | Health check y métricas para Prometheus | `GET /actuator/health`, `GET /actuator/prometheus` |
| Documentación | Swagger UI y contrato OpenAPI | `/swagger-ui.html`, `/v3/api-docs` |

**Tecnología:** Java 21, Spring Boot 3.5, arquitectura multicapa, PostgreSQL 16 con Flyway, errores ProblemDetail
(RFC 9457) con `traceId`, Docker multi-etapa y Docker Compose.

**Qué no trae todavía:** las historias HU-01 a HU-12 (barrido, búsqueda de conflictos, estado de fórmulas, historial,
configuración y notificaciones) ni la autenticación JWT. Empiezan en el Sprint 1 (ver ADR-006).

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

La primera vez tarda unos minutos porque compila el proyecto. Cuando `--wait` termina, la API está lista. Se levantan tres servicios:

| Servicio | Para qué sirve |
|---|---|
| `postgres` | Base de datos PostgreSQL 16 (solo accesible dentro de la red de Docker) |
| `apis-simuladas` | WireMock con las APIs simuladas de otros equipos (para las HU de integración; el arquetipo aún no las usa) |
| `api-patent-sweep` | La API Patent Sweep, expuesta en el puerto `API_PUERTO` |

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

Si el puerto `8085` está ocupado, cambie `API_PUERTO` y use ese puerto en todas las URL de esta guía.

---

## 5. Probar desde Swagger

Abra **http://localhost:8085/swagger-ui.html**, despliegue el endpoint y pulse *Try it out*.

### `POST /api/v1/patentes/busqueda`

| Cuerpo | Resultado esperado |
|---|---|
| `{"codigoPatente":"PAT-000123","titulo":"Cuchilla de diamante segmentada"}` | **201** con la patente, `patenteId` y `estadoVigencia: "EN_TRAMITE"` |
| El mismo cuerpo otra vez | **409** "Ya existe una patente registrada con el código: PAT-000123" |
| `{"codigoPatente":"PAT-12","titulo":"X"}` | **400** "El código debe tener el formato PAT-NNNNNN" |
| `{"codigoPatente":"PAT-000124","titulo":""}` | **400** "El título es obligatorio" |
| Cuerpo que no es JSON | **400** "El cuerpo de la petición no es válido" |

### `GET /api/v1/patentes/estado/{id}`

| `id` | Resultado esperado |
|---|---|
| El `patenteId` que devolvió el POST (por ejemplo `1`) | **200** con la patente |
| `999` | **404** "Patente no encontrada con ID: 999" |

Todos los errores vienen en formato ProblemDetail con un `traceId`, que también aparece en el log
(`docker compose logs api-patent-sweep`).

### Alternativa con `curl`

```bash
curl -s -X POST http://localhost:8085/api/v1/patentes/busqueda \
  -H "Content-Type: application/json" \
  -d '{"codigoPatente":"PAT-000123","titulo":"Cuchilla de diamante segmentada"}'

curl -s http://localhost:8085/api/v1/patentes/estado/1
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
