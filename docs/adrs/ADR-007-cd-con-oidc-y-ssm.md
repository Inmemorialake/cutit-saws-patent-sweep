# ADR-007: Despliegue continuo con OIDC y SSM Run Command, sin SSH

- **Estado:** Aceptada. Reemplaza los puntos 3 y 5 de [ADR-005](ADR-005-despliegue-ec2-compose-ghcr.md)
- **Relacionado con:** ADR-005, guía [docs/despliegue/EC2.md](../despliegue/EC2.md), plantilla `infra/aws/cd-oidc-ssm.yml`

## Contexto

En ADR-005, el CD entraba al EC2 por SSH. Ese esquema tenía cinco debilidades:

1. **Puerto 22 abierto a internet.** Los runners de GitHub cambian de IP, así que no se podía limitar el origen.
2. **Llave privada permanente en GitHub** (`EC2_SSH_KEY`). Si se filtra, da shell en el servidor hasta que alguien la rote.
3. **Servidor sin verificar.** Sin `EC2_KNOWN_HOSTS`, se aceptaba la huella SSH que el servidor presentara en ese momento (TOFU).
4. **Environment `production` sin reglas.** Cualquier rama podía usar sus secretos.
5. **Actions fijadas por tag** (`@v4`). Un tag comprometido ejecutaría código ajeno con acceso a los secretos.

Ya se confirmó que la cuenta permite crear recursos de IAM, que era el bloqueo que ADR-005 mencionaba.

## Decisión

1. **OIDC en lugar de llaves.** GitHub emite un token firmado por ejecución y `aws-actions/configure-aws-credentials`
   lo cambia por credenciales temporales (30 min) de un rol IAM. La política de confianza exige
   `aud = sts.amazonaws.com` y `sub = repo:Inmemorialake/cutit-saws-patent-sweep:environment:production`.
2. **SSM Run Command en lugar de SSH.** El rol solo tiene `ssm:SendCommand` sobre esa instancia y el documento
   `AWS-RunShellScript`, más la lectura del resultado. El agente SSM del EC2 ejecuta `scripts/desplegar-ec2.sh`.
   **El puerto 22 se cierra** y el acceso manual se hace por Session Manager.
3. **Imagen por digest.** El CD despliega `imagen@sha256:...` y lo guarda en `imagen.env`. El paquete de GHCR es
   público, así que el servidor no recibe tokens de GitHub.
4. **Environment `production` limitado a `main`.** Como el `sub` del token incluye el environment, ninguna otra rama
   puede asumir el rol.
5. **Actions fijadas por SHA**, con `permissions` mínimos por job. Dependabot las mantiene al día.
6. **El script de despliegue sale del commit del workflow** (main), y los compose del commit que se despliega. Así, un
   rollback a un commit viejo usa el procedimiento vigente.
7. La infraestructura de IAM vive como código en `infra/aws/cd-oidc-ssm.yml` (CloudFormation).

## Alternativas consideradas

| Alternativa | Por qué no |
|---|---|
| Mantener SSH y endurecerlo (known_hosts obligatorio, llave con `command=` forzado) | Mejora, pero sigue con el 22 abierto y una llave permanente en GitHub |
| Runner self-hosted en el EC2 | En un repositorio público, un PR podría ejecutar código en el servidor |
| ECR con perfil de instancia | Evitaría depender de que el paquete de GHCR sea público, pero agrega un servicio y su costo. Se puede adoptar si la imagen deja de ser pública |
| ECS/Fargate o CodeDeploy | Exceden los créditos y el alcance del curso (ver ADR-005) |

## Consecuencias

- No hay ningún secreto de larga duración que dé acceso al servidor. Revocar el acceso es borrar el stack o el rol.
- Cada despliegue queda auditado en CloudTrail y en el historial de Run Command.
- Configurarlo requiere un paso más: desplegar la plantilla y asociar el perfil de instancia (ver EC2.md, sección 3).
- La salida de un comando SSM se trunca a 24.000 caracteres en la consola. Alcanza para el despliegue; los logs
  completos de la API se leen en el servidor.
- Si el agente SSM se detiene o la instancia pierde salida por 443, el CD falla (`InvalidInstanceId`) hasta corregirlo
  desde la consola de EC2 (por ejemplo, reiniciando la instancia).
