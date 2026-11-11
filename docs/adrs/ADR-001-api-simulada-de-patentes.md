# ADR-001: API simulada de patentes con WireMock, consumida por HTTP

- **Estado:** Aceptada
- **Relacionado con:** DA-05, HU-02

## Contexto

No existe una API real de patentes para el curso. El profesor pidió una API simulada con respuestas preconfiguradas que permita validar conflictos y diferencias mínimas. La versión anterior tenía un adaptador en Java con cuatro patentes fijas y términos de búsqueda "mágicos", que no ejercitaba la comunicación HTTP.

## Decisión

La fuente de patentes se simula con **WireMock** (`patent-sweep/mocks/mappings/patentes-*.json`), dentro del contenedor `apis-simuladas` que ya usan Materiales y Notificaciones. Patent Sweep la consume con `FuentePatentesClient` (`GET /patentes/buscar`), con Retry (3 intentos cada 5 s) y Circuit Breaker, y la URL se configura con `FUENTE_PATENTES_URL`.

Casos cubiertos: conflicto claro (3 de 4 materiales), límite del 50 %, patente con un material menos, fórmula sin conflicto, falla 503 (`FALLO-SIMULADO`) y respuesta lenta de 25 s (`LENTA-SIMULADA`).

## Consecuencias

- Se prueban de verdad los timeouts, los reintentos y los errores HTTP.
- Pasar a una API real solo exige cambiar la URL.
- Hay que mantener los mapeos, y las respuestas son fijas por término de búsqueda (no dinámicas).
- Las fechas del simulador deben usar `dd/MM/yyyy`, el formato del PB que lee la API; una prueba lee los mapeos con la configuración real para detectar errores.

## Alternativa descartada

Un adaptador en Java dentro de la aplicación: mezcla código de prueba con el de producción y no valida la integración HTTP.
