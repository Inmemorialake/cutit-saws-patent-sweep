# Progreso del Sprint 1

Fuente de verdad para saber qué está implementado. Antes de continuar, leer `AGENTS.md`, este archivo y
`git status --short --branch`. Rama de trabajo: `jmarin`.

## Estado actual

- [x] **Fase 0 — Gobernanza**: ADR-008 (restauración Sprint 1), ADR-009 (dedupe por pareja),
      `docs/discrepancias.md`, este archivo y entrada en `CHANGELOG.md`.
- [x] **Fase 1 — HU-00: cimientos** (commits `edc4a36`, `3a9fc6d` y el cierre de esta fase)
  - [x] 1/3 dependencias, configuración, seguridad JWT, clientes base y variables de entorno.
  - [x] 2/3 modelo JPA, repositorios, enums y excepciones desde `238806c`.
  - [x] 3/3 pruebas base, `mvn clean verify` (33 pruebas, cobertura y Checkstyle en verde) y `AGENTS.md` actualizado.
- [ ] **Fase 2 — HU-02: búsqueda manual** (`feat(HU-02)`): `MotorCoincidencias`, `FuentePatentesPort`,
      `MaterialesClient.obtenerFormula`, `ConfiguracionService.criteriosVigentes`, `ConflictoService`,
      `HistorialService`, evento y endpoint `/buscar-conflictos`.
- [ ] **Fase 3 — HU-04: estado calculado** (`feat(HU-04)`): `EstadoFormulaService.recalcular`, endpoint
      `/estado?formulaId=...`, conservando `/estado/{id}`.
- [ ] **Fase 4 — HU-01: barrido** (4 commits `HU-01-1A` … `HU-01-1D`): planificador dinámico con
      ShedLock, cliente paginado, núcleo por fórmula, cierre/auditoría/métricas.

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
