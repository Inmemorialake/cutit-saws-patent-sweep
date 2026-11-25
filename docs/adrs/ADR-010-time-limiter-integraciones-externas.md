# ADR-010: TimeLimiter global para integraciones externas bloqueantes

- **Estado:** Aceptada
- **Relacionado con:** ADR-008, D-5, HU-01, HU-02 y HU-04
- **Fecha:** 2026-11-24

## Contexto

Los clientes de Materiales/Fórmulas y de la fuente de patentes usan `RestClient`, cuyas llamadas son
bloqueantes. Sus métodos devuelven DTOs o listas, pero estaban anotados directamente con
`@TimeLimiter`. Resilience4j requiere que un método con esa anotación devuelva `CompletionStage`;
al interceptar los métodos en ejecución, Spring lanzaba `IllegalReturnTypeException` y las consultas
de HU-02 y HU-04 terminaban en 500. Las pruebas unitarias anteriores construían los clientes sin el
proxy de Spring AOP, por lo que no detectaban el problema.

El timeout de lectura de `RestClient` limita cada intento HTTP, no la operación completa. Por tanto,
los tres intentos y sus esperas de cinco segundos podían exceder el límite global de 20 segundos
definido en D-5.

## Decisión

1. Se mantienen los métodos públicos síncronos de los clientes y el uso de `RestClient`; no cambian
   los contratos de los endpoints ni los servicios que consumen esos clientes.
2. Se ejecuta cada operación externa en un `CompletableFuture` dentro de un pool de integraciones
   acotado. El `TimeLimiter` de Resilience4j envuelve la operación completa, incluidos el circuit
   breaker, los reintentos y sus esperas.
3. El límite global se obtiene de la instancia configurada en Resilience4j: 20 segundos para
   `materiales` y `fuentePatentes`. Si vence, se devuelve `ServicioNoDisponibleException` para
   conservar la respuesta 503; el control del plazo impide iniciar otro intento después del límite.
4. Se conserva el timeout HTTP por intento: 5 segundos para conectar y 20 segundos para leer.
   `cancel-running-future` queda habilitado en ambas instancias del `TimeLimiter`.
5. Se usan pools independientes para notificaciones e integraciones, con el dimensionamiento
   existente (`núcleo=2`, `máximo=4`, `cola=100`); el planificador de Resilience4j usa un
   `ScheduledExecutorService` separado.

## Archivos afectados

| Archivo | Cambio |
|---|---|
| [`AsyncConfig.java`](../../patent-sweep/backend/src/main/java/co/edu/univalle/patentsweep/config/AsyncConfig.java) | Define el pool acotado de integraciones y el planificador del `TimeLimiter`. |
| [`ResilientExternalCallExecutor.java`](../../patent-sweep/backend/src/main/java/co/edu/univalle/patentsweep/integration/ResilientExternalCallExecutor.java) | Nuevo ejecutor que compone timeout global, reintentos y circuit breaker, y traduce timeout/interrupción a errores de integración. |
| [`MaterialesClient.java`](../../patent-sweep/backend/src/main/java/co/edu/univalle/patentsweep/integration/MaterialesClient.java) | Retira `@TimeLimiter` de los métodos bloqueantes y delega las llamadas al ejecutor. |
| [`FuentePatentesClient.java`](../../patent-sweep/backend/src/main/java/co/edu/univalle/patentsweep/integration/FuentePatentesClient.java) | Aplica el mismo flujo a las consultas de la fuente de patentes. |
| [`RestClientConfig.java`](../../patent-sweep/backend/src/main/java/co/edu/univalle/patentsweep/config/RestClientConfig.java) | Aclara la diferencia entre el timeout por intento y el límite global. |
| [`application.yml`](../../patent-sweep/backend/src/main/resources/application.yml) | Configura 20 segundos globales y la cancelación de la espera de la tarea en ambas integraciones. |
| [`MaterialesClientTest.java`](../../patent-sweep/backend/src/test/java/co/edu/univalle/patentsweep/integration/MaterialesClientTest.java) y [`FuentePatentesClientTest.java`](../../patent-sweep/backend/src/test/java/co/edu/univalle/patentsweep/integration/FuentePatentesClientTest.java) | Adaptan las pruebas de cliente y verifican la configuración del timeout. |
| [`ResilientExternalCallExecutorTest.java`](../../patent-sweep/backend/src/test/java/co/edu/univalle/patentsweep/integration/ResilientExternalCallExecutorTest.java) | Comprueba los reintentos normales y que el límite global impida una llamada tardía. |
| [`CHANGELOG.md`](../../CHANGELOG.md), [`AGENTS.md`](../../AGENTS.md) y [`docs/progress.md`](../progress.md) | Registran la decisión y el estado de validación del cambio. |

## Consecuencias

- HU-01, HU-02 y HU-04 ya no dependen de que `@TimeLimiter` intercepte métodos síncronos; las
  llamadas externas respetan el límite global documentado en D-5.
- Las peticiones REST esperan la respuesta de la integración o el vencimiento del límite. El pool
  separado acota el trabajo concurrente y evita competir con el pool de notificaciones.
- Si un timeout vence mientras `RestClient` ya está bloqueado dentro de una petición HTTP, la
  respuesta al llamador respeta el límite global; la liberación del hilo también depende del timeout
  de lectura de esa petición. No se inician reintentos posteriores al vencimiento.
- No se modifican endpoints, DTOs, esquema de PostgreSQL ni migraciones.

## Validación

- `mvn clean verify`: 142 pruebas, JaCoCo y Checkstyle en verde; 0 violaciones de Checkstyle.
- Docker Compose: imagen reconstruida y API saludable con WireMock local.
- Pruebas de humo: HU-04 con `form-001` respondió 200; HU-02 con `form-003` respondió 409, como
  corresponde a una fórmula descontinuada.
