# Progreso del Sprint 1

Fuente de verdad para saber qué está implementado. Antes de continuar, leer `AGENTS.md`, este archivo y
`git status --short --branch`. Ramas de trabajo: `jmarin` / `dtaborda` / `vyuan`.

## Estado actual

- [x] **Fase 0 — Gobernanza**: ADR-008 (restauración Sprint 1), ADR-009 (dedupe por pareja),
      ADR-010 (monitoreo y HTTPS), ADR-011 (TimeLimiter global para integraciones),
      `docs/discrepancias.md`, este archivo y entrada en `CHANGELOG.md`.
- [x] **Fase 1 — HU-00: cimientos** (commits `edc4a36`, `3a9fc6d` y el cierre de esta fase)
  - [x] 1/3 dependencias, configuración, seguridad JWT, clientes base y variables de entorno.
  - [x] 2/3 modelo JPA, repositorios, enums y excepciones desde `238806c`.
  - [x] 3/3 pruebas base, `mvn clean verify` (33 pruebas, cobertura y Checkstyle en verde) y `AGENTS.md` actualizado.
- [x] **Fase 2 — HU-02: búsqueda manual** (`feat(HU-02)`): `MotorCoincidencias`, `FuentePatentesPort`,
      `MaterialesClient.obtenerFormula`, `FuentePatentesClient`, `ConfiguracionService.criteriosVigentes`,
      `ConflictoService` (dedupe ADR-009 y evento), `HistorialService`, controlador y DTOs de
      `POST /buscar-conflictos`. `mvn clean verify` pasa con 94 pruebas, cobertura y Checkstyle en verde.
- [x] **Fase 3 — HU-04: estado calculado** (`feat(HU-04)`): `EstadoFormulaService.consultarEstado`/`recalcular`,
      endpoint `GET /estado?formulaId=...` junto a `/estado/{id}` (D-8), reglas de ADR-003/004 y llamada desde
      `BusquedaService`. `mvn clean verify` pasa con 110 pruebas, cobertura y Checkstyle en verde.
- [x] **Fase 4 — HU-01: barrido** (4 commits `HU-01-1A` … `HU-01-1D`): planificador dinámico con
      ShedLock, cliente paginado, núcleo por fórmula, cierre/auditoría/métricas. `mvn clean verify`
      pasa con 134 pruebas, cobertura y Checkstyle en verde.
- [x] **Complemento de HU-00 — Login JWT**: `POST /api/v1/auth/login` valida usuarios existentes y emite
      token Bearer con vencimiento, username y rol; `401` genérico para credenciales inválidas y `400` para
      campos vacíos. No incluye registro ni administración de cuentas. `mvn clean verify` pasa con 142
      pruebas; Checkstyle, JaCoCo y la construcción Docker están en verde. Login y uso del token también se
      comprobaron contra PostgreSQL efímero con Docker Compose.
- [x] **Corrección de resiliencia HU-01/HU-02/HU-04**: las integraciones bloqueantes se ejecutan en un pool
      acotado y el `TimeLimiter` de 20 s envuelve la llamada lógica completa, incluidos los reintentos; los
      intentos que vencen el plazo no vuelven a consultar la API externa.
- [x] **Monitoreo y HTTPS (ADR-010)**: Actuator usa un puerto interno para Prometheus, el health check público se
      conserva con `SaludController`, y el Compose incorpora Prometheus, Grafana, node-exporter y Caddy para HTTPS.
      `mvn clean verify` pasa con 146 pruebas, JaCoCo y Checkstyle en verde.
- [x] **Ajustes PB (C-1 a C-9) y cierre de HU-02**: retiro del arquetipo del Sprint 0, `alertasActivas` y mensajes de HU-04, `CON_CONFLICTO` solo con conflictos vigentes, mensaje de lista vacía, 20 s por intento y omisión de fórmulas descontinuadas. `mvn clean verify` pasa con 160 pruebas (con Docker), cobertura y Checkstyle en verde. Cierre de HU-02: pruebas de integración
      (`IntegracionBase`, `BusquedaManualIntegracionTest`, `BusquedaManualResilienciaTest`), retiro de la llamada
      redundante a `recalcular` en `BusquedaService` y migración V8 (ADR-012, propuesta).
- [x] **Complemento GUI HU-02 (`static/`)**: adaptación de la interfaz web liviana (`index.html`, `app.js`,
      `estilos.css`) reutilizando la base visual de HU-04 para la vista **Patentes > Búsqueda Manual**, botón
      **“Buscar Coincidencias”**, validación `form-NNN`, manejo de estados/mensajes del PB y enlaces provisionales
      (`#conflicto/{conflictoId}`) listos para HU-08.

## Cómo reanudar en otra sesión

```bash
git status --short --branch
git log --oneline -8
mvn -f patent-sweep/backend/pom.xml -q -DskipTests compile
```

Buscar el primer ítem sin marcar, ejecutar solo ese corte, pasar `mvn clean verify` y hacer commit con
mensaje Conventional Commit en español según `AGENTS.md`. Actualizar este archivo antes de terminar.

## Gates

- `cd patent-sweep/backend && mvn clean verify`: compilación, pruebas, JaCoCo ≥ 80 % en `service` y
  `exception`, Checkstyle sin violaciones y JAR.
- `cd patent-sweep && docker compose up -d --build --wait`: API en `localhost:8085`.
- SonarCloud en PR y `main`; no fusionar con CI en rojo.
