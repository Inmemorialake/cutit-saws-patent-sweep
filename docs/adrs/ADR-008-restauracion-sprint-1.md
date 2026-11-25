# ADR-008: Restauración del alcance Sprint 1 y fin del arquetipo de una transacción

- **Estado:** Aceptada
- **Relacionado con:** ADR-006 (arquetipo Sprint 0), DA-02, DA-11, DA-12, HU-00, HU-02, HU-04 y HU-01
- **Fecha:** 2026-11-23

## Contexto

El Sprint 0 redujo el backend a una sola transacción (`POST /api/v1/patentes/busqueda` para registrar y
`GET /api/v1/patentes/estado/{id}` para consultar) y retiró seguridad, Resilience4j, caché, clientes HTTP,
el puerto `FuentePatentesPort` y sus pruebas (ADR-006). Esa decisión fue correcta para entregar y desplegar
el arquetipo, pero HU-01 da por existentes HU-00, HU-02 y HU-04 (`docs/HU/HU-01.MD:17-32`), es decir,
exactamente el código retirado.

El último estado completo antes del recorte está en el commit `238806c` (`5d13af5^1`). Las migraciones
Flyway V1–V6 se conservaron, de modo que el esquema físico ya contiene `barrido`, `conflicto`,
`revision_formula`, `configuracion` y las demás tablas; solo faltan sus entidades, repositorios y servicios.

## Decisión

1. Se restaura de forma selectiva el código de `238806c`, en este orden: HU-00 (cimientos), HU-02
   (búsqueda manual), HU-04 (estado calculado) y, por último, HU-01 (barrido periódico).
2. Se reintroducen, con versión fijada a mano: Spring Security + JWT/RBAC, Caffeine, Resilience4j + AOP,
   Testcontainers, WireMock y ArchUnit. En HU-01 se añaden además ShedLock y `@EnableScheduling`.
3. Se conservan las rutas del arquetipo: `POST /api/v1/patentes/busqueda` sigue registrando patentes y
   `GET /api/v1/patentes/estado/{id}` sigue consultando por identificador.
4. La búsqueda manual de HU-02 se expone en `POST /api/v1/patentes/buscar-conflictos` y la consulta por
   fórmula de HU-04 en `GET /api/v1/patentes/estado?formulaId=...`, para no romper los contratos vigentes.
5. Las migraciones V1–V6 no se modifican. Solo se creará una migración V7 para la tabla de ShedLock.
6. ADR-006 permanece como registro histórico del Sprint 0; deja de describir el estado actual del sistema.

## Consecuencias

- La API vuelve a tener autenticación. `/actuator/health` queda permitido para los healthcheck de Docker y
  del CD; `/actuator/prometheus` pasa a estar restringido.
- Las URLs de Materiales y de la fuente de patentes, y los secretos JWT, se configuran solo por variables
  de entorno; no se versionan valores reales.
- El gate de JaCoCo (≥ 80 % de líneas y ramas en `service` y `exception`) y Checkstyle siguen vigentes;
  el código restaurado se adapta a `ProblemDetail` con `traceId` y a DTOs `record`.
- El trabajo se fragmenta en commits cortos y se registra en `docs/progress.md` para poder reanudarlo en
  otra sesión sin perder estado.

> **Nota (2026-11-25):** la decisión de conservar `/busqueda` y `/estado/{id}` del arquetipo quedó sin efecto: ambas rutas del Sprint 0 se retiraron. La búsqueda manual sigue en `/buscar-conflictos` hasta que el equipo defina la ruta final y el estado usa `/estado/{formulaId}` (D-7, D-8).
