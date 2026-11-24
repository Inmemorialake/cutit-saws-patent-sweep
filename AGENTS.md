# AGENTS.md — Patent Sweep API

## Identidad y prioridad

Módulo **Patent Sweep** de la plataforma Cutit Saws (curso 750021C, Universidad del Valle). Backend Java 21 + Spring Boot 3.5.16 en `patent-sweep/backend`, paquete raíz `co.edu.univalle.patentsweep`. Versión vigente: `v1.0.0-beta.1`; Sprint 0 cerrado y Sprint 1 completado en código (HU-00, HU-01, HU-02 y HU-04).

**Regla de oro: el estado real del proyecto manda.** Ante discrepancias, este es el orden de precedencia:

1. Código actual, `README.md`, `CHANGELOG.md` y ADR-006 (alcance real del Sprint 0).
2. `CONTRIBUTING.md`, `.github/workflows/` y `pom.xml` (reglas de trabajo y calidad).
3. `docs/arquitectura/DAS_Patent_Sweep_V3.0.pdf`, backlog V3.0 y ADR-001…007 (diseño y backlog, aún no implementado).

No implementes funcionalidad «porque está en el DAS»: el DAS y el backlog describen el destino, no el estado actual. Para el avance del Sprint 1, la fuente de verdad es `docs/progress.md` junto con ADR-008 y ADR-009.

## Estado actual (Sprint 1)

Sí existe:

- `POST /api/v1/patentes/busqueda`: registra una patente (`codigoPatente` con formato `PAT-NNNNNN`, `titulo`, opcionalmente `tipoProducto` y `resumen`) en estado `EN_TRAMITE` → 201/400/409.
- `GET /api/v1/patentes/estado/{id}` → 200/404; ambas rutas exigen JWT desde HU-00.
- `POST /api/v1/patentes/buscar-conflictos`: búsqueda manual (HU-02) restringida a `ADMINISTRADOR`; consulta Materiales/Fórmulas y la fuente de patentes, filtra con el motor de coincidencias, persiste conflictos/historial y devuelve 200/400/401/403/404/409/503 (D-7, ADR-009).
- `GET /api/v1/patentes/estado?formulaId=form-NNN` (HU-04) → 200/400/401/404/500; convive con `GET /estado/{id}` (D-8). El estado se calcula en cada consulta y no se persiste: conflicto vigente, evaluación abierta, versión no revisada/fallida o `LIBRE` (ADR-003/004).
- Barrido automático (HU-01): `BarridoScheduler` con `Trigger` dinámico y ShedLock, `MaterialesClient.listarAprobadas` por páginas de 10, orquestación por fórmula con origen `BARRIDO_AUTOMATICO`, recuperación de barridos huérfanos y métricas `patentsweep.barrido.duracion` / `patentsweep.barrido.formulas{resultado}` (D-2, D-5, D-10).
- Errores `ProblemDetail` (RFC 9457) con `traceId`; Swagger/OpenAPI y Actuator/Prometheus. El health check y Swagger son públicos; Prometheus quedó restringido (ADR-008).
- `POST /api/v1/auth/login` autentica usuarios existentes y devuelve un JWT Bearer con expiración, username y rol; no hay registro ni administración de usuarios.
- PostgreSQL 16 con Flyway V1–V7; entidades y repositorios mapeados para todas las tablas del esquema, con enums y excepciones propias.
- Spring Security con JWT y roles `ADMINISTRADOR`/`SERVICIO`, Resilience4j, caché Caffeine, AOP, reloj inyectable, `RestClient` y clientes funcionales para Materiales y la fuente de patentes (HU-00/HU-02, ADR-008).
- Pruebas de JWT, usuarios, filtro, handlers, motores, clientes WireMock, controladores y servicios; `mvn clean verify` pasa con 142 pruebas, JaCoCo y Checkstyle en verde.
- Docker multi-etapa, Compose, CI y CD a EC2 por SSM (sin SSH, imagen fijada por digest de GHCR).

No existe todavía:

- HU-05…HU-12: evaluación, auditoría, notificaciones ni configuración operativa.
- Registro y administración de usuarios: las cuentas iniciales se siembran desde variables de entorno y se guardan en PostgreSQL (DA-11 restaurado parcialmente, ADR-008).
- `ConflictoDetectadoEvent` ya se publica en HU-02, pero todavía no hay listeners; el resto del código anterior se recupera desde `238806c` solo cuando una HU lo pida.

## Cómo trabajar en el código

- Arquitectura multicapa `Controller → Service → Repository`. DTOs `record` en `controller.dto`, entidades con Lombok, excepciones y `@RestControllerAdvice` en `exception`.
- Inyección por constructor (`private final`), sin `@Autowired`. Identificadores, mensajes y `@DisplayName` en español; anotaciones Spring en inglés.
- Reglas de negocio y `@Transactional` en `PatenteService`; el controlador solo valida y mapea a DTO, nunca expone la entidad.
- Errores nuevos: excepción propia + handler en `GlobalExceptionHandler` con `traceId`. No filtrar mensajes internos en respuestas 500.
- Cada HU nueva lleva pruebas unitarias/de integración, contrato Swagger actualizado y entrada en `CHANGELOG.md`; si cambia una decisión de arquitectura, crear un ADR en `docs/adrs/`.

## Comandos y gates

```bash
cd patent-sweep/backend
mvn clean verify
```

Verifica compilación, pruebas, JaCoCo (≥ 80 % de líneas y ramas en `service` y `exception`), Checkstyle (0 violaciones, también en tests) y el JAR. Reporte de cobertura: `patent-sweep/backend/target/site/jacoco/index.html`.

```bash
cd patent-sweep
cp .env.example .env
docker compose up -d --build --wait
```

API en `localhost:8085`; `GUIA_EJECUCION.md` tiene más casos de prueba. El análisis de SonarCloud corre en PR y en `main` (plan gratuito): `mvn verify sonar:sonar -Dsonar.token=< token >` desde `patent-sweep/backend`. Una imagen Docker que no compile rompe el CD: construirla también antes de enviar el PR.

## Flujo Git

- Rama personal `inicialapellido` (p. ej. `agonzalez`) → PR a `develop` con una aprobación → PR de `develop` a `main` con **Create a merge commit** (nunca squash ni rebase) → etiqueta `v1.0.0-beta.N` → actualizar `CHANGELOG.md`.
- No fusionar con el CI en rojo. `main` y `develop` están protegidas: sin force-push ni borrado.
- Conventional Commits en español: `feat|fix|docs|refactor|test|chore|style|ci`, con `Refs: HU-NN` cuando aplique.
- No publicar secretos, IPs, IDs de instancia ni llaves en PRs (repositorio público). `.env` de producción solo en el servidor.
- Dependencias y GitHub Actions se actualizan a mano en PR propio; los Actions van fijados por SHA y un salto de versión mayor es una migración con ADR.

## Trampas del repositorio

- El directorio y el remoto se llaman `cutit-saws-materiales-api`, pero el contenido, el CI y el despliegue son **Patent Sweep** (`cutit-saws-patent-sweep` en README, ADRs e infra). No reescribas el proyecto por el nombre de la carpeta.
- `docs/ia/` y `docs/referencias/` describen **Materiales/Fórmulas**: son referencia de integración, no código de este módulo.
- `docs/HU/HU-00.md` y `docs/HU/HU-02.md` están versionados y con contenido; `HU-01.MD` usa extensión `.MD`. Consulta `docs/progress.md` antes de asumir qué HU está implementada.
- ADR-001…004 están a la espera de sus HU; ADR-005 y ADR-007 se leen juntos (SSH quedó sustituido por SSM).
- ADR-008 y ADR-009 dejan sin efecto parcial el arquetipo de ADR-006 y la unicidad por versión de ADR-002; el estado real se registra en `docs/progress.md`.
- No cargar datos reales de patentes en producción; con HU-00 vuelve la seguridad: health y Swagger siguen públicos, Prometheus está restringido (ADR-008).
