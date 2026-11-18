# Guía de contribución — Patent Sweep API

Este documento describe el flujo de trabajo y los estándares de calidad para colaborar en el módulo **Patent Sweep (Equipo 1)** de Cutit Saws. Toda contribución sigue este proceso para mantener la compatibilidad con el Documento de Arquitectura de Software (DAS V3.0) y el CI/CD verde.

## Estrategia de ramas (Git Flow)

```
main         ──●──────────●──────●──── (versiones etiquetadas: v1.0.0-beta.1, v1.0.0-beta.2…) ─► despliegue en AWS
                ↑          ↑      ↑
                PR         PR     PR
                │          │      │
develop      ──●──●──●──●──●──●──●──── (integración estable del equipo)
                ↑  ↑  ↑     ↑
                PR PR PR    PR
                │  │  │     │
napellido     ──●──●──●──●───────────── (ramas personales: primera letra del nombre + primer apellido)
```

### Ramas principales

- **`main`**: producción. Solo se actualiza al cerrar un sprint validado, y **cada merge se despliega automáticamente en el EC2** (ver [CD](#gate-4-despliegue-continuo-cd)). Cada versión lleva una etiqueta semántica (`v1.0.0-beta.X`).
- **`develop`**: integración. Aquí confluye el trabajo validado del equipo.
- **`primerainicialnombre` + `primerapellido`** (por ejemplo, `agonzalez`): ramas personales para las historias de usuario.

`main` y `develop` están protegidas con un *ruleset*: exigen **Pull Request con al menos una aprobación** de otra persona, y no permiten borrar la rama ni hacer *force-push*. Una aprobación se descarta si se suben commits nuevos al PR.

### Reglas de los flujos

#### Trabajo en ramas personales
- Commits frecuentes con mensajes claros (ver [Convención de commits](#convención-de-commits)).
- Validar localmente antes de cada `git push` ([Gate 1](#gate-1-validación-local-previa-al-push)).
- Mantener la rama sincronizada con `develop` (rebase o merge) para evitar conflictos mayores.

#### Promoción a `develop`
- **Pull Request** desde la rama personal hacia **`develop`**, usando la [plantilla](.github/pull_request_template.md): historia de usuario, criterios de aceptación y evidencia.
- El CI debe quedar **en verde**: pruebas, cobertura, Checkstyle, **quality gate de SonarCloud** y build de la imagen Docker.
- Se requiere **una aprobación** de un compañero antes del *merge*.

#### Promoción de `develop` a `main` (cierre de sprint)
1. PR de `develop` hacia `main` con el resumen del sprint, y el CI en verde.
2. Fusionar con **Create a merge commit**, nunca con *Squash* ni *Rebase*: así `main` y `develop` mantienen la misma historia y el siguiente PR de promoción no arrastra commits repetidos.
3. Comprobar que el **CD** termine en verde (*Actions → CD Despliegue en EC2*) y que la API responda en `/actuator/health`.
4. Crear la etiqueta de la versión sobre el merge commit de `main` y subirla:
   ```bash
   git fetch origin
   git tag -a v1.0.0-beta.N origin/main -m "Patent Sweep v1.0.0-beta.N — cierre del Sprint N"
   git push origin v1.0.0-beta.N
   ```
5. Actualizar `CHANGELOG.md`: la sección `[Unreleased]` pasa a `[1.0.0-beta.N] — fecha`, y se agrega su enlace al final.

---

## Gates de calidad y validación

### Gate 1: Validación local previa al push

```bash
cd patent-sweep/backend
mvn clean verify     # compila, ejecuta las pruebas y verifica JaCoCo (≥ 80 %) y Checkstyle (0 violaciones)
```

Si el comando falla, el código no se sube. Para probar la API completa: `docker compose up -d --build --wait` desde `patent-sweep/` (ver [GUIA_EJECUCION.md](patent-sweep/GUIA_EJECUCION.md)).

### Gate 2: Integración continua (`ci.yml`)

Corre en cada PR y en cada push a `main` y `develop`:

| Job | Qué verifica |
|---|---|
| **Pruebas, cobertura, estilo y Sonar** | `mvn verify` sobre **Java 21**: pruebas, JaCoCo (80 % de líneas y ramas en `service` y `exception`) y Checkstyle sin violaciones. Después, el análisis de **SonarCloud**, que **falla si el quality gate no pasa** |
| **Construir imagen Docker** | Que el `Dockerfile` construya, para que un PR no pueda romper el despliegue |

- SonarCloud (plan gratuito) analiza **los PR y `main`**. En un push a `develop` se omite con un aviso, porque el PR ya lo analizó.
- El *ruleset* todavía no marca estos checks como obligatorios: **no fusione un PR con el CI en rojo.** Se recomienda agregarlos como *required status checks* en *Settings → Rules → Rulesets*.
- Si un job falla con **HTTP 429** de Maven Central, es un límite externo (ya tiene reintentos): vuelva a ejecutarlo.

### Gate 3: Revisión por pares

Al menos **una aprobación** de otra persona del equipo, que revisa el código, la evidencia y que el PR cumpla la Definición de Hecho.

### Gate 4: Despliegue continuo (CD)

Cuando el CI pasa en un push a `main`, `cd.yml` publica la imagen en GHCR y la despliega en el EC2 por AWS Systems Manager, con health check y prueba de humo. **No hay acceso SSH** al servidor: el acceso manual es por Session Manager. Detalles, rollback y solución de problemas en [docs/despliegue/EC2.md](docs/despliegue/EC2.md).

---

## Convención de commits

Formato [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/), en español:

```text
<tipo>(<alcance opcional>): <descripción corta en imperativo>

[cuerpo opcional: qué cambia y por qué]

[footer opcional con referencias, por ejemplo: Refs: HU-02]
```

### Tipos permitidos

| Tipo | Uso principal |
|---|---|
| `feat` | Nueva funcionalidad (corresponde a una historia de usuario) |
| `fix` | Corrección de un error |
| `docs` | Cambios exclusivos en documentación (`README`, ADR, guías) |
| `refactor` | Refactorización sin alterar la funcionalidad externa |
| `test` | Adición o ajuste de pruebas |
| `chore` | Mantenimiento: dependencias, configuración, plugins |
| `style` | Formato, espacios o saltos de línea (sin afectar la lógica) |
| `ci` | Workflows de GitHub Actions |

---

## Dependencias

Las dependencias (Maven, imágenes de Docker y GitHub Actions) **se actualizan a mano**, en un PR propio:

- Las GitHub Actions están fijadas por **SHA** (con la versión en un comentario). Para actualizar una, se reemplaza el SHA por el del nuevo release.
- Un salto de versión mayor (por ejemplo, Spring Boot 4 o un Java distinto de 21) es una **migración**: se planifica como tarea del sprint y, si cambia una decisión del DAS, lleva su ADR.

---

## Definición de Hecho (DoD)

Una historia de usuario está **Done** cuando:
- [ ] El código sigue la arquitectura multicapa (Controller, Service, Integration, Repository).
- [ ] Tiene pruebas unitarias y de integración que pasan.
- [ ] La cobertura de JaCoCo cumple el umbral, en local y en el pipeline.
- [ ] Checkstyle no reporta violaciones y el **quality gate de SonarCloud** pasa.
- [ ] El PR fue revisado, aprobado por pares y fusionado en `develop`.
- [ ] La documentación de contratos y endpoints (Swagger/OpenAPI) está actualizada, y el `CHANGELOG.md` registra el cambio.
- [ ] Si introduce una decisión de arquitectura, tiene su ADR en `docs/adrs/`.
