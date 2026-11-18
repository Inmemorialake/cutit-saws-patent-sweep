# API Patent Sweep: backend

Servicio de vigilancia de patentes de la plataforma de Cutit Saws Ltd. — curso 750021C Desarrollo de Software II,
Universidad del Valle, 2026-2.

Este directorio es el **arquetipo Sprint 0** del backend: la estructura del DAS V3.0 hecha código, con **una sola
transacción completa** (registrar y consultar una patente), sus pruebas, CI y despliegue. El alcance y lo que se
quitó a propósito están en [ADR-006](../docs/adrs/ADR-006-arquetipo-sprint-0-una-transaccion.md).

## 1. Resumen arquitectónico

Arquitectura **multicapa** sobre **Java 21 y Spring Boot 3** (DAS DA-02), con base de datos propia **PostgreSQL 16**
(DA-01). Cada capa depende solo de la inferior o de la capa transversal:

```
controller ──► service ──► repository + model ──► PostgreSQL 16 (Flyway)
 (REST, DTO)   (reglas,     (Spring Data JPA)
                transacción)
   config / exception: capa transversal (OpenAPI, errores ProblemDetail con traceId)
```

Flujo de la transacción `POST /api/v1/patentes/busqueda`:

1. `PatenteController` valida el `PatenteRequestDTO` (Bean Validation: `PAT-NNNNNN`, título obligatorio y longitudes).
2. `PatenteService.registrar` (`@Transactional`) verifica que el código no exista (409 si existe) y guarda.
3. `Patente` (`@PrePersist`) fija `createdAt` y el estado inicial `EN_TRAMITE`.
4. La respuesta 201 se arma con `PatenteResponseDTO`. Los errores pasan por `GlobalExceptionHandler` (400/404/409/500 en ProblemDetail, con `traceId`).

## 2. Requisitos

| Para | Necesita |
|---|---|
| Ejecutar con Docker | Docker 24+ y Docker Compose v2.24+ (comando `docker compose`) |
| Pruebas y cobertura | JDK 21 y Maven 3.9+ (no requiere Docker ni PostgreSQL) |

## 3. Inicio rápido

```bash
cd patent-sweep
cp .env.example .env
docker compose up -d --build --wait
```

- Swagger UI: `http://localhost:8085/swagger-ui.html` (el puerto es `API_PUERTO` de `.env`)
- Health check: `http://localhost:8085/actuator/health`
- Casos de prueba paso a paso: [GUIA_EJECUCION.md](GUIA_EJECUCION.md)

Para detener: `docker compose down` (agregue `-v` para borrar también los datos de PostgreSQL).

## 4. Pruebas, cobertura y estilo

```bash
cd backend
mvn clean verify
```

| Qué | Detalle |
|---|---|
| Pruebas | JUnit 5 + Mockito (estilo AAA): `PatenteControllerTest` (`@WebMvcTest`), `PatenteServiceTest`, `GlobalExceptionHandlerTest` y `RestriccionesEntidadesTest` |
| Cobertura | JaCoCo. `verify` **falla** si `service` o `exception` bajan del 80 % de líneas o ramas. Reporte en `backend/target/site/jacoco/index.html`. Hoy: 100 % |
| Estilo | Checkstyle (`backend/config/checkstyle/checkstyle.xml`), también ligado a `verify` |

## 5. Integración continua y SonarCloud

[`../.github/workflows/ci.yml`](../.github/workflows/ci.yml) corre en cada PR y en cada push a `main` y `develop`:

| Job | Qué hace |
|---|---|
| **Pruebas, cobertura, estilo y Sonar** | Descarga las dependencias (con reintentos ante el HTTP 429 de Maven Central), ejecuta `mvn clean verify`, sube el JAR como artefacto y corre `mvn sonar:sonar`, que **espera el quality gate** y falla si no pasa |
| **Construir imagen Docker** | Construye la imagen sin publicarla, para que un PR no pueda romper el despliegue |

SonarCloud:

- La organización, la clave del proyecto, la URL y `sonar.qualitygate.wait` están en las propiedades de `backend/pom.xml`.
- La cobertura llega desde el XML de JaCoCo. `config`, `model` y `dto` se excluyen **solo de la cobertura**; su código sí se analiza.
- Requisitos: el secreto `SONAR_TOKEN` en el repositorio y el **análisis automático desactivado** en SonarCloud
  (*Administration → Analysis Method*). Si está activo, el análisis desde el CI falla.
- El plan gratuito analiza **solo los PR y `main`**: en un push a `develop` Sonar se omite con un aviso, porque el PR ya lo analizó.
- Los PR de forks no reciben el token: en ellos Sonar también se omite con un aviso.
- **Primer análisis de una rama:** el quality gate *Sonar way* evalúa el código nuevo respecto del análisis anterior. En el primer
  análisis no hay con qué comparar, el estado queda `NONE` y el CI lo marca como fallido. Se resuelve volviendo a ejecutar el job.
  Ya pasó con `main` y no vuelve a ocurrir.
- Para ejecutarlo en local: `mvn verify sonar:sonar -Dsonar.token=<SU_TOKEN>` desde `backend/`.

### Protección de ramas

`main` y `develop` tienen un *ruleset*: PR obligatorio con **una aprobación**, sin borrar la rama ni hacer *force-push*.
Los checks del CI **todavía no son obligatorios** en la regla. Se recomienda agregar **Pruebas, cobertura, estilo y Sonar**
y **Construir imagen Docker** como *required status checks* (*Settings → Rules → Rulesets*). Mientras tanto, no se fusiona
un PR con el CI en rojo. El flujo completo está en [CONTRIBUTING.md](../CONTRIBUTING.md).

## 6. Despliegue continuo en AWS (EC2)

- [`../.github/workflows/cd.yml`](../.github/workflows/cd.yml) corre cuando el CI pasa en un push a `main` (o a mano).
  Publica la imagen en `ghcr.io/inmemorialake/cutit-saws-patent-sweep` (SHA y `latest`) y la despliega **por digest**.
- **Sin SSH**: el job lanza en el EC2, con **SSM Run Command**, el documento `PatentSweep-Desplegar`. Su procedimiento
  es fijo y solo recibe el SHA (que debe pertenecer a `main`) y el digest de la imagen. El usuario IAM de GitHub no puede
  ejecutar nada más. El puerto 22 queda cerrado.
- El script verifica `/actuator/health` y hace una prueba de humo contra la API. El rollback es el mismo workflow con `image_tag=<SHA>`.
- En el servidor se usa `docker-compose.yml` más [`docker-compose.prod.yml`](docker-compose.prod.yml) (imagen de GHCR
  y límites de memoria). El `.env` de producción vive solo en el servidor.
- La infraestructura (documento SSM, usuario de CD y perfil de instancia) está en [`../infra/aws/cd-ssm.yml`](../infra/aws/cd-ssm.yml).
- Guía paso a paso: [docs/despliegue/EC2.md](../docs/despliegue/EC2.md). Decisiones: [ADR-005](../docs/adrs/ADR-005-despliegue-ec2-compose-ghcr.md) y [ADR-007](../docs/adrs/ADR-007-cd-con-ssm-sin-ssh.md).

## 7. Estructura

```
patent-sweep/
├── docker-compose.yml        postgres + api-patent-sweep + apis-simuladas (WireMock)
├── docker-compose.prod.yml   override para el EC2 (imagen de GHCR por digest, límites de memoria)
├── .env.example              variables de entorno documentadas
├── mocks/mappings/           respuestas simuladas de Materiales/Fórmulas, fuente de patentes y Notificaciones (ADR-001)
└── backend/
    ├── Dockerfile            multi-etapa, usuario sin privilegios
    ├── pom.xml
    ├── config/checkstyle/    reglas de inspección estática
    └── src/
        ├── main/java/co/edu/univalle/patentsweep/
        │   ├── controller/   Controllers Layer: PatenteController y dto/
        │   ├── service/      Logic Layer: PatenteService
        │   ├── integration/  Integration Layer: clientes de APIs externas (vacía en el Sprint 0, ver su LEEME.txt)
        │   ├── repository/   Data Access Layer: PatenteRepository
        │   ├── model/        Data Access Layer: entidad Patente y enums
        │   ├── config/       Utility Layer: OpenApiConfig
        │   └── exception/    Utility Layer: GlobalExceptionHandler (ProblemDetail) y excepciones propias
        ├── main/resources/   application*.yml y db/migration (Flyway V1–V6)
        └── test/             pruebas unitarias y de capa web
```

## 8. Decisiones arquitectónicas clave (DAS)

| Decisión | Dónde se ve en el código |
|---|---|
| DA-01 base de datos propia | `application.yml`, `db/migration/V1__esquema.sql` |
| DA-02 multicapa, Java 21, Spring Boot 3 | paquetes `controller`, `service`, `repository`, `model` |
| DA-12 errores ProblemDetail con `traceId` | `exception/GlobalExceptionHandler`, Micrometer Tracing |
| P23/P28 salud y métricas | Actuator (`health`, `prometheus`) |
| P24 contrato OpenAPI | `config/OpenApiConfig`, anotaciones del controlador |

Las decisiones DA-05 a DA-11 (puerto de la fuente de patentes, Resilience4j, notificaciones asíncronas, estado
precalculado, JWT + RBAC) vuelven con las HU que las necesitan. Su última implementación está en el commit `5d13af5`.

### Adiciones que no están en el DAS ([Archetype Convention Addition])

| Adición | Justificación |
|---|---|
| Flyway (`db/migration`) | El ER exige índices únicos parciales, columnas JSONB y el catálogo IPC/CPC precargado. Las migraciones son versionadas (guía del curso) |
| JaCoCo | Umbral mínimo de 80 % de líneas y ramas en `service` y `exception` |
| WireMock (`apis-simuladas`) | Permitirá probar las HU de integración sin depender de las APIs de otros equipos |

## 9. Limitaciones conocidas

- **Sin autenticación** en el Sprint 0: en el EC2 cualquiera puede registrar patentes. No cargue datos reales hasta que vuelva DA-11 (ADR-006).
- `/actuator/prometheus` y Swagger UI también son públicos en `prod`. Cuando vuelva la seguridad, hay que decidir si se
  restringen o se apagan (`springdoc.swagger-ui.enabled=false` en `application-prod.yml`).
- El Sprint 0 no implementa historias del backlog: HU-01 a HU-12 empiezan en el Sprint 1.
- `restart: unless-stopped` reinicia el contenedor si el proceso termina, pero no si queda `unhealthy`.
- Spring Boot 3.5.x puede quedar fuera del soporte de código abierto. Migrar a 4.x es una tarea planificada: exige
  springdoc 3.x y revisar la configuración de pruebas (`@WebMvcTest`).
