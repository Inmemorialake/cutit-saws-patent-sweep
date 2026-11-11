# ADR-003: Estado de la fórmula calculado y sin tabla `formula`

- **Estado:** Aceptada
- **Relacionado con:** DA-08, HU-04

## Contexto

La tabla `formula` guardaba una copia de la fórmula (nombre, descripción, activa) y su estado precalculado, que había que recalcular en cada búsqueda, descarte o evaluación (patrón P16). Ninguna respuesta del PB muestra el nombre ni la descripción, y Materiales/Fórmulas es la dueña de esos datos. Un estado guardado puede quedar desactualizado.

## Decisión

Se elimina la tabla `formula` (migración V4). `conflicto`, `revision_formula` y `evaluacion_formula` guardan solo el código `formula_id` como texto. HU-04 verifica la fórmula en Materiales y calcula el estado en cada consulta, en este orden:

1. Conflicto `DETECTADO` de la versión vigente: `CONFLICTO_DETECTADO`.
2. Evaluación `EN_PROCESO` o `PENDIENTE_DE_ENVIO`: `PENDIENTE_REVISION`.
3. Nunca revisada, última revisión fallida o revisada solo en otra versión: `NO_PROCESADO`.
4. En cualquier otro caso: `LIBRE`.

## Consecuencias

- El estado nunca queda desactualizado y los datos de la fórmula tienen un solo dueño.
- **HU-04 depende de Materiales:** si no responde, devuelve 503. Se mitiga con los reintentos y el Circuit Breaker.
- Cada consulta hace tres búsquedas en la base y una llamada HTTP.
- Pendiente: si Materiales no expone una lista de fórmulas, el barrido (HU-01) necesitará una tabla mínima de fórmulas vigiladas.
