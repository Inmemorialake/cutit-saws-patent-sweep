# ADR-009: Conflicto único por fórmula y patente, sin importar la versión

- **Estado:** Aceptada; sustituye parcialmente ADR-002 (DA-17)
- **Relacionado con:** ADR-002, HU-01 (criterio 6.2), HU-02 (criterio 3.3), HU-09
- **Fecha:** 2026-11-23

## Contexto

El backlog y las HU exigen que un conflicto sea único por pareja `formulaId + patenteId`, aunque la fórmula
cambie de versión y aunque el conflicto existente esté `DESCARTADO` (`docs/HU/HU-01.MD:96`,
`docs/HU/HU-02.md:86`). ADR-002 decidió lo contrario —único por fórmula + patente + versión— para que un
descarte antiguo no ocultara un problema nuevo, y la migración V3 materializó ese criterio con el
`UNIQUE (formula_id, patente_id, version_formula)`.

Al restaurar HU-02 y HU-01 hay que elegir una de las dos reglas. Cambiar la restricción de la base
requeriría una migración nueva y revisar los conflictos ya persistidos; mantener la restricción actual y
hacer el dedupe en el servicio evita tocar datos históricos.

## Decisión

1. `ConflictoService.registrarNuevos` busca por `formulaId + patenteId` **sin filtrar `versionFormula`**.
2. Si ya existe un conflicto para esa pareja, esté `DETECTADO` o `DESCARTADO`, no se inserta otro y no se
   publica otro `ConflictoDetectadoEvent`.
3. La columna `version_formula` y el `UNIQUE` de V3 se conservan tal cual por compatibilidad con el
   esquema y con los datos ya migrados. No se crea una migración para esta decisión.
4. La foto de la versión evaluada sigue guardándose en `revision_formula` (ADR-004), aunque ya no afecte
   a la unicidad.

## Consecuencias

- Se cumple la regla del PB y de las HU para HU-01, HU-02 y HU-09: el barrido es idempotente y repetir una
  búsqueda no duplica conflictos.
- Un descarte pasa a ser permanente para esa pareja aunque la fórmula cambie de versión. Es la desviación
  de DA-17 que ADR-002 ya marcaba como pendiente de confirmación; queda registrada en
  `docs/discrepancias.md` y debe confirmarse con el profesor.
- ADR-002 sigue vigente para el esquema físico y los datos históricos; su regla de negocio queda
  superada por esta ADR en la capa de servicio.
- Si el PO confirma que el descarte debe valer solo por versión, se creará una nueva ADR y una migración
  posterior para alinear la base con el servicio.
