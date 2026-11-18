# 🛡️ Patent Sweep API — Cutit Saws

API REST del módulo de **Patent Sweep (Equipo 1)** para la plataforma modular de Cutit Saws — curso 750021C Desarrollo de Software II, Universidad del Valle. Gestiona la vigilancia automatizada y validación cruzada de riesgo de patentes para proteger los diseños de fabricación.

[![CI](https://github.com/Inmemorialake/cutit-saws-patent-sweep/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/Inmemorialake/cutit-saws-patent-sweep/actions/workflows/ci.yml)
[![CD](https://github.com/Inmemorialake/cutit-saws-patent-sweep/actions/workflows/cd.yml/badge.svg)](https://github.com/Inmemorialake/cutit-saws-patent-sweep/actions/workflows/cd.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=Inmemorialake_cutit-saws-patent-sweep&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Inmemorialake_cutit-saws-patent-sweep)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Inmemorialake_cutit-saws-patent-sweep&metric=coverage)](https://sonarcloud.io/summary/new_code?id=Inmemorialake_cutit-saws-patent-sweep)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Inmemorialake_cutit-saws-patent-sweep&metric=bugs)](https://sonarcloud.io/summary/new_code?id=Inmemorialake_cutit-saws-patent-sweep)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=Inmemorialake_cutit-saws-patent-sweep&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=Inmemorialake_cutit-saws-patent-sweep)
[![Status](https://img.shields.io/badge/status-Sprint%200%20%E2%80%94%20arquetipo-blue.svg)](docs/adrs/ADR-006-arquetipo-sprint-0-una-transaccion.md)

---

## 📋 Descripción y contexto

Cutit Saws Ltd. es un fabricante boutique de motosierras hidráulicas de diamante que, tras el éxito de su modelo "Ripit 5000", enfrenta una demanda que supera su capacidad de manufactura. Para escalar sus operaciones de forma segura, la dirección definió una plataforma backend modular basada en 5 APIs REST independientes.

Esta API (**Patent Sweep**) es responsable de la **vigilancia y validación cruzada de riesgo de patentes**.

| Rol en la plataforma | Consumidor / Relación |
|---|---|
| **Es consumida por** | Materiales / Fórmulas — validación de componentes contra patentes |
| **Es consumida por** | Fabricación — validación de lotes de producción |

### Estado actual: Sprint 0 (arquetipo)

El backend implementa **una sola transacción completa**, registrar y consultar una patente, que recorre todas las capas, con pruebas, CI, análisis de calidad y despliegue continuo. Las historias del backlog (HU-01 a HU-12) se implementan desde el Sprint 1. Ver [ADR-006](docs/adrs/ADR-006-arquetipo-sprint-0-una-transaccion.md).

---

## 🏛️ Arquitectura

Arquitectura **multicapa** sobre Java 21 y Spring Boot 3 (DAS DA-02), con base de datos propia PostgreSQL 16 (DA-01):

```
Cliente ──► PatenteController (REST, DTO, validación)        /api/v1/patentes/...
                 │
                 ▼
            PatenteService (reglas: código único, estado inicial)   @Transactional
                 │
                 ▼
            PatenteRepository (Spring Data JPA) ──► PostgreSQL 16 (migraciones Flyway V1–V6)

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
| `POST` | `/api/v1/patentes/busqueda` | 201 · 400 · 409 | Registra una patente (`codigoPatente` con formato `PAT-NNNNNN`, `titulo`, y opcionalmente `tipoProducto` y `resumen`) en estado `EN_TRAMITE` |
| `GET` | `/api/v1/patentes/estado/{id}` | 200 · 404 | Consulta una patente y su estado de vigencia |
| `GET` | `/actuator/health` · `/actuator/prometheus` | 200 | Salud y métricas |
| `GET` | `/swagger-ui.html` · `/v3/api-docs` | 200 | Documentación OpenAPI |

Los errores se devuelven en formato **ProblemDetail** con `traceId`.

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
| CI/CD | GitHub Actions · GHCR · AWS EC2 con OIDC + SSM Run Command (sin SSH) |

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

curl -s -X POST localhost:8085/api/v1/patentes/busqueda \
  -H 'Content-Type: application/json' \
  -d '{"codigoPatente":"PAT-000123","titulo":"Cuchilla de diamante segmentada"}'   # 201

curl -s localhost:8085/api/v1/patentes/estado/1             # 200
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
3. Abra un PR hacia `develop` con la [plantilla](.github/pull_request_template.md). El **CI** ([`ci.yml`](.github/workflows/ci.yml)) debe pasar: pruebas, cobertura, estilo, el quality gate de SonarCloud y el build de la imagen Docker.
4. `develop` se integra en `main` por PR. Con el CI en verde en `main`, el **CD** ([`cd.yml`](.github/workflows/cd.yml)) publica la imagen en GHCR y la despliega en el EC2 **sin SSH**: credenciales temporales por OIDC y comando por AWS Systems Manager, con health check y prueba de humo. El rollback se hace con el mismo workflow. Ver [ADR-007](docs/adrs/ADR-007-cd-con-oidc-y-ssm.md).
5. Dependabot propone actualizaciones semanales de actions, Maven y Docker hacia `develop`.

Convenciones de commits y ramas: [`CONTRIBUTING.md`](CONTRIBUTING.md) · Historial de cambios: [`CHANGELOG.md`](CHANGELOG.md).

---

## 📁 Estructura del repositorio

```
.
├── .github/          workflows (ci.yml, cd.yml), dependabot.yml, plantilla de PR
├── docs/
│   ├── adrs/         decisiones de arquitectura (ADR-001 a ADR-007)
│   ├── arquitectura/ DAS V3.0
│   ├── backlog/      Product Backlog V3.0
│   ├── despliegue/   guía de EC2
│   ├── ia/           prompts y salidas usados con asistentes de IA
│   └── referencias/  material de apoyo (análisis de HU de Materiales)
├── infra/aws/        plantilla CloudFormation: OIDC, rol de despliegue y perfil de instancia
├── patent-sweep/     backend/ (Spring Boot), mocks/ (WireMock), docker-compose*.yml, .env.example
└── scripts/          preparar-ec2.sh y desplegar-ec2.sh
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
