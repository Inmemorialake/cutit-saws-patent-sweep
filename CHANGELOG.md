# Changelog

Todos los cambios notables del proyecto **Patent Sweep (Equipo 1)** se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).
El versionado sigue [Semantic Versioning](https://semver.org/lang/es/).

---

## [Unreleased]

### Cambiado

- HU-04: ruta `GET /estado/{formulaId}`, respuesta con `alertasActivas`, mensaje de éxito y errores del PB (400/503/500) y verificación de la fórmula en 5 s sin reintentos (política `materialesVerificacion`).
- HU-02: la búsqueda manual pasa a `POST /api/v1/patentes/busqueda`, como pide el PB; se retira `POST /buscar-conflictos` (D-7).
- HU-04 (GUI): la vista se llama «Patentes > Estado de Barrido», el botón «Ver Estado de Barrido» y muestra la tabla de `alertasActivas` (o «La fórmula no tiene alertas activas»).
- HU-01 (criterio 1.1): un barrido omitido por haber otro en curso queda en la auditoría (`barrido`) con estado `OMITIDO` y el mensaje «Barrido omitido: existe una ejecución en curso»; antes solo iba al log.
- HU-01/HU-02: el resultado del historial es `CON_CONFLICTO` solo con conflictos vigentes; el barrido guarda el mensaje del PB con la lista vacía y omite las fórmulas `DESCONTINUADAS` sin marcar fallo técnico.
- Integraciones: 20 s por intento y tope de 70 s por llamada lógica (antes 20 s en total).
- HU-02: `BusquedaService` ya no vuelve a consultar Materiales después de guardar conflictos e historial
  (la llamada a `recalcular` descartaba su resultado y podía convertir una búsqueda guardada en un 503).
- Migración V8 (ADR-012): el `UNIQUE` de `conflicto` pasa a `(formula_id, patente_id)`, sin la versión.

### Añadido

- Pruebas de integración de HU-02 (`@SpringBootTest` con PostgreSQL por Testcontainers y WireMock).

### Corregido

- Límite de memoria de Grafana en producción de 256 a 384 MB: en el EC2 usaba ~242 MB en reposo (94 % del límite) y
  podía reiniciarse por OOM.
- Una ruta inexistente (por ejemplo, la antigua `/buscar-conflictos`) respondía 500 y registraba un «Error interno»; ahora responde 404 `ProblemDetail`, y un método HTTP no soportado, 405.

## [1.0.0-beta.2] — 2026-11-24 · Sprint 1: HU-01, HU-02, monitoreo y HTTPS

### Añadido

- Monitoreo con **Prometheus + Grafana + node-exporter** en Docker Compose, con datasource y tableros provisionados
  (*Patent Sweep · API* y *Patent Sweep · Servidor (EC2)*) e histograma de latencia HTTP para el p95 (ADR-010).
- **Caddy** como única entrada pública en producción: HTTPS con Let's Encrypt para la API y Grafana en subdominios
  propios, redirección HTTP → HTTPS y cabeceras de seguridad (HSTS, `nosniff`, `X-Frame-Options`…).
- ADR-010 con el mapeo de la decisión al OWASP Top 10 (2021).

- ADR-008: restauración del alcance Sprint 1 desde el commit `238806c` y fin del arquetipo de una sola transacción.
- ADR-009: conflicto único por fórmula y patente, sin importar la versión; matiza ADR-002.
- ADR-011: TimeLimiter global de 20 s para integraciones HTTP bloqueantes, aplicado sobre la operación asíncrona completa.
- `docs/discrepancias.md` con las decisiones D-1 a D-12 y `docs/progress.md` como estado de avance del Sprint 1.
- Seguridad JWT con roles `ADMINISTRADOR`/`SERVICIO`, configuración de Resilience4j, caché Caffeine,
  reloj inyectable y clientes `RestClient` base para Materiales y la fuente de patentes (HU-00, ADR-008).
- Variables de entorno para JWT y las URL de integraciones en `.env.example` y Docker Compose.
- `POST /api/v1/auth/login`: autentica usuarios existentes y devuelve un JWT Bearer, el vencimiento en segundos,
  el usuario y el rol; credenciales inválidas responden 401 con `ProblemDetail`.
- Pruebas de autenticación para el login público, validación de entradas, respuesta genérica 401 y claims del JWT.
- Entidades y repositorios JPA para las tablas del esquema V1–V6: `conflicto`, `revision_formula`,
  `configuracion`, `evaluacion_formula`, `notificacion` y `usuario`, con sus enums de estado (P04, P17, P18).
- Excepciones de integración y reglas de negocio, con handlers `ProblemDetail` 404/409/401/403/503 (P07, P19).
- Pruebas base de `JwtProvider`, filtro de seguridad, usuarios semilla y handlers `ProblemDetail` (HU-00).
- `POST /api/v1/patentes/buscar-conflictos`: búsqueda manual restringida a `ADMINISTRADOR`, con filtros por
  clasificación/antigüedad, cálculo del porcentaje de coincidencia, persistencia de conflictos e historial,
  publicación de `ConflictoDetectadoEvent` y respuestas 200/400/401/403/404/409/503 (HU-02, D-7, ADR-009).
- Clientes funcionales `MaterialesClient` y `FuentePatentesClient` con reintentos/circuit breaker, motor
  `MotorCoincidencias`, `ConfiguracionService`, `ConflictoService` y `HistorialService` (HU-02).
- Pruebas de HU-02: motor, clientes con WireMock, controlador, servicios, dedupe por pareja y seguridad
  401/403; `mvn clean verify` pasa con 94 pruebas, JaCoCo y Checkstyle en verde.
- `GET /api/v1/patentes/estado?formulaId=form-NNN` (HU-04): estado calculado en cada consulta, sin tabla
  ni columna de estado persistida, con respuestas 200/400/401/404/500 (ADR-003, D-3 y D-8).
- `EstadoFormulaService.consultarEstado`/`recalcular`: conflicto `DETECTADO` de la versión vigente,
  evaluación abierta, revisión ausente/fallida/de otra versión y `LIBRE`, en ese orden (ADR-004).
- `BusquedaService` invoca `EstadoFormulaService.recalcular(formulaId)` al terminar la búsqueda manual (HU-02).
- Pruebas de HU-04: servicio (11 casos), controlador y integración del flujo; `mvn clean verify` pasa con
  110 pruebas, JaCoCo y Checkstyle en verde.
- Barrido automático (HU-01): planificador `Trigger` con ShedLock y recuperación de barridos huérfanos
  (`V7__shedlock.sql`), cliente de Materiales paginado en 10, orquestación por fórmula con origen
  `BARRIDO_AUTOMATICO`, reintento/timeout de 20 s y cierre con conteos y `Barrido completado exitosamente`.
- Métricas Micrometer del barrido: `patentsweep.barrido.duracion` y
  `patentsweep.barrido.formulas{resultado="con_conflicto|sin_conflicto|no_procesado"}` (D-2, D-5, D-10).
- Pruebas de HU-01: planificador, recuperación, paginación, orquestación, cierre y métricas;
  `mvn clean verify` pasa con 134 pruebas, JaCoCo y Checkstyle en verde.

### Cambiado

- Actuator se sirve en un puerto de management interno (`MANAGEMENT_PUERTO`, 9091): `/actuator/prometheus` deja de ser
  público y solo pasa sin JWT por ese puerto. `/actuator/health` sigue en el puerto de la API con `SaludController`.
- En producción la API queda en `127.0.0.1:8085`; desde Internet se entra por `https://<API_DOMINIO>`. El Security
  Group abre 80/443 y cierra el 8085.
- El documento SSM `PatentSweep-Desplegar` copia `monitoring/` y exige las variables nuevas del `.env`.
- La prueba de humo del despliegue acepta **401** sin token en `/api/v1/patentes/estado/0` además del 404: con la seguridad
  JWT de ADR-008 esa ruta ya no es pública y el CD habría marcado como fallido un despliegue sano.
- node-exporter queda tras el profile `servidor` en local (en Docker Desktop sus montajes del host pueden fallar); en
  producción siempre corre.

- `AGENTS.md` describe el estado real tras HU-00, HU-01, HU-02 y HU-04: seguridad, modelo JPA, búsqueda
  manual, estado calculado y barrido automático restaurados; Prometheus continúa restringido (ADR-008).
- La búsqueda manual usa `POST /buscar-conflictos` con el usuario del JWT; `POST /busqueda` sigue siendo solo de registro (D-7).
- Conviven `GET /estado/{id}` y `GET /estado?formulaId=...`; la primera no se rompe (D-8).
- Un conflicto se identifica por fórmula y patente, sin duplicarse aunque cambie la versión o el estado (ADR-009).

### Corregido

- Las integraciones HTTP aplican el `TimeLimiter` de Resilience4j alrededor de la llamada asíncrona completa,
  incluidos los reintentos, sin interceptar métodos bloqueantes con una anotación incompatible.
- `JwtAuthFilter` se registra como bean en `SecurityConfig` para no romper los slices `@WebMvcTest`.
- CI y Dockerfile toleran el HTTP 429 de Maven Central: caché de Maven con `restore-keys`, espera creciente
  hasta 300 s y reintentos internos de Wagon, sin intervención manual ni infraestructura adicional.
- `SecurityConfig` documenta y suprime `java:S4502`: CSRF no aplica en una API stateless con JWT en la
  cabecera y sin cookies de sesión.

---

## [1.0.0-beta.1] — 2026-11-18 · Cierre del Sprint 0

Primera versión desplegada en producción (AWS EC2) por el CD.

### Añadido

- Despliegue continuo en AWS (`.github/workflows/cd.yml`): imagen en GHCR (SHA y `latest`) desplegada **por digest** en un EC2 con Docker Compose, con health check, prueba de humo y rollback por SHA (ADR-005).
- Despliegue **sin SSH** con SSM Run Command: GitHub solo puede lanzar el documento `PatentSweep-Desplegar` (procedimiento fijo, parámetros validados y solo commits de `main`) en la instancia del proyecto, con un usuario IAM de permiso mínimo. Es compatible con la experiencia de Proyectos de AWS, que bloquea OIDC (ADR-007).
- Plantilla CloudFormation `infra/aws/cd-ssm.yml`: documento SSM de despliegue, usuario IAM de CD y perfil de instancia para SSM.
- `docker-compose.prod.yml` (imagen de GHCR y límites de memoria), `scripts/preparar-ec2.sh` (Docker, swap, agente SSM, SSH endurecido y fail2ban) y guía `docs/despliegue/EC2.md`.
- Job de CI que construye la imagen Docker en cada PR.
- ADR-005 (EC2 + Compose + GHCR), ADR-006 (arquetipo Sprint 0) y ADR-007 (CD por SSM sin SSH).

### Cambiado

- **Arquetipo Sprint 0 de una sola transacción** (ADR-006): el backend queda en registrar (`POST /api/v1/patentes/busqueda`) y consultar (`GET /api/v1/patentes/estado/{id}`) una patente.
- Las rutas usan el prefijo `/api/v1` que exige el PB.
- El CI ejecuta un solo `mvn verify` (pruebas, JaCoCo, Checkstyle y JAR), con las actions fijadas por SHA, concurrencia por PR y timeouts.
- SonarCloud configurado en `pom.xml` (organización, clave, URL y `sonar.qualitygate.wait`). Solo `config`, `model` y `dto` se excluyen de la cobertura, no del análisis.
- Limpieza del repositorio: los prompts pasan a `docs/ia/` y el análisis de HU de Materiales a `docs/referencias/`. `.idea/`, `.vscode/` y los volcados `hs_err_pid*.log` dejan de versionarse.
- README, `patent-sweep/README.md`, `GUIA_EJECUCION.md`, `CONTRIBUTING.md` y la plantilla de PR actualizados al alcance y al flujo reales del Sprint 0.
- Las actualizaciones de dependencias se hacen a mano: se probó Dependabot y se retiró, porque proponía saltos de versión mayor (Spring Boot 4, Java 24/26) como si fueran actualizaciones.

### Eliminado

- Spring Security, JWT (jjwt), Resilience4j, caché (Caffeine), AOP y PostgreSQL embebido, que el arquetipo no usa.
- `RestClientConfig`, `FuentePatentesPort` y el esquema de seguridad Bearer de OpenAPI.
- Variables `JWT_*`, `ADMIN_*`, `SERVICIO_*` y las URL de otras APIs en `docker-compose.yml` y `.env.example`.
- La búsqueda de conflictos, el estado de fórmulas, las notificaciones y la autenticación de `develop`. Se recuperan desde el commit `5d13af5` cuando las pida cada HU.

### Corregido

- La API respondía **401 en todos los endpoints**, incluido `/actuator/health`: quedaba Spring Security sin configuración tras el recorte, y por eso fallaban los healthcheck de Docker y del CD.
- SonarCloud fallaba en silencio (`continue-on-error` sin organización ni clave) y nunca había analizado el proyecto. Ahora el CI falla si el quality gate no pasa.
- El procesador de anotaciones usaba Lombok 1.18.34 fijo, que no compila con JDK 25. Ahora toma la versión de Spring Boot.
- Los badges de cobertura (98 %) y quality gate del README eran fijos. Ahora se leen de SonarCloud.
- El CI fallaba en los push a `develop`: el plan gratuito de SonarCloud solo analiza `main` y los PR. Ahora Sonar corre en esos dos casos y en `develop` se omite con aviso.
- Maven Central respondía HTTP 429 a los runners de GitHub: las dependencias se descargan en un paso previo con reintentos, y en el `Dockerfile` van en una capa propia que solo se rehace cuando cambia `pom.xml`.

### Histórico: funcionalidad de `develop` retirada por el arquetipo

Estas entradas describen trabajo integrado en `develop` antes del recorte (ADR-006). Las migraciones V3 a V6 se
conservan; el código Java se recupera desde `5d13af5`.

- API simulada de la fuente de patentes con WireMock (`GET /patentes/buscar`), con respuestas preconfiguradas: conflicto claro, límite del 50 %, patente con un material menos, fórmula sin conflicto, falla 503 y respuesta lenta para probar reintentos (HU-02).
- `FuentePatentesClient`: cliente HTTP de la fuente de patentes con Retry (3 intentos cada 5 s), Circuit Breaker y traducción de errores; se configura con `FUENTE_PATENTES_URL` (HU-02).
- Versión de la fórmula en cada conflicto (migración V3, DA-17): el conflicto es único por fórmula, patente y versión, y un descarte solo vale para la versión juzgada.
- Auditoría de cada revisión (migración V5): se guarda la versión y una foto de la fórmula (composición con porcentajes), los términos buscados, los filtros y las patentes candidatas (HU-02).
- Estado `PENDIENTE_REVISION` de HU-04, calculado a partir de las evaluaciones abiertas (`EvaluacionFormula`, solo lectura hasta HU-03).
- Restricciones de datos (migración V6): `CHECK` en la base para los estados, los formatos `form-NNN` y `PAT-NNNNNN`, la coherencia del descarte, las fechas de la patente y los valores numéricos de la configuración; las mismas reglas como validaciones en las entidades.
- ADR-001 a ADR-004 en `docs/adrs/`: API simulada de patentes, conflicto único por versión, estado calculado sin tabla `formula` y foto de la fórmula en la revisión.
- El estado de la fórmula (HU-04) se calcula en cada consulta y ya no se guarda; la consulta verifica la fórmula en Materiales y devuelve `NO_PROCESADO` si la versión vigente no se ha revisado.
- `conflicto` y `revision_formula` guardan solo el código de la fórmula (migración V4).
- Tabla `formula` y su repositorio: Materiales/Fórmulas es la dueña de los datos de la fórmula.
- `FuentePatentesMockAdapter` y `patentes_dataset.json`, reemplazados por la API simulada.

### Decisiones pendientes de confirmar

- DA-17 se aparta del PB (HU-01, 6.2 y HU-09, 3.2): el descarte de un conflicto vale solo para la versión de la fórmula juzgada.
- HU-04 devuelve `NO_PROCESADO` cuando la última revisión es de otra versión; el PB no define ese caso.

---

## [0.2.0-beta.1] — Sprint 0

### Añadido

- Módulo completo de **Patent Sweep** implementado bajo arquitectura multicapa (Controller, Service, Integration, Repository) sobre Java 21 y Spring Boot 3.5.16.
- Endpoint `POST /api/v1/patentes/busqueda` para la ejecución de búsqueda y detección de conflictos de patentes por fórmula.
- Endpoint `GET /api/v1/patentes/estado` para la consulta del estado precalculado de la fórmula con tiempos de respuesta óptimos.
- Autenticación y control de acceso basado en roles (`ADMINISTRADOR`, `SERVICIO`) mediante tokens de sesión JWT (`POST /auth/login`).
- Adaptador robusto de fuentes externas de patentes (`FuentePatentesMockAdapter`) desacoplado mediante puertos.
- Persistencia transaccional con PostgreSQL 16 y migraciones automatizadas gestionadas mediante Flyway.
- Manejo global de excepciones estandarizado bajo el formato **ProblemDetail (RFC 9457)** con correlación de `traceId`.
- Suite completa de pruebas unitarias y de integración alcanzando un **98% de cobertura en instrucciones** y **94% en ramas** con JaCoCo.
- Verificación estática de calidad de código con Checkstyle cumpliendo el estándar sin violaciones (`0 violations`).
- `docker-compose.yml` configurado con contenedor backend, base de datos PostgreSQL local y stack de monitoreo.
- Pipeline de CI/CD en GitHub Actions (`ci.yml`) con validación automática de compilación, estilo y pruebas.

### Decisiones arquitectónicas (DAS V3.0)

- **DA-01:** API REST independiente con base de datos propia en PostgreSQL 16, garantizando el desacoplamiento de la plataforma SOA de Cutit Saws.
- **DA-02:** Arquitectura multicapa (Controller - Service - Integration - Repository) sobre Java 21 y Spring Boot 3.5.16 para una estricta separación de responsabilidades.
- **DA-05:** Abstracción de la fuente externa de patentes detrás de un puerto (`FuentePatentesPort`) con un adaptador intercambiable para el dataset local.
- **DA-12:** Manejo de errores unificado mediante el estándar **ProblemDetail (RFC 9457)** acompañado de identificadores de correlación (`traceId`).

### Corregido

- Corrección de desajustes en las rutas de JSON Path y matchers de Mockito en las pruebas de integración de controladores.
- Ajuste de saltos de línea al final de los archivos (`NewlineAtEndOfFile`) exigidos estrictamente por Checkstyle.

---

## [0.1.0-beta.2] — Sprint 0

### Añadido

- Estructura inicial del repositorio backend (`patent-sweep/backend/`).
- Definición formal del Documento de Arquitectura de Software (DAS V3.0) y especificación de contratos de integración con los demás equipos.
- Configuración base de Maven con dependencias de Spring Security, Spring Data JPA y Lombok.
- Configuración inicial de la estrategia de ramificación (`main` ← `develop`).

---

[Unreleased]: https://github.com/Inmemorialake/cutit-saws-patent-sweep/compare/v1.0.0-beta.2...HEAD
[1.0.0-beta.2]: https://github.com/Inmemorialake/cutit-saws-patent-sweep/compare/v1.0.0-beta.1...v1.0.0-beta.2
[1.0.0-beta.1]: https://github.com/Inmemorialake/cutit-saws-patent-sweep/tree/v1.0.0-beta.1

<!-- Las entradas 0.2.0-beta.1 y 0.1.0-beta.2 son anteriores a la primera etiqueta y no tienen enlace. -->
