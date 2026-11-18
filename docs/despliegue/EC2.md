# Despliegue de Patent Sweep en un EC2 de AWS

Guía para dejar Patent Sweep corriendo en una instancia EC2 con Docker Compose y desplegarlo desde GitHub Actions
**sin llaves guardadas y sin el puerto 22 abierto**. Las decisiones están en
[ADR-005](../adrs/ADR-005-despliegue-ec2-compose-ghcr.md) (EC2 + Compose + GHCR) y
[ADR-007](../adrs/ADR-007-cd-con-oidc-y-ssm.md) (OIDC + SSM en lugar de SSH).

> Esta guía usa marcadores: `<REGION>`, `<IP_DEL_EC2>`, `<ID_INSTANCIA>`, `<ID_CUENTA>`.
> El repositorio es **público**: no escriba en él IP, IDs de cuenta o de instancia, ARN ni secretos.

## 1. Cómo funciona

```
PR ─► CI (verify: pruebas + JaCoCo + Checkstyle · SonarCloud + quality gate · build de Docker)
merge a main ─► CI ─► CD (.github/workflows/cd.yml)
                       │
   1. construye la imagen y la publica en GHCR: ghcr.io/inmemorialake/cutit-saws-patent-sweep:<SHA> y :latest
   2. pide un token OIDC a GitHub; AWS lo cambia por credenciales temporales del rol de despliegue
      (válido solo para este repositorio y el environment "production", que solo acepta main)
   3. aws ssm send-command ─► el agente SSM del EC2 ejecuta scripts/desplegar-ec2.sh como root:
      descarga compose y mocks del commit, fija la imagen por digest, pull + up -d, health y prueba de humo
                       ▼
EC2 (Ubuntu 24.04) ─ docker compose ─┬─ api-patent-sweep  (puerto 8085, público)
                                     ├─ postgres          (solo red interna, volumen datos-postgres)
                                     └─ apis-simuladas    (WireMock, solo red interna)
```

- **Sin llaves en GitHub.** El rol de AWS solo se puede asumir con un token OIDC de este repositorio y del
  environment `production`, y sus credenciales duran como máximo 30 minutos.
- **Sin puerto 22.** El comando llega por AWS Systems Manager: el agente del EC2 sale hacia AWS y nadie entra.
  Para administrar el servidor a mano se usa **Session Manager**.
- **Permisos mínimos.** El rol solo puede ejecutar `AWS-RunShellScript` sobre **esa** instancia y leer el resultado.
- **Imagen inmutable.** Se despliega `imagen@sha256:<digest>`, no un tag que se pueda mover.
- La imagen **se construye en GitHub Actions**, no en el EC2: una instancia pequeña no aguanta un build de Maven.
- El servidor usa `patent-sweep/docker-compose.yml` más `patent-sweep/docker-compose.prod.yml`, que reemplaza `build:`
  por la imagen de GHCR y pone límites de memoria (API 768 MB con `-Xmx448m`, Postgres 384 MB y WireMock 256 MB).
- El `.env` de producción **vive solo en el servidor** (`/opt/patent-sweep/.env`). Actions nunca lo crea ni lo modifica.
- La versión desplegada queda en `/opt/patent-sweep/imagen.env` (`IMAGE_REF=<imagen>@sha256:...`).

## 2. Crear el EC2 en el proyecto "Desarrollo-II"

Todo se crea **dentro del proyecto "Desarrollo-II"** y en **la región del proyecto** (`<REGION>`). Los nombres de los
menús pueden cambiar; lo que importa son los valores de la tabla.

| Parámetro | Valor |
|---|---|
| Proyecto / región | Desarrollo-II / `<REGION>` (la del proyecto) |
| Imagen (AMI) | **Ubuntu Server 24.04 LTS**, **64-bit (x86)**. Trae el agente de SSM preinstalado |
| Tipo de instancia | **t8i.small** (si no está disponible en la región, **t3.small**) |
| Par de llaves | **Ninguno** (*Proceed without a key pair*): el acceso es por Session Manager |
| Almacenamiento | 20 GB **gp3** |
| Metadatos | IMDSv2 obligatorio (*Metadata version: V2 only*) |
| Perfil de instancia | El que crea la plantilla de la sección 5 (se puede asociar después) |
| IP pública | Una **Elastic IP**, para que la dirección no cambie al detener e iniciar la instancia |
| Etiquetas | `Project=patent-sweep` |

> **Si elige ARM/Graviton** (t4g, c8g…), cree en GitHub la variable de repositorio `IMAGE_PLATFORMS=linux/arm64`.
> El workflow activa QEMU solo. Sin esa variable, la imagen es `linux/amd64`.

### Security Group

| Tipo | Puerto | Origen | Motivo |
|---|---|---|---|
| TCP personalizado | 8085/tcp | `0.0.0.0/0` (y `::/0` si usa IPv6) | API (`API_PUERTO`) |

- **Ninguna regla para el 22.** Session Manager y Run Command no lo necesitan.
- **Nunca** abra 5432 (PostgreSQL) ni 8080 (WireMock): solo existen en la red interna de Docker.
- La salida (*outbound*) debe permitir HTTPS (443) hacia internet. La regla por defecto, "todo el tráfico", sirve:
  el agente de SSM, GHCR y GitHub la necesitan.

## 3. Rol de despliegue y perfil de instancia (CloudFormation)

La plantilla [`infra/aws/cd-oidc-ssm.yml`](../../infra/aws/cd-oidc-ssm.yml) crea:

- El proveedor OIDC de GitHub (`token.actions.githubusercontent.com`).
- El **rol de despliegue**, que solo puede asumir `repo:Inmemorialake/cutit-saws-patent-sweep:environment:production`,
  con `ssm:SendCommand` limitado a la instancia y al documento `AWS-RunShellScript`.
- El **rol y el perfil de instancia** con `AmazonSSMManagedInstanceCore`.

Con el AWS CLI, con una sesión de administrador en la región del EC2:

```bash
aws cloudformation deploy --region <REGION> --stack-name patent-sweep-cd \
  --template-file infra/aws/cd-oidc-ssm.yml --capabilities CAPABILITY_IAM \
  --parameter-overrides IdInstancia=<ID_INSTANCIA>

aws cloudformation describe-stacks --region <REGION> --stack-name patent-sweep-cd \
  --query 'Stacks[0].Outputs' --output table
```

O desde la consola: *CloudFormation → Create stack → Upload a template file*, y marque
*I acknowledge that AWS CloudFormation might create IAM resources*.

- Si la cuenta **ya tiene** el proveedor OIDC de GitHub (solo puede haber uno), agregue
  `CrearProveedorOidc=false ArnProveedorOidcExistente=arn:aws:iam::<ID_CUENTA>:oidc-provider/token.actions.githubusercontent.com`.
- Asocie el perfil a la instancia con el comando de la salida `ComandoAsociarPerfil`, o en la consola:
  *EC2 → la instancia → Actions → Security → Modify IAM role*.
- A los pocos minutos la instancia aparece en *Systems Manager → Fleet Manager* como *Online*. Compruébelo con:
  `aws ssm describe-instance-information --region <REGION> --query 'InstanceInformationList[].{id:InstanceId,ping:PingStatus}'`

## 4. Preparar el servidor y crear el `.env`

Entre por **Session Manager**: en la consola, *EC2 → la instancia → Connect → Session Manager → Connect*.
También puede usar `aws ssm start-session --target <ID_INSTANCIA>` (requiere el
[plugin de Session Manager](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html)).

```bash
sudo -i
curl -fsSL https://raw.githubusercontent.com/Inmemorialake/cutit-saws-patent-sweep/main/scripts/preparar-ec2.sh -o /root/preparar-ec2.sh
bash /root/preparar-ec2.sh ubuntu
```

El script es **idempotente** (se puede repetir sin romper nada) y hace lo siguiente:

1. Instala Docker Engine y el plugin Compose desde el repositorio oficial de Docker.
2. Crea 2 GB de swap en `/swapfile` (`vm.swappiness=10`).
3. Endurece SSH (sin contraseña ni root) y activa fail2ban, como defensa adicional por si alguien reabre el 22.
4. Verifica que el agente de SSM esté instalado y en ejecución.
5. Crea `/opt/patent-sweep` y agrega `ubuntu` al grupo `docker`.

Después, en la misma sesión, cree el `.env`. Las claves se generan en el servidor y no se imprimen en pantalla:

```bash
cd /opt/patent-sweep
curl -fsSL https://raw.githubusercontent.com/Inmemorialake/cutit-saws-patent-sweep/main/patent-sweep/.env.example -o .env
chmod 600 .env

clave() { openssl rand -base64 48 | tr -dc 'A-Za-z0-9' | head -c "$1"; }
sed -i \
  -e "s|^SPRING_PROFILES_ACTIVE=.*|SPRING_PROFILES_ACTIVE=prod|" \
  -e "s|^DB_PASSWORD=.*|DB_PASSWORD=$(clave 32)|" \
  .env

# Comprobación sin mostrar valores: deben salir 0 marcadores "cambiar" y el perfil prod.
grep -c 'cambiar' .env; grep '^SPRING_PROFILES_ACTIVE' .env
```

> `DB_PASSWORD` solo se aplica la **primera vez** que se crea el volumen de PostgreSQL. Si la cambia después, también
> debe cambiarla dentro de la base (o borrar el volumen y perder los datos).

## 5. Configurar GitHub

En el repositorio, *Settings → Environments → **production***:

1. **Deployment branches and tags → Selected branches → `main`.** Así ningún workflow de otra rama puede usar el
   environment ni, por lo tanto, asumir el rol.
2. Opcional: **Required reviewers**, si quiere aprobar cada despliegue a mano.
3. Configure el secreto y las variables:

| Nombre | Tipo | Valor |
|---|---|---|
| `AWS_ROLE_ARN` | Secreto | Salida `ArnRolDespliegue` de la plantilla (es secreto porque contiene el ID de la cuenta) |
| `AWS_REGION` | Variable | `<REGION>` |
| `EC2_INSTANCE_ID` | Variable | `<ID_INSTANCIA>` |

```bash
gh secret set AWS_ROLE_ARN --env production --body '<ArnRolDespliegue>'
gh variable set AWS_REGION --env production --body '<REGION>'
gh variable set EC2_INSTANCE_ID --env production --body '<ID_INSTANCIA>'
```

Mientras falten, el workflow publica la imagen y **omite** el despliegue con un aviso, sin fallar.

**Paquete público en GHCR.** El servidor descarga la imagen **sin credenciales**. Después del primer push, vaya a
*github.com/Inmemorialake → Packages → cutit-saws-patent-sweep → Package settings → Change visibility → Public*.
Si queda privado, el despliegue falla en el `pull` con un mensaje que lo indica.

## 6. Primer despliegue

1. Integre en `main` (PR desde `develop`). Cuando el CI pase, **CD Despliegue en EC2** corre solo.
2. O lánzelo a mano: *Actions → CD Despliegue en EC2 → Run workflow* (rama `main`, `image_tag` vacío). Si el CI de
   ese commit no pasó, el workflow se niega a desplegar.
3. La salida del servidor aparece en el log del job (grupo *Salida del servidor*) y en
   *Systems Manager → Run Command → Command history*.
4. Compruebe desde su equipo:

```bash
curl http://<IP_DEL_EC2>:8085/actuator/health        # {"status":"UP"}
curl -i http://<IP_DEL_EC2>:8085/api/v1/patentes/estado/0   # 404 en ProblemDetail
# Swagger UI: http://<IP_DEL_EC2>:8085/swagger-ui.html
```

**Despliegue manual** desde una sesión de Session Manager (como root):

```bash
bash /ruta/a/desplegar-ec2.sh Inmemorialake/cutit-saws-patent-sweep <SHA_COMPLETO> ghcr.io/inmemorialake/cutit-saws-patent-sweep@sha256:<DIGEST>
```

## 7. Rollback a un SHA anterior

Cada despliegue publica la imagen con el **SHA completo** del commit como tag.

1. Busque el SHA de una versión buena (en el resumen de una ejecución anterior de "CD Despliegue en EC2", o con
   `git log --format=%H main`).
2. *Actions → CD Despliegue en EC2 → Run workflow* (rama `main`) con ese SHA en `image_tag`. No se construye nada:
   se resuelve el digest de esa imagen, se usan los compose de ese commit y se verifica la salud.

> El rollback cambia **solo la imagen y los compose**. Las migraciones de Flyway no se deshacen: si la versión nueva
> aplicó una migración incompatible, la versión vieja puede no arrancar (`ddl-auto: validate`).

## 8. Migrar desde el despliegue por SSH (una sola vez)

Si el servidor ya existía con el esquema anterior (ADR-005):

1. Secciones 3 y 5: crear la plantilla, asociar el perfil y cargar `AWS_ROLE_ARN`, `AWS_REGION` y `EC2_INSTANCE_ID`.
2. Confirmar que la instancia aparece *Online* en Fleet Manager y que un despliegue manual del CD termina bien.
3. En el Security Group, **borrar la regla del puerto 22**.
4. En GitHub, borrar los secretos viejos del environment `production`:
   `gh secret delete EC2_HOST --env production`, y lo mismo con `EC2_USER` y `EC2_SSH_KEY`.
5. En el servidor, quitar la llave pública de despliegue de `~ubuntu/.ssh/authorized_keys` y, en *EC2 → Key pairs*,
   borrar el par de llaves si ya no se usa.

## 9. Límite de gasto del proyecto

El proyecto "Desarrollo-II" tiene un **límite de gasto mensual**. Si se alcanza, **el proyecto se pausa el resto del
mes** y la API deja de responder. Para evitarlo:

- Detenga la instancia cuando no la use (el disco gp3 y la IPv4 pública siguen costando, pero mucho menos).
  SSM, IAM y el proveedor OIDC no tienen costo.
- Revise el consumo del proyecto con regularidad.
- Al final del semestre: termine la instancia, libere la Elastic IP y borre el stack `patent-sweep-cd`.

## 10. Problemas comunes

| Síntoma | Causa probable |
|---|---|
| "Despliegue omitido" en Actions | Faltan `AWS_ROLE_ARN`, `AWS_REGION` o `EC2_INSTANCE_ID` en el environment `production` |
| `Not authorized to perform sts:AssumeRoleWithWebIdentity` | El job no corre en el environment `production`, el repositorio cambió de nombre o el proveedor OIDC no existe |
| `InvalidInstanceId` en `send-command` | La instancia no está *Online* en Fleet Manager: falta el perfil de instancia, el agente está detenido o no hay salida a internet por 443 |
| `AccessDeniedException` en `send-command` | `EC2_INSTANCE_ID` no coincide con el `IdInstancia` de la plantilla |
| Estado `TimedOut` o `Failed` | Lea el grupo *Salida del servidor* del job. Las causas típicas son la falta de `.env` o de `/opt/patent-sweep`, o un paquete de GHCR privado |
| Health no llega a UP | En Session Manager: `cd /opt/patent-sweep && docker compose --env-file .env --env-file imagen.env -f docker-compose.yml -f docker-compose.prod.yml logs api-patent-sweep` |
| Contenedor reiniciándose (OOM) | Revise `docker stats`. Suba la instancia o baje `-Xmx` en `docker-compose.prod.yml` |
