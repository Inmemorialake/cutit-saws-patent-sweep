# Discrepancias y decisiones del Sprint 1

Este archivo recoge las desviaciones entre el DAS/backlog, las historias de usuario y el estado real del
código. La precedencia es la de `AGENTS.md`: código y documentación del Sprint 0 primero, después
convenciones de trabajo y, por último, DAS/backlog.

| ID | Tema | Fuentes en conflicto | Decisión adoptada |
|---|---|---|---|
| D-1 | Contrato de Materiales | El PB pedía `POST`; HU-01 consume `GET /formulas?estado=APROBADA&pagina={n}` (S-01). | Se usa el `GET` paginado; `nombre` → título, `aplicacionTecnica` → descripción. |
| D-2 | Tamaño de página | DAS: 50; PB/HU-01: 10. | 10 registros por página, fijos en el cliente. |
| D-3 | Nombre del estado | PB: `NO_PROCESADA`; HU-04/DAS: `NO_PROCESADO`. | `NO_PROCESADO`; contador `noProcesadas`. |
| D-4 | Endpoint `/barridos` | DAS lo propone; HU-01 lo condiciona al PO. | Fuera de alcance; resultados se consultan en HU-05/HU-07. |
| D-5 | Tiempos de resiliencia | 3 reintentos de 5 s + timeout de 20 s pueden exceder el límite global (S-07). | 20 s por intento (timeout de lectura HTTP); el tope de la llamada lógica en Resilience4j es 70 s (3 × 20 s + 2 × 5 s). |
| D-6 | `fechaUltimaActualizacion` | Aparece en el contrato de Materiales. | No se usa en la lógica; solo se conserva en el DTO si hace falta. |
| D-7 | Ruta de búsqueda manual | PB/DAS: `POST /busqueda`; el Sprint 0 la usaba para registrar patentes. | Se retiró el registro del Sprint 0; la búsqueda queda en `POST /buscar-conflictos` mientras el equipo define la ruta final (el PB pide `/busqueda`). |
| D-8 | Ruta de estado | Sprint 0: `/estado/{id}`; PB HU-04: `/estado/{formulaId}`. | Solo `GET /estado/{formulaId}` (se retiró la ruta del Sprint 0); responde con `alertasActivas`, mensajes del PB y 5 s sin reintentos para verificar la fórmula. |
| D-9 | Unicidad de conflictos | ADR-002/V3: por fórmula + patente + versión; PB/HU-01: por fórmula + patente. | Dedupe por pareja en el servicio (ADR-009) y `UNIQUE (formula_id, patente_id)` en la base desde V8 (ADR-012). HU-04 cuenta los conflictos `DETECTADO` por pareja, sin filtrar la versión de la fórmula. |
| D-10 | Columnas de `barrido` | V1: `total_fallidos`/`mensaje_error`; HU-01: `noProcesadas`/mensaje de cierre. | Se reutilizan las columnas existentes mapped a nombres de dominio; sin migración nueva. |
| D-11 | Seguridad y dependencias | ADR-006 retiró JWT, Resilience4j y caché; HU-00/HU-01 los exigen. | Se restauran con ADR-008; `/health` abierto y `/prometheus` restringido. |
| D-12 | Documentación HU | `AGENTS.md` decía que HU-00/HU-02 estaban vacías; están versionadas con contenido. | Se corrige `AGENTS.md` y se usa `docs/progress.md` como estado de avance. |
| D-13 | Interfaz web y enlace a HU-08 en HU-02 | Las especificaciones iniciales asumían entrega solo por API, pero el PB V3.01 describe interacciones GUI y navegación a HU-08 (aún no implementada). | Se habilita interfaz liviana en `static/` reutilizando la base visual de HU-04, con enlace provisional (`#conflicto/{conflictoId}`) hacia HU-08 y mensajes alineados al PB. |
