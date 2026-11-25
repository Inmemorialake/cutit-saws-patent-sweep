# ADR-006: Arquetipo Sprint 0 con una sola transacción

- **Estado:** Aceptada
- **Relacionado con:** DA-02 (multicapa), DA-11 (JWT + RBAC), DA-12 (ProblemDetail), commit `ac27942`

## Contexto

Para el Sprint 0 el equipo debía entregar un **arquetipo**: una sola transacción funcional y verificable que recorra
todas las capas (Controller → Service → Repository → PostgreSQL), con pruebas, CI y despliegue. La rama `develop`
tenía ya buena parte de HU-02 y HU-04 (búsqueda de conflictos, motor de coincidencias, estado de la fórmula,
clientes HTTP con Resilience4j, JWT), y eso excedía y complicaba lo que el Sprint 0 pide evaluar.

## Decisión

1. El backend se reduce a la transacción **registrar y consultar una patente**:
   `POST /api/v1/patentes/busqueda` (201/400/409) y `GET /api/v1/patentes/estado/{id}` (200/404), con errores en ProblemDetail y `traceId`.
2. Se quita todo lo que esa transacción no usa: seguridad (Spring Security, JWT, usuarios), Resilience4j, caché,
   clientes HTTP, el puerto `FuentePatentesPort` y sus pruebas.
3. Las migraciones Flyway V1–V6 se conservan completas: el modelo de datos del DAS no cambia y JPA solo mapea `patente`.
4. WireMock y sus `mocks/` se conservan en `docker-compose.yml` para las HU de integración (ADR-001).
5. Lo quitado se recupera del historial cuando lo pida cada HU: el último estado completo es el commit `5d13af5`.

## Consecuencias

- **La API no tiene autenticación.** En el EC2, el puerto 8085 es público y cualquiera puede registrar patentes.
  Se acepta para el Sprint 0, porque son datos de prueba sin valor. DA-11 (JWT + RBAC) vuelve con la primera HU que
  exponga datos reales o acciones administrativas. Hasta entonces, no se cargan datos reales en producción.
- `/actuator/prometheus` también es público. Cuando vuelva la seguridad, debe quedar restringido.
- La cobertura (JaCoCo) se mide sobre un código pequeño: 100 % hoy. El umbral del 80 % por paquete (`service` y `exception`) sigue vigente.
- Las rutas `/busqueda` y `/estado/{id}` se mantienen porque son las acordadas para el arquetipo, aunque `/busqueda`
  registre una patente. HU-02 redefine el contrato real de búsqueda.

> **Nota (2026-11-25):** el arquetipo de registro y consulta de patentes (`PatenteController`, `PatenteService`, `/busqueda` y `/estado/{id}`) se retiró del código al alinear el Sprint 1 con el PB. `POST /buscar-conflictos` (HU-02) y `GET /estado/{formulaId}` (HU-04) lo reemplazan; ver `docs/discrepancias.md` (D-7, D-8).
