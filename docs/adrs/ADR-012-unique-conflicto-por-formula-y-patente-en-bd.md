# ADR-012: `UNIQUE (formula_id, patente_id)` en la tabla `conflicto`

- **Estado:** Aceptada (2026-11-25, por el equipo antes de la release del Sprint 1; amplía el límite de migraciones nuevas de ADR-008)
- **Relacionado con:** ADR-002, ADR-009, HU-01 (criterio 6.2), HU-02 (criterio 3.3), tarea de Trello 9
- **Fecha:** 2026-11-24

## Contexto

ADR-009 hizo cumplir en el servicio la regla "un conflicto por pareja `formulaId + patenteId`", pero dejó el
`UNIQUE (formula_id, patente_id, version_formula)` de V3 en la base y anotó que, si se confirmaba la regla, se
crearía una migración posterior. La tarea 9 del Sprint 1 pide revisar esa restricción para que sea por pareja,
independientemente de la versión. Con la restricción actual, dos procesos concurrentes (una búsqueda manual y
el barrido) podrían insertar la misma pareja con versiones distintas sin que la base lo impidiera.

## Decisión

1. La migración V8 reemplaza `uq_conflicto_formula_patente_version` por `uq_conflicto_formula_patente
   UNIQUE (formula_id, patente_id)`.
2. Antes de cambiar la restricción, V8 comprueba que no haya parejas repetidas. Si las hay, se detiene con un
   mensaje explícito: no borra ni fusiona filas porque `notificacion` referencia a cada conflicto.
3. La columna `version_formula` se conserva: sigue indicando con qué versión se detectó el conflicto.
4. `ConflictoService` no cambia: ya buscaba por pareja sin filtrar la versión.

## Consecuencias

- La base y el servicio aplican la misma regla; la unicidad ya no depende solo de la lógica de aplicación.
- HU-04 cuenta los conflictos `DETECTADO` por pareja, sin filtrar la versión: un conflicto detectado con la
  versión 1 sigue como alerta activa en la versión 2 hasta que se descarte (D-9). La versión solo influye en
  si la fórmula vigente cuenta como revisada (ADR-004).
- Verificado antes del despliegue (2026-11-25): la base de producción no tenía parejas repetidas, así que V8
  se aplica sin detenerse.
- Si el PO decide que el descarte vale solo por versión, habrá que revertir esta ADR con una migración nueva.
