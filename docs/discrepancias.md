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
| D-5 | Tiempos de resiliencia | 3 reintentos de 5 s + timeout de 20 s pueden exceder el límite global (S-07). | Límite global de 20 s por llamada lógica; cancela reintentos restantes. |
| D-6 | `fechaUltimaActualizacion` | Aparece en el contrato de Materiales. | No se usa en la lógica; solo se conserva en el DTO si hace falta. |
| D-7 | Ruta de búsqueda manual | `POST /busqueda` registra patentes en Sprint 0 (ADR-006); HU-02 lo define como búsqueda de conflictos. | Se mantiene el registro en `/busqueda` y se crea `/buscar-conflictos`. |
| D-8 | Ruta de estado | Sprint 0: `/estado/{id}`; HU-04: `/estado?formulaId=...`. | Conviven ambas; la consulta por identificador no se rompe. |
| D-9 | Unicidad de conflictos | ADR-002/V3: por fórmula + patente + versión; PB/HU-01: por fórmula + patente. | Dedupe por pareja en el servicio (ADR-009) y `UNIQUE (formula_id, patente_id)` en la base desde V8 (ADR-012). |
| D-10 | Columnas de `barrido` | V1: `total_fallidos`/`mensaje_error`; HU-01: `noProcesadas`/mensaje de cierre. | Se reutilizan las columnas existentes mapped a nombres de dominio; sin migración nueva. |
| D-11 | Seguridad y dependencias | ADR-006 retiró JWT, Resilience4j y caché; HU-00/HU-01 los exigen. | Se restauran con ADR-008; `/health` abierto y `/prometheus` restringido. |
| D-12 | Documentación HU | `AGENTS.md` decía que HU-00/HU-02 estaban vacías; están versionadas con contenido. | Se corrige `AGENTS.md` y se usa `docs/progress.md` como estado de avance. |
