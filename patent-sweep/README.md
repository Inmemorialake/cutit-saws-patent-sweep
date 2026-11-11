# API Patent Sweep (Equipo 2)

Servicio de vigilancia de patentes de la plataforma de automatización de la cadena de suministro de
Cutit Saws Ltd. — curso 750021C Desarrollo de Software II, Universidad del Valle, 2026-2.

Este directorio es el **arquetipo (línea base)** del backend: la estructura del DAS V3.0 hecha código, más un
**ejemplo transaccional completo** (HU-02, búsqueda manual de patentes) con sus pruebas unitarias.

## 1. Resumen arquitectónico

Arquitectura **multicapa** sobre **Java 21 y Spring Boot 3** (DAS DA-02), con base de datos propia
**PostgreSQL 16** (DA-01). Cada capa solo depende de la inferior o de la capa transversal:

```
controller ──► service ──┬──► integration (clientes HTTP con Resilience4j, puerto de la fuente de patentes)
 (REST, DTO)   (lógica)  └──► repository + model (Spring Data JPA, PostgreSQL)
        config / exception: capa transversal (seguridad JWT + RBAC, caché, OpenAPI, errores ProblemDetail)
```

El ejemplo transaccional recorre todas las capas: `POST /api/v1/auth/login` → `POST /api/v1/patentes/busqueda`
(JWT → controlador → servicio → cliente de Materiales → fuente de patentes → motor de coincidencias → base de datos)
→ evento `ConflictoDetectadoEvent` → notificación asíncrona a la API de Notificaciones.

## 2. Requisitos

| Para | Necesita |
|---|---|
| Ejecutar todo con Docker | Docker 24.0 o superior y Docker Compose v2.20 o superior (comando `docker compose`) |
| Pruebas y cobertura sin Docker | JDK 21 y Maven 3.9 o superior |

## 3. Inicio rápido

```bash
git clone https://github.com/Inmemorialake/Desarrollo-II.git
cd Desarrollo-II
git checkout develop-v2
cd patent-sweep
cp .env.example .env
docker compose up --build
```

Edite `.env` antes de levantar el sistema si quiere cambiar las claves (los valores por defecto son solo marcadores).
Cuando el contenedor `api-patent-sweep` esté `healthy`:

- Swagger UI: `http://localhost:8085/swagger-ui.html` (el puerto es `API_PUERTO` de `.env`)
- Health check: `http://localhost:8085/actuator/health`

### Probar el flujo de ejemplo

```bash
# 1. Iniciar sesión (usuario y clave de ADMIN_USUARIO y ADMIN_CLAVE en .env) y guardar el token
TOKEN=$(curl -s -X POST http://localhost:8085/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usuario":"admin","clave":"cambiar_clave_del_administrador"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

# 2. Buscar patentes en conflicto con la fórmula form-001 (la API simulada de Materiales la define)
curl -s -X POST http://localhost:8085/api/v1/patentes/busqueda \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"formulaId":"form-001"}'
```

Fórmulas simuladas (`mocks/mappings`): `form-001` produce dos conflictos (`PAT-100001` con 3 de 4 materiales y `PAT-100002` con justo el 50 %);
`form-002` produce uno (`PAT-200001`; `PAT-200002` solo coincide en un material y se descarta); `form-003` está DESCONTINUADA (409); cualquier otro código responde 404. En Swagger UI también puede
usar el botón **Authorize** con el token del login.

Para detener: `docker compose down` (agregue `-v` para borrar también los datos de PostgreSQL).

## 4. Ejecutar las pruebas (sin Docker)

```bash
cd backend
mvn test
```

Son pruebas unitarias con JUnit 5 y Mockito (estilo AAA, sin tocar una base real) más una prueba de integración
(`FlujoBusquedaIntegracionTest`) que levanta un PostgreSQL 16 embebido para validar Flyway, el mapeo JPA, la seguridad y el
flujo completo.

## 5. Cobertura

```bash
cd backend
mvn verify
```

Genera el reporte en `backend/target/site/jacoco/index.html` y **falla la compilación** si la cobertura de líneas o
ramas es inferior al 81 % en los paquetes `service` y `service.event`.

## 6. Integración continua y estilo

El workflow [../.github/workflows/ci.yml](../.github/workflows/ci.yml) se ejecuta en cada push y pull request hacia
`main`, `develop-v1` y `develop-v2`. Ejecuta las pruebas, valida el umbral de cobertura con JaCoCo, revisa reglas
básicas de estilo con Checkstyle y construye el artefacto Maven. La configuración de Checkstyle está en
`backend/config/checkstyle/checkstyle.xml`.

SonarCloud aún no está integrado. Además, para que un resultado fallido impida aceptar un pull request, hay que
marcar el workflow como verificación obligatoria en las reglas de protección de ramas de GitHub.

## 7. Estructura del proyecto

```
patent-sweep/
├── docker-compose.yml        postgres + api-patent-sweep + apis-simuladas (WireMock)
├── .env.example              variables de entorno documentadas
├── mocks/mappings/           respuestas simuladas de Materiales/Fórmulas, Fuente de patentes y Notificaciones
└── backend/
    ├── Dockerfile            multi-etapa, usuario sin privilegios
    ├── pom.xml
    ├── config/checkstyle/    reglas de inspección estática
    └── src/main/java/co/edu/univalle/patentsweep/
        ├── controller/       Controllers Layer: BusquedaController, AuthController, dto/, mapper/
        ├── service/          Logic Layer: BusquedaService, RevisionService, MotorCoincidencias,
        │                     EstadoFormulaService, NotificacionService, event/
        ├── integration/      Integration Layer: MaterialesClient, NotificacionesClient, port/, adapter/, dto/
        ├── repository/       Data Access Layer: repositorios Spring Data JPA
        ├── model/            Data Access Layer: entidades JPA y enums de estado
        ├── config/           Utility Layer: seguridad JWT, caché, OpenAPI, RestClient, async, Jackson
        └── exception/        Utility Layer: GlobalExceptionHandler (ProblemDetail) y excepciones propias
```

Las carpetas siguen el blueprint acordado con el DAS (secciones 2 a 4).

## 8. Decisiones arquitectónicas clave (DAS)

| Decisión | Dónde se ve en el código |
|---|---|
| DA-01 base de datos propia | `application.yml`, `db/migration/V1__esquema.sql` |
| DA-02 multicapa, Java 21, Spring Boot 3 | paquetes `controller`, `service`, `integration`, `repository` |
| DA-05 fuente de patentes detrás de un puerto | `integration/port/FuentePatentesPort`, `integration/FuentePatentesClient` (HTTP contra la API simulada) |
| DA-06 Resilience4j | `@Retry` y `@CircuitBreaker` en los clientes; configuración en `application.yml` |
| DA-07 notificaciones por evento asíncrono | `ConflictoDetectadoEvent`, `NotificacionService` |
| DA-08 estado de fórmula precalculado | `EstadoFormulaService` |
| DA-11 JWT + RBAC | `config/SecurityConfig`, `JwtProvider`, `AuthController` |
| DA-12 errores ProblemDetail con `traceId` | `exception/GlobalExceptionHandler` |

Los requisitos funcionales y mensajes exactos salen del Product Backlog V3.0 (HU-02 y HU-12). Los puntos donde el PB
y el DAS difieren se resolvieron a favor del PB (prefijo `/api/v1`, formato de fechas `dd/mm/aaaa hh:mm:ss`).

### Adiciones que no están en el DAS ([Archetype Convention Addition])

| Adición | Justificación |
|---|---|
| Flyway (`db/migration`) | El ER exige un índice único parcial, columnas JSONB y el catálogo IPC/CPC precargado; migraciones versionadas (guía del curso) |
| JaCoCo | Umbral mínimo de 81 % en líneas y ramas de los paquetes `service` y `service.event` |
| `UsuarioSeedRunner` | El DAS no define cómo se crea el primer usuario; se crea desde variables de entorno |
| Mapeadores manuales | Evitan sumar MapStruct, que no está en el DAS |
| `RevisionService` | Separa la transacción de base de datos de las llamadas HTTP para no retener conexiones |
| WireMock (`apis-simuladas`) | Permite que el ejemplo corra sin las APIs de otros equipos |
| PostgreSQL embebido (solo pruebas) | Prueba de integración sin Docker |

## 9. Limitaciones conocidas y siguientes pasos

- **Alcance del arquetipo:** solo HU-02 está implementada de punta a punta, más el envío de notificaciones de HU-12 que HU-02
  dispara. Faltan HU-01 (barrido y `BarridoScheduler`), HU-03 a HU-11, y el reintento de notificaciones `PENDIENTE_NOTIFICACION`
  al iniciar cada barrido.
- **Pendiente:** SonarCloud y Grafana. El workflow de CI ejecuta pruebas, JaCoCo y Checkstyle, pero no despliega la
  aplicación. Los endpoints de Actuator (`/actuator/health`, `/actuator/prometheus`) ya están expuestos.
- **TimeLimiter:** se aplica como tiempo máximo de lectura del cliente HTTP (20 s por intento, según el PB), no como anotación
  `@TimeLimiter`, que exige métodos asíncronos.
- **Caffeine:** `CacheConfig` está lista, pero se usará cuando se implemente la validación de clasificaciones (HU-10).
- **ShedLock y purga de 12 meses** (DAS DA-10, tabla 14): no implementados; solo hay un nodo.
- **Health check de Docker:** `restart: unless-stopped` reinicia el contenedor si el proceso termina, pero no si queda
  `unhealthy` (Docker Engine no lo hace; el DAS propone un monitor como autoheal).
- **Roles:** el DAS define solo `ADMINISTRADOR` y `SERVICIO`; el PB menciona tres perfiles humanos. Los GET quedan abiertos
  a cualquier usuario autenticado.
- **Spring Boot 3.x** (3.5.16) puede estar fuera del soporte de código abierto; migrar a 4.x requiere revisar Resilience4j y springdoc.
- **Verificación:** las pruebas unitarias y la de integración pasan; el `Dockerfile` y `docker-compose.yml` no se ejecutaron en el
  entorno donde se generó el arquetipo (sin Docker instalado); se validó solo la sintaxis del Compose.
