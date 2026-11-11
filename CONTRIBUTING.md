# Guía de contribución

Este documento describe el flujo de trabajo para colaborar en nutrI-plantA. Toda contribución debe seguir este proceso.

## Estrategia de ramas

```
main         ──●──────────●──────●──── (releases tagged: v0.1.0, v0.2.0)
                ↑          ↑      ↑
                PR         PR     PR
                │          │      │
develop      ──●──●──●──●──●──●──●──── (integración estable)
                ↑  ↑  ↑     ↑
                PR PR PR    PR
                │  │  │     │
mpastrana    ──●──●──●──●───────────── (rama del director)
```

### Ramas principales

- **`main`**: rama de producción. Solo se actualiza al cerrar un sprint completo y validado. Cada merge a `main` lleva un tag semántico (`v0.1.0`, `v0.2.0`, etc.). Protegida.
- **`develop`**: rama de integración. Aquí confluye el trabajo validado y listo para integración. Protegida con regla de PR obligatorio.
- **`mpastrana`**: rama personal del director del proyecto. Aquí se desarrolla todo el trabajo de un sprint completo antes de promoverlo a `develop`.

### Reglas de los flujos

#### Trabajo dentro de `mpastrana`

- Hacer commits frecuentes con mensajes claros (ver Conventional Commits abajo).
- Validar localmente antes de cada push.
- Push directo a `mpastrana` permitido (no requiere PR).
- Mantener `mpastrana` sincronizada con `develop`: hacer `git pull origin develop` y rebase al menos una vez por semana.

#### Promoción de `mpastrana` a `develop`

- Solo cuando el sprint esté completo y estable.
- Apertura de PR desde `mpastrana` a `develop`.
- El PR debe describir las historias de usuario completadas.
- CI debe pasar todos los gates (ver abajo).
- Qodo Merge revisa automáticamente y publica observaciones.
- Atender hallazgos de Qodo antes de mergear.
- Squash merge (un commit en `develop` por sprint).

#### Promoción de `develop` a `main`

- Solo al cierre de un sprint validado y con demo funcional.
- PR desde `develop` a `main` con tag semántico (`v0.X.0`).
- Actualizar `CHANGELOG.md` con los cambios del sprint.
- Actualizar documentos de arquitectura si hubo nuevos ADRs.

## Gates de calidad

### Gate 1: validación local antes de push a `mpastrana`

Antes de cualquier push, ejecutar localmente:

```powershell
# Tests unitarios
make test

# Linter
make lint

# Type checker
make typecheck

# Validación de arquitectura (import-linter)
make arch-check
```

Si cualquiera falla, no hacer push.

### Gate 2: pipeline CI al abrir PR a `develop`

GitHub Actions ejecuta automáticamente:

1. Instalación de dependencias.
2. `ruff check` (linter).
3. `mypy` (type checker estricto).
4. `import-linter` (reglas de arquitectura hexagonal).
5. `pytest --cov` (cobertura mínima 80% sobre código nuevo).
6. Build de imágenes Docker.
7. Qodo Merge analiza el diff y publica review en el PR.

Si el pipeline falla, el PR no puede mergearse.

### Gate 3: revisión humana antes de merge

Aunque CI pase, el director del proyecto debe:

1. Leer el diff completo línea por línea.
2. Revisar las observaciones de Qodo Merge.
3. Atender al menos las observaciones marcadas como "critical" o "major".
4. Verificar que los criterios de aceptación de las HUs están cubiertos.
5. Aprobar el PR explícitamente antes de mergear.

## Convención de commits

Usar [Conventional Commits](https://www.conventionalcommits.org/):

```
<tipo>(<alcance opcional>): <descripción corta>

[cuerpo opcional con detalle]

[footer opcional con referencias]
```

### Tipos permitidos

| Tipo | Uso |
|---|---|
| `feat` | Nueva funcionalidad (corresponde a una HU) |
| `fix` | Corrección de bug |
| `docs` | Cambios de documentación |
| `refactor` | Refactor sin cambio funcional |
| `test` | Añadir o ajustar tests |
| `chore` | Tareas de mantenimiento (deps, configs) |
| `style` | Formato, sin cambio de lógica |
| `perf` | Mejora de performance |
| `ci` | Cambios en CI/CD |

### Ejemplos

```
feat(auth): implementa registro de usuarios con Argon2id

Cierra HU-01. Incluye:
- Endpoint POST /auth/register
- Validación con Pydantic
- Hash Argon2id con pwdlib
- Estado inicial PENDIENTE_APROBACION

Refs: HU-01
```

```
fix(diagnostico): corrige timeout en CNN provider

El timeout no se aplicaba correctamente al servicio externo.
Ahora se respeta CNN_VISION_TIMEOUT.
```

```
docs(adr): añade ADR-07 sobre manejo de imágenes inválidas
```

## Plantilla de PR

Al abrir un PR, llenar la plantilla `pull_request_template.md` que aparece automáticamente. Debe incluir:

- Historias de usuario cerradas.
- Criterios de aceptación verificados.
- Tests añadidos.
- Documentación actualizada (si aplica).
- ADR creado (si la PR introduce decisión arquitectónica).
- Screenshots o evidencia de pruebas manuales.

## Issues

Cada historia de usuario del backlog se materializa como un Issue en GitHub:

- Título: `[HU-NN] Título de la historia`.
- Cuerpo: usar la plantilla `historia-usuario.md`.
- Labels: `epic:E1`, `priority:must`, `points:5`.
- Milestone: `Sprint NN`.

## Definición de Listo (DoR)

Una historia está Ready cuando:

- Tiene narrativa Mike Cohn completa.
- Tiene al menos 5 criterios de aceptación verificables.
- Está estimada en story points.
- Tiene prioridad MoSCoW.
- Sus dependencias están resueltas o planificadas antes.

## Definición de Hecho (DoD)

Una historia está Done cuando:

- Código commiteado en rama feature.
- PR abierto y descripción enlazada al Issue.
- Todos los criterios de aceptación pasan (verificados con tests).
- Cobertura mínima 80% sobre código nuevo.
- ruff, mypy, import-linter pasan sin errores.
- Qodo Merge revisó el PR y los hallazgos relevantes fueron atendidos.
- OpenAPI actualizado si aplica.
- ADR actualizado si introduce decisión arquitectónica.
- PR aprobado y mergeado a `develop`.
- Demo funcional registrado en bitácora.

## Soporte y dudas

- Issues técnicos: abrir issue en GitHub con label `question`.
- Dudas arquitectónicas: discutir antes de codear, no después.
