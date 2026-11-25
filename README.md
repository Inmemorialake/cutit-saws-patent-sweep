# 🛡️ Patent Sweep API — Cutit Saws

API REST del módulo de **Patent Sweep (Equipo 1)** para la plataforma modular de Cutit Saws — curso 750021C Desarrollo de Software II, Universidad del Valle. Gestiona la vigilancia automatizada y validación cruzada de riesgo de patentes para proteger los diseños de fabricación.

[![CI](https://github.com/Inmemorialake/cutit-saws-patent-sweep/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/Inmemorialake/cutit-saws-patent-sweep/actions/workflows/ci.yml)
[![CD](https://github.com/Inmemorialake/cutit-saws-patent-sweep/actions/workflows/cd.yml/badge.svg)](https://github.com/Inmemorialake/cutit-saws-patent-sweep/actions/workflows/cd.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=Inmemorialake_cutit-saws-patent-sweep&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Inmemorialake_cutit-saws-patent-sweep)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Inmemorialake_cutit-saws-patent-sweep&metric=coverage)](https://sonarcloud.io/summary/new_code?id=Inmemorialake_cutit-saws-patent-sweep)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Inmemorialake_cutit-saws-patent-sweep&metric=bugs)](https://sonarcloud.io/summary/new_code?id=Inmemorialake_cutit-saws-patent-sweep)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=Inmemorialake_cutit-saws-patent-sweep&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=Inmemorialake_cutit-saws-patent-sweep)
[![Versión](https://img.shields.io/github/v/tag/Inmemorialake/cutit-saws-patent-sweep?include_prereleases&label=versi%C3%B3n)](CHANGELOG.md)
[![Status](https://img.shields.io/badge/status-Sprint%200%20%E2%80%94%20arquetipo-blue.svg)](docs/adrs/ADR-006-arquetipo-sprint-0-una-transaccion.md)

---

## 📋 Descripción y contexto

Cutit Saws Ltd. es un fabricante boutique de motosierras hidráulicas de diamante que, tras el éxito de su modelo "Ripit 5000", enfrenta una demanda que supera su capacidad de manufactura. Para escalar sus operaciones de forma segura, la dirección definió una plataforma backend modular basada en 5 APIs REST independientes.

Esta API (**Patent Sweep**) es responsable de la **vigilancia y validación cruzada de riesgo de patentes**.

| Rol en la plataforma | Consumidor / Relación |
|---|---|
| **Es consumida por** | Materiales / Fórmulas — validación de componentes contra patentes |
| **Es consumida por** | Fabricación — validación de lotes de producción |

### Estado actual: Sprint 1 cerrado · `v1.0.0-beta.3`

El backend implementa HU-00 (JWT y cimientos), HU-01 (barrido automático), HU-02 (búsqueda manual de conflictos) y HU-04 (estado de la fórmula), con pruebas, CI, análisis de calidad y despliegue continuo. El arquetipo de registro y consulta de patentes del Sprint 0 (ADR-006) se retiró; HU-03 y HU-05 a HU-12 siguen pendientes. Estado detallado en [`docs/progress.md`](docs/progress.md).

---

## 🏛️ Arquitectura

Arquitectura **multicapa** sobre Java 21 y Spring Boot 3 (DAS DA-02), con base de datos propia PostgreSQL 16 (DA-01):

```
Cliente ──► BusquedaController · EstadoFormulaController (REST, DTO, validación)   /api/v1/patentes/...
                 │
                 ▼
            BusquedaService · EstadoFormulaService · BarridoService   @Transactional
                 │
                 ├──► MaterialesClient · FuentePatentesClient (Resilience4j)
                 ▼
            Repositorios (Spring Data JPA) ──► PostgreSQL 16 (migraciones Flyway V1–V8)

Transversal: GlobalExceptionHandler (ProblemDetail RFC 9457 + traceId) · OpenAPI · Actuator/Prometheus
```

| Documento | Ubicación |
|---|---|
| Documento de Arquitectura de Software (DAS V3.0) | [`docs/arquitectura/`](docs/arquitectura/) |
| Product Backlog V3.0 (HU-01 a HU-12) | [`docs/backlog/`](docs/backlog/) |
| Decisiones de arquitectura (ADR-001 a 007) | [`docs/adrs/`](docs/adrs/) |
| Guía de despliegue en AWS | [`docs/despliegue/EC2.md`](docs/despliegue/EC2.md) |

---

## 🎯 Endpoints

| Método | Ruta | Respuestas | Descripción |
|---|---|---|---|
| `POST` | `/api/v1/patentes/busqueda` | 200 · 400 · 401 · 403 · 404 · 409 · 503 | Búsqueda manual de conflictos (solo `ADMINISTRADOR`) |
| `GET` | `/api/v1/patentes/estado/{formulaId}` | 200 · 400 · 401 · 404 · 500 · 503 | Estado calculado de una fórmula, con sus alertas activas (HU-04) |
| `GET` | `/actuator/health` | 200 · 503 | Salud de la API y de PostgreSQL (público) |
| `GET` | `/swagger-ui.html` · `/v3/api-docs` | 200 | Documentación OpenAPI |

Los errores se devuelven en formato **ProblemDetail** con `traceId`.

### Monitoreo y acceso (ADR-010)

- En producción la API y Grafana se publican **solo por HTTPS** a través de Caddy (certificados de Let's Encrypt) en dos subdominios; el EC2 solo abre 80/443. Detalles en [`docs/despliegue/EC2.md`](docs/despliegue/EC2.md).
- **Prometheus** recolecta las métricas de la API (puerto interno 9091) y del servidor (node-exporter); **Grafana** trae provisionados los tableros *Patent Sweep · API* y *Patent Sweep · Servidor (EC2)*, con login obligatorio.
- En local, con `docker compose up -d`, Grafana queda en **http://localhost:3000** (usuario y clave de `GRAFANA_ADMIN_*` en `.env`). node-exporter solo corre en el EC2; en Linux se puede activar con `docker compose --profile servidor up -d` para ver el tablero del servidor.

### Barrido automático (HU-01)

- El planificador lee la frecuencia de la fila activa de `configuracion` (`frecuencia_valor`, `frecuencia_unidad` y `frecuencia_horas`) en cada ciclo. Para cambiarla, actualice esa fila (o inserte una nueva activa); el nuevo intervalo aplica en la siguiente ejecución sin reiniciar. HU-10 añadirá el endpoint de configuración.
- El barrido consulta Materiales/Fórmulas en páginas de 10 fórmulas aprobadas, busca patentes candidatas, reutiliza el motor de coincidencias de HU-02 y registra cada resultado con origen `BARRIDO_AUTOMATICO` y usuario `SISTEMA`. Un fallo por fórmula la deja `NO_PROCESADO` y no detiene el barrido.
- `@SchedulerLock` con ShedLock evita dos ejecuciones concurrentes aunque haya varias réplicas. Si el proceso se reinicia con un barrido `EN_CURSO`, el recuperador lo marca `FALLIDO` al arrancar.
- Métricas en `/actuator/prometheus`, solo por el puerto interno de management (`MANAGEMENT_PUERTO`, ADR-010), que Prometheus recolecta y Grafana muestra en el tablero *Patent Sweep · API*: `patentsweep.barrido.duracion` (Timer) y `patentsweep.barrido.formulas{resultado="con_conflicto|sin_conflicto|no_procesado"}` (Counter), `patentsweep.barrido.ejecuciones{estado="COMPLETADO|FALLIDO|OMITIDO"}` (Counter) y `patentsweep.barrido.ultima.ejecucion{estado}` (Gauge, segundos epoch). El tablero muestra hace cuánto fue el último barrido, su estado y una barra por ejecución en 7 días. Los logs del barrido incluyen `traceId`.

---

## 🛠️ Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje y framework | Java 21 · Spring Boot 3.5.16 |
| Persistencia | Spring Data JPA · Flyway · PostgreSQL 16 |
| Pruebas y cobertura | JUnit 5 · Mockito · JaCoCo (umbral 80 % en `service` y `exception`) |
| Calidad | Checkstyle · SonarCloud (quality gate obligatorio en el CI) |
| Documentación API | SpringDoc OpenAPI (Swagger UI) |
| Observabilidad | Spring Boot Actuator · Micrometer (Prometheus, `traceId` en logs) |
| Contenedores | Docker multi-etapa (usuario sin privilegios) · Docker Compose · WireMock |
| CI/CD | GitHub Actions · GHCR · AWS EC2 con SSM Run Command (sin SSH, documento de despliegue propio) |

---

## 🚀 Inicio rápido

Requisitos: **Docker** con Docker Compose v2.24+ (para ejecutar) y, opcionalmente, **JDK 21 + Maven 3.9+** (para las pruebas).

```bash
git clone https://github.com/Inmemorialake/cutit-saws-patent-sweep.git
cd cutit-saws-patent-sweep/patent-sweep
cp .env.example .env            # cambie DB_PASSWORD si quiere
docker compose up -d --build --wait
```

Probar:

```bash
curl -s localhost:8085/actuator/health                      # {"status":"UP"}

# Con un JWT de /api/v1/auth/login (ver GUIA_EJECUCION.md):
curl -s -X POST localhost:8085/api/v1/patentes/busqueda \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"formulaId":"form-001"}'                            # 200

curl -s localhost:8085/api/v1/patentes/estado/form-001 \
  -H "Authorization: Bearer $TOKEN"                         # 200
```

Swagger UI: **http://localhost:8085/swagger-ui.html** · Más casos de prueba: [`patent-sweep/GUIA_EJECUCION.md`](patent-sweep/GUIA_EJECUCION.md)

Pruebas, cobertura y estilo (sin Docker):

```bash
cd patent-sweep/backend
mvn clean verify     # reporte en target/site/jacoco/index.html
```

---

## 🔄 Flujo de trabajo y CI/CD

```
rama personal ─PR─► develop ─PR─► main ─► CD ─► EC2 (AWS)
                │              │
                └──── CI ──────┘  pruebas · JaCoCo · Checkstyle · SonarCloud (quality gate) · build de Docker
```

1. Trabaje en su rama personal (`primerainicialnombre` + `primerapellido`, por ejemplo `agonzalez`).
2. Valide en local con `mvn clean verify`.
3. Abra un PR hacia `develop` con la [plantilla](.github/pull_request_template.md). El **CI** ([`ci.yml`](.github/workflows/ci.yml)) debe pasar: pruebas, cobertura, estilo, el quality gate de SonarCloud y el build de la imagen Docker. Se necesita **una aprobación** de otra persona del equipo.
4. Al cerrar el sprint, `develop` se integra en `main` por PR (con *Create a merge commit*) y se etiqueta la versión (`v1.0.0-beta.N`). Con el CI en verde en `main`, el **CD** ([`cd.yml`](.github/workflows/cd.yml)) publica la imagen en GHCR y la despliega en el EC2 **sin SSH** con AWS Systems Manager: GitHub solo puede lanzar un documento de despliegue fijo, con el SHA de `main` y el digest de la imagen como únicos parámetros. Incluye health check y prueba de humo, y el rollback se hace con el mismo workflow. Ver [ADR-007](docs/adrs/ADR-007-cd-con-ssm-sin-ssh.md).

Flujo completo, convenciones de commits y Definición de Hecho: [`CONTRIBUTING.md`](CONTRIBUTING.md) · Historial de cambios: [`CHANGELOG.md`](CHANGELOG.md) · Operación del servidor (cómo entrar a la consola, rotación de llaves, costos, problemas comunes): [`docs/despliegue/EC2.md`](docs/despliegue/EC2.md).

---

## 📁 Estructura del repositorio

```
.
├── .github/          workflows (ci.yml, cd.yml) y plantilla de PR
├── docs/
│   ├── adrs/         decisiones de arquitectura (ADR-001 a ADR-010)
│   ├── arquitectura/ DAS V3.0
│   ├── backlog/      Product Backlog V3.0
│   ├── despliegue/   guía de EC2
│   ├── ia/           prompts y salidas usados con asistentes de IA
│   └── referencias/  material de apoyo (análisis de HU de Materiales)
├── infra/aws/        plantilla CloudFormation: documento SSM de despliegue, usuario de CD y perfil de instancia
├── patent-sweep/     backend/ (Spring Boot), mocks/ (WireMock), docker-compose*.yml, .env.example
└── scripts/          preparar-ec2.sh (prepara el servidor)
```

Detalle técnico del backend: [`patent-sweep/README.md`](patent-sweep/README.md).

---

## 👥 Equipo — Patent Sweep

| Nombre | Rol |
|---|---|
| **Isabela Bermúdez Moreno** | Desarrollo Backend, Pruebas Unitarias & CI/CD |
| **Andrés Gerardo González Rosero** | Desarrollo Backend, Pruebas Unitarias & CI/CD |
| **Juan Marín Orozco** | Desarrollo Backend, Pruebas Unitarias & CI/CD |
| **David Taborda Montenegro** | Desarrollo Backend, Pruebas Unitarias & CI/CD |
| **Victoria Yuan Chen** | Desarrollo Backend, Pruebas Unitarias & CI/CD |

---

## 📚 Créditos y curso

**750021C — Desarrollo de Software II**
Semestre 2026-2 (agosto–diciembre)
Profesor: Manuel Alejandro Pastrana Pardo, PhD.
Escuela de Ingeniería de Sistemas y Computación — **Universidad del Valle**

---

## 📄 Licencia

Proyecto académico — Universidad del Valle. Todos los derechos reservados.
