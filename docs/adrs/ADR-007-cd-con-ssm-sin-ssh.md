# ADR-007: Despliegue continuo por SSM Run Command con un documento propio, sin SSH

- **Estado:** Aceptada. Reemplaza los puntos 3 y 5 de [ADR-005](ADR-005-despliegue-ec2-compose-ghcr.md)
- **Relacionado con:** ADR-005, guía [docs/despliegue/EC2.md](../despliegue/EC2.md), plantilla `infra/aws/cd-ssm.yml`

## Contexto

En ADR-005, el CD entraba al EC2 por SSH. Ese esquema tenía cinco debilidades:

1. **Puerto 22 abierto a internet.** Los runners de GitHub cambian de IP, así que no se podía limitar el origen.
2. **Llave privada en GitHub** (`EC2_SSH_KEY`) que da **shell completa** en el servidor si se filtra.
3. **Servidor sin verificar.** Sin `EC2_KNOWN_HOSTS`, se aceptaba la huella SSH que el servidor presentara en ese momento (TOFU).
4. **Environment `production` sin reglas.** Cualquier rama podía usar sus secretos.
5. **Actions fijadas por tag.** Un tag comprometido ejecutaría código ajeno con acceso a los secretos.

La solución de referencia es **OIDC + rol IAM + SSM**, sin credenciales de larga duración. No se puede usar porque la
cuenta está en la **experiencia de Proyectos** de AWS: una SCP administrada por AWS **niega los proveedores de identidad
OIDC** (`iam:*OpenIDConnectProvider*`, verificado con el CLI). Para levantar esa SCP habría que activar las *funciones
avanzadas*, que según la documentación de AWS **no se puede revertir**, **elimina el límite de gasto** del proyecto y
reemplaza la vista por proyectos. Con créditos de estudiante, el límite de gasto es una protección que se quiere conservar.

La experiencia de Proyectos sí permite usuarios y roles IAM para acceso programático.

## Decisión

1. **SSM Run Command en lugar de SSH.** El agente SSM del EC2 sale hacia AWS: **el puerto 22 se cierra** y el acceso
   manual se hace por Session Manager. La instancia usa un perfil con `AmazonSSMManagedInstanceCore`.
2. **Documento SSM propio (`PatentSweep-Desplegar`)** con el procedimiento de despliegue **fijo**. Solo recibe dos
   parámetros, validados con `allowedPattern`: el SHA (`^[0-9a-f]{40}$`) y el digest (`^sha256:[0-9a-f]{64}$`).
   El repositorio, el nombre de la imagen y el directorio están fijos en el documento (`allowedValues` de un solo valor).
3. **El SHA debe pertenecer a `main`.** El documento consulta la API de GitHub (`compare/<sha>...main`) y solo acepta
   `ahead` o `identical`. Sin esta verificación, un commit de un fork, que GitHub también sirve por la URL del
   repositorio, podría traer un `docker-compose.yml` malicioso.
4. **Usuario IAM de GitHub Actions con permiso mínimo:** `ssm:SendCommand` solo sobre **ese documento** y **esa
   instancia**, más la lectura del resultado. No puede usar `AWS-RunShellScript` ni ningún otro servicio. Su llave
   se guarda como secreto del environment `production`, que solo acepta la rama `main`.
5. **Imagen por digest** e imagen pública en GHCR: el servidor no recibe tokens de GitHub.
6. **Actions fijadas por SHA** (se actualizan a mano) y `permissions` mínimos por job.
7. La infraestructura vive como código en `infra/aws/cd-ssm.yml` (CloudFormation). La llave de acceso no se crea en la
   plantilla (quedaría visible en las salidas del stack): se crea con el CLI y va directo a GitHub.

## Alternativas consideradas

| Alternativa | Por qué no |
|---|---|
| OIDC + rol IAM + SSM | Lo ideal: sin credenciales de larga duración. Bloqueado por la SCP de la experiencia de Proyectos; activar funciones avanzadas es irreversible y elimina el límite de gasto. **Es la migración natural** si algún día se activan: solo cambia el paso de credenciales del workflow |
| Usuario IAM con `AWS-RunShellScript` | Una llave filtrada daría ejecución arbitraria como root, igual que SSH |
| Modelo *pull* (el EC2 revisa GHCR y se despliega solo) | No deja ninguna credencial en GitHub, pero el resultado no se ve en Actions, agrega demora y más piezas |
| Mantener SSH y endurecerlo | Sigue con el 22 abierto y una llave que da shell |
| Runner self-hosted en el EC2 | En un repositorio público, un PR podría ejecutar código en el servidor |

## Consecuencias

- Queda **una credencial de larga duración** (la llave del usuario IAM). Si se filtra, permite **desplegar o
  redesplegar** una imagen publicada en el GHCR propio a partir de un commit de `main`, y nada más: ni shell, ni otros
  comandos, ni otros servicios. Se **rota cada 90 días** (EC2.md, sección 7) y se revoca borrando la llave.
- Cada despliegue queda auditado en CloudTrail y en el historial de Run Command.
- Cambiar el procedimiento de despliegue exige actualizar el stack (el documento es la única fuente). Es intencional:
  un PR no puede cambiar lo que se ejecuta en el servidor.
- El documento depende de la API pública de GitHub (60 consultas por hora sin autenticar, de sobra para el CD).
- Si el agente SSM se detiene o la instancia pierde salida por 443, el CD falla (`InvalidInstanceId`) hasta corregirlo
  desde la consola (por ejemplo, reiniciando la instancia).
