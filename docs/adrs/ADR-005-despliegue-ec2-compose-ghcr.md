# ADR-005: Despliegue en un EC2 con Docker Compose, imagen en GHCR y entrega por SSH

- **Estado:** Aceptada
- **Relacionado con:** DA-01, P22 (contenedores), guía [docs/despliegue/EC2.md](../despliegue/EC2.md)

## Contexto

Patent Sweep debe quedar accesible en AWS para la integración con los demás equipos y para la evaluación del curso. El sistema ya corre con `docker-compose.yml` (API, PostgreSQL 16 y WireMock para las APIs de otros equipos). Las restricciones:

- Cuenta personal en la **experiencia nueva de AWS con Proyectos** (proyecto "Desarrollo-II"), con créditos limitados y un **límite de gasto mensual**: si se alcanza, el proyecto se pausa ese mes.
- Sin usuarios de IAM ni AWS Organizations preparados. Las funciones avanzadas del proyecto pueden no estar activas.
- Repositorio **público**: ningún secreto ni dato de infraestructura puede quedar en el código.
- Un solo nodo es suficiente (el DAS no exige alta disponibilidad para el curso).

## Decisión

1. **Una instancia EC2** (t8i.small, x86_64, Ubuntu 24.04, 20 GB gp3, Elastic IP) que corre el mismo `docker-compose.yml`, más el override `docker-compose.prod.yml` con la imagen de GHCR y límites de memoria. PostgreSQL y WireMock solo existen en la red interna de Docker.
2. **La imagen se construye en GitHub Actions** y se publica en **GHCR** (`ghcr.io/inmemorialake/cutit-saws-patent-sweep`) con el SHA del commit y `latest`, usando el `GITHUB_TOKEN` del job. El paquete es público.
3. **Workflow `cd.yml`**, separado del CI: corre cuando el CI pasa en un push a `main` (o a mano), usa el environment `production` y entra **por SSH** (`EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`). Copia los compose y los mocks, hace `pull` y `up -d` y verifica `/actuator/health` con reintentos. Si faltan los secretos, se omite sin fallar. El rollback es el mismo workflow con `image_tag=<SHA>`.
4. El `.env` de producción lo crea y administra una persona **en el servidor**. Actions nunca lo toca.
5. Como los runners de GitHub salen desde IP variables, **el puerto 22 queda abierto** y se compensa con SSH solo por llave (sin contraseña ni root) y fail2ban (`scripts/preparar-ec2.sh`).

## Alternativas consideradas

| Alternativa | Por qué no (por ahora) |
|---|---|
| **ECS Fargate + RDS + ALB** | Es lo más "productivo", pero cuesta varias veces más (ALB y RDS cobran por hora aunque no haya tráfico) y exige roles IAM, VPC y task definitions. Excede los créditos y el alcance del curso. |
| **Elastic Beanstalk (Docker)** | Agrega una capa gestionada (y normalmente un balanceador) sin resolver la base de datos, y pide roles de servicio que la cuenta no tiene preparados. |
| **Amazon ECR en lugar de GHCR** | Exige credenciales de AWS en GitHub (OIDC + rol IAM) y un perfil de instancia para hacer pull. GHCR es gratis para repositorios públicos y usa el token del propio workflow. |
| **Compilar la imagen en el EC2** | Una instancia de 1–2 GB no aguanta un build de Maven junto con la aplicación corriendo, y el servidor necesitaría el código y el JDK. |
| **OIDC + rol IAM + SSM Run Command** | Elimina el puerto 22 y las llaves en GitHub. Es la mejora natural, pero necesita IAM y Systems Manager, que pueden requerir activar funciones avanzadas del proyecto. Queda descrita en la guía, sin implementar. |
| **Runner self-hosted en el EC2** | Evita la entrada SSH, pero en un repositorio público un PR podría ejecutar código en el servidor. |
| **Limitar el 22 a la IP del administrador** | Rompe el despliegue automático: GitHub Actions no podría conectarse. |

## Consecuencias

- Costo bajo y predecible (una instancia, un disco y una IPv4). Al detener la instancia, el gasto baja casi a cero.
- Un solo punto de falla: si la instancia cae, la API cae. Los datos de PostgreSQL viven en un volumen del disco de la instancia, **sin respaldos automáticos** (se pueden tomar snapshots del volumen EBS).
- El puerto 22 está expuesto a internet. Se mitiga con autenticación solo por llave y fail2ban, y se elimina migrando a SSM.
- La llave privada del servidor queda como secreto de GitHub en el environment `production`. Si se filtra, hay que rotar el par de llaves.
- El rollback solo cambia la imagen: las migraciones de Flyway no se revierten.
- El mismo `docker-compose.yml` sirve en local y en producción, así que no hay una segunda definición del sistema que mantener.
