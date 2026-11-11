# Despliegue de Patent Sweep en un EC2 de AWS

Guía para dejar Patent Sweep corriendo en una instancia EC2 con Docker Compose. La decisión y sus alternativas están
en [ADR-005](../adrs/ADR-005-despliegue-ec2-compose-ghcr.md).

> Esta guía usa marcadores: `<REGION>`, `<IP_DEL_EC2>`, `<TU_IP>`, `<USUARIO_EC2>`, `<RUTA_DE_LA_LLAVE>`.
> El repositorio es **público**: no escriba en él IP, IDs de cuenta o de instancia, nombres de llaves ni secretos.

## 1. Cómo funciona

```
push a main ─► CI (pruebas, JaCoCo, Checkstyle, SonarCloud) ─► CD (.github/workflows/cd.yml)
                                                                 │
                         1. construye la imagen y la publica en GHCR
                            ghcr.io/inmemorialake/cutit-saws-patent-sweep:<SHA> y :latest
                         2. por SSH: copia los compose y los mocks a /opt/patent-sweep,
                            hace docker compose pull + up -d y verifica /actuator/health
                                                                 ▼
EC2 (Ubuntu 24.04) ─ docker compose ─┬─ api-patent-sweep  (puerto 8085, público)
                                     ├─ postgres          (solo red interna, volumen datos-postgres)
                                     └─ apis-simuladas    (WireMock, solo red interna)
```

- La imagen **se construye en GitHub Actions**, no en el EC2: una instancia pequeña no aguanta un build de Maven.
- El servidor usa `patent-sweep/docker-compose.yml` más el override `patent-sweep/docker-compose.prod.yml`, que
  reemplaza `build:` por la imagen de GHCR y pone límites de memoria (API 768 MB con `-Xmx448m`, Postgres 384 MB,
  WireMock 256 MB).
- El `.env` de producción **vive solo en el servidor** (`/opt/patent-sweep/.env`). Actions nunca lo crea ni lo modifica.
- La versión desplegada queda registrada en `/opt/patent-sweep/imagen.env` (`IMAGE_NAME` e `IMAGE_TAG`).

## 2. Crear el EC2 en el proyecto "Desarrollo-II"

La cuenta usa la **experiencia nueva de AWS con Proyectos**, no la consola clásica. Todo se crea **dentro del
proyecto "Desarrollo-II"** y en **la región del proyecto** (`<REGION>`). Esta guía no supone usuarios de IAM, AWS
Organizations ni una región fija. Los nombres exactos de los menús pueden cambiar; los valores de abajo son los que
importan.

| Parámetro | Valor |
|---|---|
| Proyecto / región | Desarrollo-II / `<REGION>` (la del proyecto) |
| Imagen (AMI) | **Ubuntu Server 24.04 LTS**, arquitectura **64-bit (x86)** |
| Tipo de instancia | **t8i.small** (Intel, x86_64). Si no está disponible o no entra en el plan gratuito/créditos de la región, use **t3.small** |
| Par de llaves | Cree uno nuevo (ED25519 o RSA) y guarde el `.pem` **fuera del repositorio**, por ejemplo en `~/.ssh/`, con `chmod 400` |
| Almacenamiento | 20 GB **gp3** |
| Metadatos | IMDSv2 obligatorio (*Metadata version: V2 only*) |
| IP pública | Asigne una **Elastic IP** para que la dirección no cambie al detener e iniciar la instancia |
| Etiquetas | `Project=patent-sweep` |

> **Si elige ARM/Graviton** (familias con `g`, como t4g o c8g): la imagen debe construirse para `linux/arm64`. En
> GitHub, en *Settings → Secrets and variables → Actions → Variables*, cree la variable `IMAGE_PLATFORMS` con el valor
> `linux/arm64` (o `linux/amd64,linux/arm64` para ambas). El workflow activa QEMU automáticamente. Las imágenes base
> (Temurin, PostgreSQL y WireMock) existen para las dos arquitecturas. Sin esa variable, la imagen es `linux/amd64`.

### Security Group

| Tipo | Puerto | Origen | Motivo |
|---|---|---|---|
| SSH | 22/tcp | `0.0.0.0/0` | Despliegue desde GitHub Actions (ver la nota) |
| TCP personalizado | 8085/tcp | `0.0.0.0/0` (y `::/0` si usa IPv6) | API (`API_PUERTO`) |

**Nunca** abra 5432 (PostgreSQL) ni 8080 (WireMock): solo existen en la red interna de Docker y `docker-compose.yml`
no los publica.

> **Nota sobre el puerto 22.** Los runners de GitHub Actions salen desde IP que cambian, así que el 22 no se puede
> limitar a una sola IP sin romper el despliegue automático. Para compensarlo, `scripts/preparar-ec2.sh` desactiva el
> acceso por contraseña y el de root, y activa fail2ban. Si prefiere limitar el 22 a `<TU_IP>/32`, el despliegue
> automático dejará de funcionar y tendrá que usar la alternativa sin SSH (sección 9) o desplegar a mano (sección 6).

## 3. Preparar el servidor

Desde su equipo:

```bash
scp -i <RUTA_DE_LA_LLAVE> scripts/preparar-ec2.sh ubuntu@<IP_DEL_EC2>:~
ssh -i <RUTA_DE_LA_LLAVE> ubuntu@<IP_DEL_EC2>
sudo bash ~/preparar-ec2.sh ubuntu
exit        # vuelva a entrar para que el grupo docker se aplique
```

El script es **idempotente** (se puede repetir sin romper nada) y hace lo siguiente:

1. Instala Docker Engine y el plugin Compose desde el repositorio oficial de Docker.
2. Crea 2 GB de swap en `/swapfile` (`vm.swappiness=10`).
3. Desactiva el SSH con contraseña y el de root, y activa fail2ban para sshd.
4. Crea `/opt/patent-sweep` con dueño `ubuntu` y agrega `ubuntu` al grupo `docker`.

## 4. Crear el `.env` de producción

En el servidor, como `ubuntu`. Las claves se generan allí mismo con `openssl` y no se imprimen en pantalla:

```bash
cd /opt/patent-sweep
curl -fsSL https://raw.githubusercontent.com/Inmemorialake/cutit-saws-patent-sweep/main/patent-sweep/.env.example -o .env
chmod 600 .env

clave() { openssl rand -base64 48 | tr -dc 'A-Za-z0-9' | head -c "$1"; }
sed -i \
  -e "s|^SPRING_PROFILES_ACTIVE=.*|SPRING_PROFILES_ACTIVE=prod|" \
  -e "s|^DB_PASSWORD=.*|DB_PASSWORD=$(clave 32)|" \
  -e "s|^JWT_SECRET=.*|JWT_SECRET=$(clave 64)|" \
  -e "s|^ADMIN_CLAVE=.*|ADMIN_CLAVE=$(clave 24)|" \
  -e "s|^SERVICIO_CLAVE=.*|SERVICIO_CLAVE=$(clave 24)|" \
  .env

# Comprobación sin mostrar valores: deben salir 0 marcadores "cambiar" y el perfil prod.
grep -c 'cambiar' .env; grep '^SPRING_PROFILES_ACTIVE' .env
```

- Las URL de Materiales, Notificaciones y la fuente de patentes siguen apuntando a `http://apis-simuladas:8080`
  (WireMock) hasta que los otros equipos publiquen sus APIs.
- `DB_PASSWORD` solo se aplica la **primera vez** que se crea el volumen de PostgreSQL. Si la cambia después, también
  debe cambiarla dentro de la base (o borrar el volumen y perder los datos).
- Para consultar la clave del administrador cuando la necesite: `grep ^ADMIN_CLAVE /opt/patent-sweep/.env`, en el
  servidor y nunca en un log compartido.

## 5. Configurar GitHub

En el repositorio: *Settings → Environments → New environment* → **`production`**. Opcionalmente, limite el environment
a la rama `main` (*Deployment branches*) y agregue revisores requeridos.

| Secreto (environment `production`) | Valor |
|---|---|
| `EC2_HOST` | `<IP_DEL_EC2>` (la Elastic IP) |
| `EC2_USER` | `ubuntu` |
| `EC2_SSH_KEY` | Contenido completo del `.pem` |
| `EC2_KNOWN_HOSTS` (opcional) | Salida de `ssh-keyscan -H <IP_DEL_EC2>`. Si falta, el workflow confía en la llave que el servidor presente en ese momento |

Con el CLI de GitHub, sin mostrar la llave:

```bash
gh secret set EC2_HOST --env production --body '<IP_DEL_EC2>'
gh secret set EC2_USER --env production --body 'ubuntu'
gh secret set EC2_SSH_KEY --env production < <RUTA_DE_LA_LLAVE>
```

Mientras falten estos secretos, el workflow publica la imagen y **omite** el despliegue con un aviso, sin fallar.

**Paquete público en GHCR.** Después del primer push de la imagen, vaya a *github.com/Inmemorialake → Packages →
cutit-saws-patent-sweep → Package settings → Change visibility → Public*. El despliegue igual inicia sesión en GHCR
con el token temporal del job, pero con el paquete público cualquiera puede descargar la imagen sin credenciales.

## 6. Primer despliegue

1. Integre en `main` (PR desde `develop`). Cuando el CI pase, el workflow **CD Despliegue en EC2** corre solo.
2. O lánzelo a mano: *Actions → CD Despliegue en EC2 → Run workflow* (rama `main`, `image_tag` vacío). Si el CI de
   ese commit no pasó, el workflow se niega a desplegar.
3. Compruebe:

```bash
curl http://<IP_DEL_EC2>:8085/actuator/health        # {"status":"UP"}
# Swagger UI: http://<IP_DEL_EC2>:8085/swagger-ui.html
```

Despliegue manual desde el servidor (por ejemplo, si el puerto 22 está cerrado para Actions):

```bash
cd /opt/patent-sweep
docker compose --env-file .env --env-file imagen.env -f docker-compose.yml -f docker-compose.prod.yml pull
docker compose --env-file .env --env-file imagen.env -f docker-compose.yml -f docker-compose.prod.yml up -d --no-build
```

## 7. Rollback a un SHA anterior

Cada despliegue publica la imagen con el **SHA completo** del commit como tag.

1. Busque el SHA de una versión buena: en *Actions*, en una ejecución anterior de "CD Despliegue en EC2", o con
   `git log --format=%H main`.
2. *Actions → CD Despliegue en EC2 → Run workflow* (rama `main`) e ingrese ese SHA en `image_tag`. No se construye
   nada: se despliega la imagen que ya existe y se verifica el health.

> El rollback cambia **solo la imagen**. Las migraciones de Flyway no se deshacen: si la versión nueva aplicó una
> migración incompatible con la anterior, la versión vieja puede no arrancar (`ddl-auto: validate`).

## 8. Límite de gasto del proyecto

El proyecto "Desarrollo-II" tiene un **límite de gasto mensual**. Si se alcanza, **el proyecto se pausa durante el
resto de ese mes** y la API deja de responder. Para no llegar a ese punto:

- Detenga la instancia cuando no la use (el disco gp3 y la IPv4 pública siguen costando, pero mucho menos).
- Revise el consumo del proyecto con regularidad.
- Al final del semestre, termine la instancia y libere la Elastic IP.

## 9. Alternativa sin SSH: OIDC + rol IAM + SSM Run Command (no implementada)

Elimina el puerto 22 por completo:

1. Registrar a GitHub como proveedor OIDC en la cuenta (`token.actions.githubusercontent.com`).
2. Crear un rol IAM que solo pueda asumir este repositorio y la rama `main` (condición `sub` del token), con permiso
   para `ssm:SendCommand` sobre la instancia y para leer el resultado.
3. Darle a la instancia un perfil con `AmazonSSMManagedInstanceCore` para que el agente de SSM (preinstalado en
   Ubuntu de AWS) la registre.
4. En el workflow, reemplazar los pasos SSH por `aws-actions/configure-aws-credentials` (OIDC, sin llaves) y
   `aws ssm send-command` con el mismo script de pull, up y health check.

Ventajas: sin puerto 22, sin llaves privadas en GitHub y con auditoría en AWS. Costo: más configuración, y en la
experiencia de Proyectos **puede requerir activar funciones avanzadas del proyecto** (IAM, proveedores OIDC y
Systems Manager), que no están disponibles por defecto. Ver [ADR-005](../adrs/ADR-005-despliegue-ec2-compose-ghcr.md).

## 10. Problemas comunes

| Síntoma | Causa probable |
|---|---|
| "Despliegue omitido" en Actions | Faltan los secretos del environment `production` |
| `Permission denied (publickey)` | `EC2_USER` o `EC2_SSH_KEY` incorrectos |
| `Falta /opt/patent-sweep` | No se ejecutó `scripts/preparar-ec2.sh` |
| `No existe /opt/patent-sweep/.env` | Falta el paso 4 |
| Health no llega a UP | `docker compose ... logs api-patent-sweep` en el servidor: suele ser el `.env` (clave de BD, JWT menor de 32 caracteres) |
| Contenedor reiniciándose (OOM) | Revise `docker stats`. Suba la instancia o baje `-Xmx` en `docker-compose.prod.yml` |
