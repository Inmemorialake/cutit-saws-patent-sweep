# Guía de contribución — Patent Sweep API

Este documento describe el flujo de trabajo y los estándares de calidad para colaborar en el módulo **Patent Sweep (Equipo 1)** de Cutit Saws. Toda contribución debe seguir rigurosamente este proceso para asegurar la compatibilidad con el Documento de Arquitectura de Software (DAS V3.0) y el éxito en la integración continua.

## Estrategia de ramas (Git Flow)

```
main         ──●──────────●──────●──── (releases tagados: v1.0.0-beta.1, v1.0.0-beta.2)
                ↑          ↑      ↑
                PR         PR     PR
                │          │      │
develop      ──●──●──●──●──●──●──●──── (integración estable del equipo)
                ↑  ↑  ↑     ↑
                PR PR PR    PR
                │  │  │     │
napellido     ──●──●──●──●───────────── (ramas personales de desarrollo, etiquetadas con la primera letra del nombre y el primer apellido del desarrollador)
```

### Ramas principales

- **`main`**: Rama de producción y versiones estables. Solo se actualiza al cerrar un sprint completo y validado. Cada merge a `main` lleva un tag semántico (`v1.0.0-beta.X`). Se encuentra protegida.
- **`develop`**: Rama de integración. Aquí confluye todo el trabajo validado por el equipo listo para pruebas globales. Se encuentra protegida con regla de Pull Request obligatorio y aprobación por pares.
- **`primerainicialnombre` + `primerapellido`**: Ramas personales o de subtareas para el desarrollo de historias de usuario específicas.

### Reglas de los flujos

#### Trabajo en ramas de desarrollo (`primerainicialnombre` + `primerapellido`)
- Hacer commits frecuentes con mensajes claros y descriptivos.
- Validar y compilar localmente antes de realizar cualquier `git push`.
- Mantener la rama sincronizada con `develop` frecuentemente mediante rebase o merge para evitar conflictos mayores.

#### Promoción a `develop`
- Apertura obligatoria de un **Pull Request (PR)** desde la rama de feature hacia **`develop`**.
- El PR debe detallar claramente la Historia de Usuario asociada y los criterios de aceptación resueltos.
- El pipeline de CI/CD debe pasar de manera exitosa (compilación, pruebas, checkstyle y quality gate).
- Se requiere al menos **una aprobación obligatoria** por parte de un compañero del equipo antes de realizar el *merge*.

#### Promoción de `develop` a `main`
- Al cierre de un sprint validado y con entrega funcional aprobada.
- PR desde `develop` hacia `main` acompañado de su respectivo tag semántico.
- Actualización obligatoria del archivo `CHANGELOG.md` con los cambios incorporados en la versión.

---

## Gates de calidad y validación

### Gate 1: Validación local previa al Push
Antes de subir código al repositorio remoto, debes ejecutar localmente el ciclo completo de Maven para asegurar que todo compila y pasa las reglas de calidad:

```bash
# Limpiar, compilar y ejecutar pruebas unitarias/integración con JaCoCo
mvn clean verify

# Validar el cumplimiento de las reglas de estilo de código con Checkstyle
mvn checkstyle:check
```
Si cualquiera de los dos comandos arroja errores o violaciones, el código no debe subirse.

### Gate 2: Pipeline de CI/CD en GitHub Actions (`ci.yml`)
Al abrir un Pull Request hacia `develop`, GitHub Actions ejecuta de forma automatizada:
1. Compilación del proyecto backend sobre **Java 21**.
2. Ejecución de toda la suite de pruebas unitarias y de integración.
3. Verificación estática de estilo mediante **Checkstyle** (exigiendo cero violaciones).
4. Auditoría del Quality Gate y cobertura de código con **JaCoCo** (superando el umbral establecido).

Si el pipeline falla, el PR queda bloqueado y no podrá ser fusionado.

---

## Convención de commits

Usa un formato estructurado para los mensajes de tus commits:

```text
<tipo>(<alcance opcional>): <descripción corta>

[cuerpo opcional con detalle técnico]

[footer opcional con referencias]
```

### Tipos permitidos

| Tipo | Uso principal |
|---|---|
| `feat` | Nueva funcionalidad (corresponde a una Historia de Usuario) |
| `fix` | Corrección de un error o bug |
| `docs` | Cambios exclusivos en documentación (`README`, `DAS`, etc.) |
| `refactor` | Refactorización de código sin alterar la funcionalidad externa |
| `test` | Adición, ajuste o corrección de pruebas unitarias/integración |
| `chore` | Tareas de mantenimiento, configuración de dependencias o plugins |
| `style` | Cambios de formato, espacios o saltos de línea (sin afectar lógica) |
| `ci` | Modificaciones en los workflows de GitHub Actions |

---

## Definición de Hecho (DoD)

Una Historia de Usuario se considera completamente **Done** cuando cumple con lo siguiente:
- [ ] Código implementado bajo la arquitectura multicapa establecida (Controller, Service, Integration, Repository).
- [ ] Pruebas unitarias e de integración añadidas y pasando exitosamente.
- [ ] Cobertura de JaCoCo validada localmente y en el pipeline.
- [ ] Cero violaciones reportadas por Checkstyle.
- [ ] Pull Request abierto, revisado, aprobado por pares y fusionado en `develop`.
- [ ] Documentación de contratos o endpoints (Swagger/OpenAPI) actualizada correctamente.