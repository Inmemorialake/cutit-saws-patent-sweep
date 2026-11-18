# Despliegue de Patent Sweep en un EC2 de AWS

Guía para dejar Patent Sweep corriendo en una instancia EC2 con Docker Compose y desplegarlo desde GitHub Actions
**sin SSH y sin el puerto 22 abierto**, dentro de la **experiencia de Proyectos** de AWS. Las decisiones están en
[ADR-005](../adrs/ADR-005-despliegue-ec2-compose-ghcr.md) (EC2 + Compose + GHCR) y
[ADR-007](../adrs/ADR-007-cd-con-ssm-sin-ssh.md) (SSM con un documento propio).

> Esta guía usa marcadores: `<REGION>` (la del proyecto, por ejemplo `us-east-2`), `<IP_DEL_EC2>` y `<ID_INSTANCIA>`.
> El repositorio es **público**: no escriba en él IP, IDs de cuenta o de instancia, llaves ni secretos.

## 1. Cómo funciona

```
PR ─► CI (verify: pruebas + JaCoCo + Checkstyle · SonarCloud + quality gate · build de Docker)
merge a main ─► CI ─► CD (.github/workflows/cd.yml)
                       │
   1. construye la imagen y la publica en GHCR: ghcr.io/inmemorialake/cutit-saws-patent-sweep:<SHA> y :latest
   2. con el usuario IAM de despliegue, lanza el documento SSM "PatentSweep-Desplegar" en el EC2
      con solo dos parámetros: el SHA del commit y el digest de la imagen
   3. en el EC2 (agente SSM, como root), el documento:
      verifica que el SHA pertenezca a main · descarga compose y mocks de ese commit ·
      fija la imagen por digest · pull + up -d · health check · prueba de humo
                       ▼
EC2 (Ubuntu 24.04) ─ docker compose ─┬─ api-patent-sweep  (puerto 8085, público)
                                     ├─ postgres          (solo red interna, volumen datos-postgres)
                                     └─ apis-simuladas    (WireMock, solo red interna)
```

- **Sin puerto 22.** El agente SSM del EC2 sale hacia AWS y nadie entra. El acceso manual es por **Session Manager**.
- **Sin comandos libres.** GitHub solo puede lanzar el documento `PatentSweep-Desplegar`, cuyo procedimiento es fijo
  (`infra/aws/cd-ssm.yml`). El repositorio y la imagen están fijados en el documento y los parámetros se validan con regex.
- **Solo commits de `main`.** Así, un commit de un fork no puede traer un `docker-compose.yml` malicioso.
- **Permiso mínimo.** El usuario IAM de GitHub solo puede ejecutar ese documento en esa instancia y leer el resultado.
  Si su llave se filtra, lo único que permite es redesplegar imágenes propias desde commits de `main`.
- **Imagen inmutable.** Se despliega `imagen@sha256:<digest>`, no un tag que se pueda mover.
- La imagen **se construye en GitHub Actions**, no en el EC2: una instancia pequeña no aguanta un build de Maven.
- El servidor usa `patent-sweep/docker-compose.yml` más `patent-sweep/docker-compose.prod.yml` (imagen de GHCR y
  límites de memoria: API 768 MB con `-Xmx448m`, Postgres 384 MB y WireMock 256 MB).
- El `.env` de producción **vive solo en el servidor** (`/opt/patent-sweep/.env`). Actions nunca lo crea ni lo modifica.
- La versión desplegada queda en `/opt/patent-sweep/imagen.env` (`IMAGE_REF=<imagen>@sha256:...`).

> **¿Por qué no OIDC?** Lo ideal sería no tener ninguna llave de larga duración (OIDC + rol IAM). La experiencia de
> Proyectos lo bloquea con una SCP, y desbloquearlo exige activar funciones avanzadas, que es **irreversible** y
> **elimina el límite de gasto**. Ver ADR-007.

## 2. El EC2

| Parámetro | Valor |
|---|---|
| Proyecto / región | El proyecto del curso / `<REGION>` |
| Imagen (AMI) | **Ubuntu Server 24.04 LTS**, **64-bit (x86)**. Trae el agente de SSM preinstalado |
| Tipo de instancia | **t8i.small** (si no está disponible, **t3.small**) |
| Par de llaves | No hace falta: el acceso es por Session Manager |
| Almacenamiento | 20 GB **gp3** |
| Metadatos | IMDSv2 obligatorio (*Metadata version: V2 only*) |
| Perfil de instancia | `patent-sweep-ec2-ssm` (lo crea la plantilla de la sección 3) |
| IP pública | Una **Elastic IP**, para que la dirección no cambie al detener e iniciar la instancia |
| Etiquetas | `Name=patent-sweep`, `Project=patent-sweep` |

> **Si elige ARM/Graviton** (t4g, c8g…), cree en GitHub la variable de repositorio `IMAGE_PLATFORMS=linux/arm64`.

### Security Group

| Tipo | Puerto | Origen | Motivo |
|---|---|---|---|
| TCP personalizado | 8085/tcp | `0.0.0.0/0` (y `::/0` si usa IPv6) | API (`API_PUERTO`) |

- **Ninguna regla para el 22.**
- **Nunca** abra 5432 (PostgreSQL) ni 8080 (WireMock): solo existen en la red interna de Docker.
- La salida (*outbound*) debe permitir HTTPS (443). La regla por defecto, "todo el tráfico", sirve.

## 3. Infraestructura de despliegue (CloudFormation)

La plantilla [`infra/aws/cd-ssm.yml`](../../infra/aws/cd-ssm.yml) crea:

| Recurso | Nombre | Para qué |
|---|---|---|
| Documento SSM | `PatentSweep-Desplegar` | El procedimiento de despliegue, fijo |
| Usuario IAM | `patent-sweep-github-cd` | Identidad de GitHub Actions: solo puede ejecutar el documento en la instancia |
| Rol y perfil de instancia | `patent-sweep-ec2-ssm` | Permiten que el agente de SSM registre la instancia |

Con una sesión del CLI (`aws login`) en la región del proyecto:

```bash
aws cloudformation deploy --stack-name patent-sweep-cd \
  --template-file infra/aws/cd-ssm.yml --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides IdInstancia=<ID_INSTANCIA>

# Asociar el perfil a la instancia (una sola vez)
aws ec2 associate-iam-instance-profile --instance-id <ID_INSTANCIA> \
  --iam-instance-profile Name=patent-sweep-ec2-ssm
```

Con la instancia encendida, a los pocos minutos aparece en *Systems Manager → Fleet Manager* como *Online*:

```bash
aws ssm describe-instance-information --query 'InstanceInformationList[].{id:InstanceId,ping:PingStatus}'
```

> Si cambia el procedimiento de despliegue (el documento), vuelva a ejecutar el mismo `aws cloudformation deploy`.
> Un PR no puede cambiar lo que se ejecuta en el servidor: es intencional.

## 4. Preparar el servidor y crear el `.env`

Entre por **Session Manager**: en la consola, *EC2 → la instancia → Connect → Session Manager → Connect*, o
`aws ssm start-session --target <ID_INSTANCIA>` con el
[plugin de Session Manager](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html).

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

Después, en la misma sesión, cree el `.env`. La clave se genera en el servidor y no se imprime en pantalla:

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

1. **Deployment branches and tags → Selected branches → `main`.** Ningún workflow de otra rama podrá leer las credenciales.
2. Opcional: **Required reviewers**, si quiere aprobar cada despliegue a mano.
3. Cree la llave del usuario de despliegue y cárguela **directo** en GitHub, sin mostrarla ni guardarla en un archivo:

```bash
read -r ID SECRETO < <(aws iam create-access-key --user-name patent-sweep-github-cd \
  --query 'AccessKey.[AccessKeyId,SecretAccessKey]' --output text)
gh secret set AWS_ACCESS_KEY_ID --env production --body "$ID"
printf '%s' "$SECRETO" | gh secret set AWS_SECRET_ACCESS_KEY --env production
unset ID SECRETO

gh variable set AWS_REGION --env production --body '<REGION>'
gh variable set EC2_INSTANCE_ID --env production --body '<ID_INSTANCIA>'
```

| Nombre | Tipo | Valor |
|---|---|---|
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | Secretos | Llave del usuario `patent-sweep-github-cd` |
| `AWS_REGION` | Variable | `<REGION>` |
| `EC2_INSTANCE_ID` | Variable | `<ID_INSTANCIA>` |

Mientras falten, el workflow publica la imagen y **omite** el despliegue con un aviso, sin fallar.

**Paquete de GHCR (una sola vez).** En *github.com/Inmemorialake → Packages → cutit-saws-patent-sweep → Package settings*:

1. **Change visibility → Public.** El servidor descarga la imagen **sin credenciales**, y un paquete nuevo nace
   privado aunque el repositorio sea público (hereda los permisos, no la visibilidad). Si queda privado, el despliegue
   falla en el `pull` con un mensaje que lo indica.
2. **Manage Actions access:** el repositorio `cutit-saws-patent-sweep` debe aparecer con rol **Write**. Si el paquete
   lo creó el propio workflow, ya está. Si se creó de otra forma (por ejemplo, con un `docker push` manual), hay que
   agregarlo, o el CD falla al publicar con `denied: permission_denied: write_package`.

## 6. Desplegar

1. Integre en `main` (PR desde `develop`). Cuando el CI pase, **CD Despliegue en EC2** corre solo.
2. O lánzelo a mano: *Actions → CD Despliegue en EC2 → Run workflow* (rama `main`, `image_tag` vacío). Si el CI de
   ese commit no pasó, el workflow se niega a desplegar.
3. La salida del servidor aparece en el job (grupo *Salida del servidor*) y en
   *Systems Manager → Run Command → Command history*.
4. Compruebe desde su equipo:

```bash
curl http://<IP_DEL_EC2>:8085/actuator/health                 # {"status":"UP"}
curl -i http://<IP_DEL_EC2>:8085/api/v1/patentes/estado/0       # 404 en ProblemDetail
# Swagger UI: http://<IP_DEL_EC2>:8085/swagger-ui.html
```

**Despliegue manual** (sin GitHub), con el mismo documento:

```bash
aws ssm send-command --instance-ids <ID_INSTANCIA> --document-name PatentSweep-Desplegar \
  --parameters "Sha=<SHA_DE_MAIN>,Digest=sha256:<DIGEST>"
```

También se puede lanzar desde *Systems Manager → Run Command → Run command → PatentSweep-Desplegar*.

### Rollback a un SHA anterior

Cada imagen se publica con el **SHA completo** del commit como tag.

1. Busque el SHA de una versión buena (en el resumen de una ejecución anterior de "CD Despliegue en EC2", o con
   `git log --format=%H main`).
2. *Actions → CD Despliegue en EC2 → Run workflow* (rama `main`) con ese SHA en `image_tag`. No se construye nada:
   se resuelve el digest de esa imagen, se usan los compose de ese commit y se verifica la salud.

> El rollback cambia **solo la imagen y los compose**. Las migraciones de Flyway no se deshacen: si la versión nueva
> aplicó una migración incompatible, la versión vieja puede no arrancar (`ddl-auto: validate`).

## 7. Rotar la llave de despliegue (cada 90 días)

Un usuario IAM admite dos llaves a la vez, así que se puede rotar sin cortar el CD:

```bash
aws iam list-access-keys --user-name patent-sweep-github-cd     # anote el AccessKeyId actual
# Cree la nueva y cárguela en GitHub con el bloque de la sección 5 (paso 3).
# Lance el CD a mano y confirme que termina bien. Luego borre la vieja:
aws iam delete-access-key --user-name patent-sweep-github-cd --access-key-id <ID_VIEJO>
```

Si sospecha que la llave se filtró, **bórrela de inmediato** con `delete-access-key` y cree otra.

## 8. Migrar desde el despliegue por SSH (una sola vez)

> **Hecho el 2026-11-18** en el EC2 del proyecto: puerto 22 cerrado, secretos `EC2_*` borrados, llave SSH retirada
> del servidor y par de llaves eliminado. Esta sección queda para un servidor nuevo creado con el esquema anterior.

Si el servidor ya existía con el esquema anterior (ADR-005):

1. Secciones 3 a 5: crear el stack, asociar el perfil, encender la instancia, verificar que aparezca *Online* y cargar
   las credenciales y variables en GitHub.
2. Lanzar el CD a mano y confirmar que termina bien.
3. En el Security Group, **borrar la regla del puerto 22**.
4. En GitHub, borrar los secretos viejos del environment `production`:
   `gh secret delete EC2_HOST --env production`, y lo mismo con `EC2_USER` y `EC2_SSH_KEY`.
5. En el servidor (Session Manager), quitar la llave pública de despliegue de `~ubuntu/.ssh/authorized_keys`, y en
   *EC2 → Key pairs*, borrar el par de llaves si ya no se usa.

## 9. Calendario de operación

| Cuándo | Qué | Dónde |
|---|---|---|
| Cada 90 días (próxima: **2027-02-16**) | Rotar la llave del usuario `patent-sweep-github-cd` | Sección 7 |
| Cada semana | Revisar el consumo del proyecto frente al límite de gasto | *AWS Settings → Projects* |
| Al cambiar el procedimiento de despliegue | Actualizar el stack `patent-sweep-cd` (`aws cloudformation deploy`) | Sección 3 |
| Al terminar el semestre | Terminar la instancia, liberar la Elastic IP y borrar el stack | Sección 10 |

## 10. Límite de gasto del proyecto

El proyecto tiene un **límite de gasto mensual**. Si se alcanza, **el proyecto se pausa el resto del mes** y la API
deja de responder. Para evitarlo:

- Detenga la instancia cuando no la use (el disco gp3 y la IPv4 pública siguen costando, pero mucho menos).
  SSM, IAM y CloudFormation no tienen costo.
- Revise el consumo del proyecto con regularidad.
- Al final del semestre: termine la instancia, libere la Elastic IP y borre el stack `patent-sweep-cd`.

## 11. Problemas comunes

| Síntoma | Causa probable |
|---|---|
| "Despliegue omitido" en Actions | Faltan `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION` o `EC2_INSTANCE_ID` en el environment `production` |
| `InvalidInstanceId` en `send-command` | La instancia no está *Online* en Fleet Manager: está detenida, le falta el perfil de instancia, el agente está detenido o no hay salida por 443 |
| `AccessDeniedException` en `send-command` | `EC2_INSTANCE_ID` no coincide con el `IdInstancia` del stack, o la llave es de otro usuario |
| `InvalidParameters` | El SHA o el digest no cumplen el formato (40 caracteres hexadecimales y `sha256:` + 64) |
| "El commit … no pertenece a main" | Se intentó desplegar un commit que no es ancestro de `main` |
| Estado `TimedOut` o `Failed` | Lea el grupo *Salida del servidor* del job. Las causas típicas son la falta de `.env` o de `/opt/patent-sweep`, o un paquete de GHCR privado |
| Health no llega a UP | En Session Manager: `cd /opt/patent-sweep && docker compose --env-file .env --env-file imagen.env -f docker-compose.yml -f docker-compose.prod.yml logs api-patent-sweep` |
| Contenedor reiniciándose (OOM) | Revise `docker stats`. Suba la instancia o baje `-Xmx` en `docker-compose.prod.yml` |
| `denied: permission_denied: write_package` al publicar | El repositorio no tiene rol **Write** sobre el paquete de GHCR (sección 5, paso 2) |
| `set: Illegal option -o pipefail` en un comando manual de Run Command | `AWS-RunShellScript` ejecuta con `sh` (dash), no con bash. Envuelva el script en `bash -s <<'FIN' … FIN`, como hace `PatentSweep-Desplegar` |
| El simulador de políticas de IAM niega todo, incluso al administrador | La SCP de regiones de la experiencia de Proyectos: pase `--context-entries ContextKeyName=aws:RequestedRegion,ContextKeyValues=<REGION>,ContextKeyType=string` |
