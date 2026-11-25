# ADR-010: Monitoreo con Prometheus + Grafana y acceso público solo por HTTPS

- **Estado:** Aceptada
- **Relacionado con:** ADR-005, ADR-007, ADR-008, P23 (métricas), P28 (health check), HU-01 (métricas del barrido)
- **Fecha:** 2026-11-24

## Contexto

La API ya publicaba métricas con Micrometer en `/actuator/prometheus`, pero nadie las recolectaba ni las
visualizaba. Además había tres problemas de exposición en el EC2:

1. La API se servía por **HTTP plano** en `IP:8085`: el JWT y las claves del login viajaban sin cifrar.
2. `/actuator/prometheus` era **público** en ese puerto y revelaba detalles internos (rutas, versiones de la JVM,
   tamaño del pool de conexiones, estado de los circuit breakers).
3. Con la seguridad JWT de ADR-008, esa ruta pasa a exigir token, así que un Prometheus tampoco podría leerla.

Se quería un acceso **sencillo** (un enlace en el navegador, sin instalar nada) y **seguro**.

## Decisión

1. **Actuator en un puerto de management interno** (`management.server.port=${MANAGEMENT_PUERTO:9091}`).
   - Ese puerto nunca se publica: Prometheus lo lee por la red interna de Docker.
   - `SecurityConfig` solo permite `/actuator/prometheus` sin JWT cuando la petición llega por ese puerto. Si
     alguien lo publicara por error o igualara los puertos, las métricas seguirían pidiendo JWT.
2. **`/actuator/health` se mantiene en el puerto de la API** con `SaludController`.
   - Reutiliza el `HealthEndpoint` de Spring y devuelve solo `{"status"}`: 200 o 503.
   - El `additional-path` de Spring Boot 3.5 solo admite un segmento, y así no cambia la ruta que usan el
     healthcheck de Docker, el documento SSM y la documentación.
3. **Prometheus + node-exporter + Grafana** en el mismo Docker Compose.
   - Prometheus y node-exporter no publican puertos.
   - El datasource y los tableros están versionados en `patent-sweep/monitoring/` y son de solo lectura en la UI.
4. **Caddy como única entrada pública** (80/443).
   - Obtiene y renueva certificados de Let's Encrypt para dos subdominios propios, `API_DOMINIO` y
     `GRAFANA_DOMINIO`, que son registros `A` en Cloudflare en modo *DNS only*.
   - Redirige HTTP a HTTPS y añade cabeceras de seguridad.
   - En producción la API queda en `127.0.0.1:8085`, solo para el health check del despliegue. El 8085 sale del
     Security Group.
5. **Grafana con login obligatorio:** sin registro abierto, sin acceso anónimo, cookies `Secure` y `SameSite=strict`,
   y la clave de admin generada en el servidor (`.env`).
6. **El documento SSM** copia `monitoring/` del commit desplegado y falla con un mensaje claro si al `.env` le
   faltan las variables nuevas.

## Alternativas descartadas

- **Usuario de métricas con Basic Auth en el 8085:** requería un rol nuevo, una migración y un secreto para
  Prometheus, y la ruta seguía alcanzable desde Internet.
- **Túnel de SSM para Grafana:** es lo más cerrado, pero cada persona (incluido el profesor) necesitaría AWS CLI,
  el plugin de Session Manager y credenciales de la cuenta.
- **Grafana en un puerto HTTP público:** la contraseña viajaría en texto plano.
- **Proxy de Cloudflare (nube naranja):** complica la emisión del certificado y no oculta nada, porque la IP ya
  era conocida.

## Relación con el OWASP Top 10 (2021)

| Riesgo | Cómo lo atiende esta decisión |
|---|---|
| **A01 Pérdida de control de acceso** | Métricas solo por el puerto interno (si no, JWT). Grafana sin registro ni acceso anónimo; los tableros provisionados no se pueden modificar desde la UI. Prometheus, node-exporter, Postgres y WireMock no son alcanzables desde Internet. |
| **A02 Fallas criptográficas** | Todo el tráfico público va por TLS (Let's Encrypt, renovación automática) con HSTS. El JWT y las claves ya no viajan en claro. Cookies de Grafana `Secure`. |
| **A04 Diseño inseguro** | Defensa en profundidad: aunque el puerto de management se publicara, la cadena de seguridad seguiría exigiendo JWT fuera de él. Una sola entrada pública (Caddy). |
| **A05 Configuración de seguridad incorrecta** | El Security Group pasa de exponer 8085 a solo 80/443. Cabeceras `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` y `Permissions-Policy`; se ocultan `Server` y `X-Powered-By`. API admin de Caddy desactivada. El health público no muestra detalles. Grafana sin reportes de uso ni snapshots externos. |
| **A06 Componentes vulnerables** | Imágenes fijadas a versiones exactas y recientes (Prometheus v3.15.1, Grafana 13.3.1, node-exporter v1.12.1, Caddy 2.11.8). La imagen de la API se despliega por digest (ADR-007). |
| **A07 Fallas de identificación y autenticación** | Clave de admin de Grafana aleatoria (24 caracteres) generada en el servidor. Grafana bloquea por fuerza bruta. La API mantiene JWT con expiración (ADR-008). |
| **A08 Fallas de integridad de software y datos** | Los archivos de monitoreo llegan solo desde commits de `main` por el documento SSM (ADR-007); la configuración se monta en solo lectura. |
| **A09 Fallas de registro y monitoreo** | Es el objetivo de este ADR: tableros de errores 4xx/5xx (picos de 401/403 indican intentos de acceso), latencia, salud, circuit breakers, barrido y recursos del servidor. Caddy registra los accesos HTTPS. |
| **A10 SSRF** | Prometheus y Grafana tienen destinos fijos y provisionados; el datasource no es editable desde la UI. |

A03 (inyección) no cambia con esta decisión: se sigue atendiendo con JPA parametrizado y validación de DTO.

## Consecuencias

- Las URL públicas pasan a ser `https://<API_DOMINIO>` y `https://<GRAFANA_DOMINIO>`. Postman y la evidencia del
  curso deben usar el dominio.
- El `.env` del servidor necesita `MANAGEMENT_PUERTO`, `API_DOMINIO`, `GRAFANA_DOMINIO`, `GRAFANA_ADMIN_USUARIO` y
  `GRAFANA_ADMIN_CLAVE`, y el stack `patent-sweep-cd` debe volver a desplegarse para actualizar el documento SSM.
- El monitoreo consume unos 900 MB de límite de memoria adicional (Grafana pasó de 256 a 384 MB tras medir ~240 MB en reposo en el EC2). Cabe en la t8i.small porque el uso real es
  menor y hay 2 GB de swap; si aparece OOM se reduce la retención de Prometheus o se sube la instancia.
- `MANAGEMENT_PUERTO` debe ser distinto de `API_PUERTO`. Si coincidieran, actuator volvería al puerto público y las
  métricas quedarían detrás del JWT, así que Prometheus no podría leerlas.
