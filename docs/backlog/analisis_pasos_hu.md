# Análisis de pasos por Historia de Usuario (HU-01 a HU-10)

Fuente: backlog refinado (páginas 1 y 2 del PDF). No se asume nada que no esté en el backlog; cuando algo no está definido se indica como **"No definido en el backlog"**.

## 1. Catálogo de tareas generales usadas

**Frontend**
- F1. Construir la pantalla (formulario, modal, tabla, badges, toasts, banners, Empty State)
- F2. Implementar validaciones en la interfaz
- F3. Integrar con el backend (consumir el endpoint y mostrar mensajes/traceId)

**Backend**
- B1. Clases de tipo modelo
- B2. Crear entidad en la BD
- B3. Construir clase de acceso a datos
- B4. Construir clase de lógica
- B5. Crear validaciones
- B6. Crear restricciones (reglas de estado, unicidad, integridad)

**Pruebas**
- P1. Probar (un caso por cada criterio de aceptación)

## 2. Nota sobre "¿Qué hacemos de calidad?"

El backlog **no define** una estrategia de calidad explícita (herramientas, cobertura, tipos de prueba). Lo que sí define son los criterios de aceptación, así que lo de calidad se deriva de ellos: cada criterio es un caso de prueba verificable (código HTTP + mensaje exacto + comportamiento en GUI + efecto en BD). Para las HU que modifican datos, además se verifica el **rollback** y la presencia del **traceId** en los errores 500/503.

---

## HU-01 — Registrar un material nuevo con sus propiedades

**Endpoint:** `POST /materiales` | **Pantalla:** Gestión de Materiales → Crear Material

### Pasos
**Backend**
1. Clases de tipo modelo: Material.
2. Crear entidad en la BD: Material (SKU, ID `mat-NNN`, Nombre, Categoría, Unidad de Medida, Densidad, Pureza, Estado).
3. Crear restricciones: SKU consecutivo autogenerado, Nombre único, Estado inicial ACTIVO.
4. Construir clase de acceso a datos (insertar, buscar por nombre, generar consecutivo).
5. Construir clase de lógica (asignar SKU e ID, guardar en ACTIVO, armar mensaje de éxito).
6. Crear validaciones (obligatorios, tipos, rangos, categoría válida).

**Frontend**
7. Construir la pantalla (formulario de creación).
8. Implementar validaciones en el formulario.
9. Integrar con el backend (POST, mensajes 201/400/409/500).

**Pruebas**
10. Probar los 5 criterios (carga, éxito, validación, duplicidad, falla del servicio).

### Respuestas
- **¿Cuántos campos?** 6: SKU, Nombre, Categoría, Unidad de Medida, Densidad, Pureza. (SKU es autogenerado y de solo lectura, así que el usuario diligencia 5.) El backend además asigna el ID único `mat-NNN` y el estado ACTIVO.
- **Validaciones:**
  - Campos obligatorios: Nombre, Categoría, Unidad de Medida, Densidad, Pureza → 400 con mensaje específico.
  - Categoría debe ser una de la lista → 400.
  - Nombre no puede existir previamente → 409.
  - Timeout > 5 s o fallo de BD → rollback y 500 con traceId.
- **Restricciones:**
  - SKU: alfanumérico, autogenerado, secuencial, solo lectura.
  - Nombre: alfabético.
  - Categoría: combo con Materia Prima, Componentes, Lubricantes.
  - Densidad: numérico positivo.
  - Pureza: 0–100 %.
  - Estado inicial ACTIVO; ID formato `mat-NNN`.
  - Tamaños máximos y lista de Unidades de Medida: No definido en el backlog.
- **Calidad:** 5 casos de prueba (uno por criterio). Verificar códigos 201/400/409/500, mensajes exactos, que en 400/409/500 no se persistan datos y que el 500 incluya traceId.

---

## HU-02 — Consultar las especificaciones de un material

**Endpoint:** `GET /materiales/{id}` y consulta con filtros | **Pantalla:** Gestión de Materiales → Ver Detalle

### Pasos
**Backend**
1. Clases de tipo modelo: respuesta de detalle de Material (reutiliza la entidad de HU-01).
2. Construir clase de acceso a datos (buscar por ID, listar con filtros).
3. Construir clase de lógica (consulta, armado de respuesta).
4. Crear validaciones (parámetros/filtros de consulta).

**Frontend**
5. Construir la pantalla (tabla con acción "Ver Detalle", barra de filtros, modal de ficha técnica con badge de estado, Empty State, toast y banner de error).
6. Implementar validaciones en los filtros.
7. Integrar con el backend (200/400/404/500).

**Pruebas**
8. Probar los 4 criterios.

### Respuestas
- **¿Cuántos campos?** 9 en la ficha (solo lectura): ID, SKU, Nombre, Categoría, Unidad de Medida, Densidad, Pureza, Fecha de Creación, Estado (badge ACTIVO/INACTIVO). Filtros: la barra de búsqueda admite filtros (ej. `densidad`), pero la lista completa de filtros No está definida en el backlog.
- **Validaciones:**
  - Tipo/valor del filtro (ej. `densidad=abc`) → 400.
  - ID inexistente → 404.
  - Fallo o timeout de BD → 500 con traceId.
- **Restricciones:**
  - Ficha de solo lectura.
  - Estado solo ACTIVO/INACTIVO.
  - Un material INACTIVO sigue siendo consultable (observación de HU-04).
- **Calidad:** 4 casos de prueba. Verificar que la ficha muestre los 9 campos, el toast con el nombre del parámetro, el Empty State con botón "Volver al Catálogo" y el banner con traceId.

---

## HU-03 — Modificar los datos de un material

**Endpoint:** `PUT /materiales/{id}` | **Pantalla:** Gestión de Materiales → Editar (modal)

### Pasos
**Backend**
1. Clases de tipo modelo: Material (solicitud de actualización).
2. Crear entidad en la BD: agregar `fechaModificacion` a Material.
3. Construir clase de acceso a datos (buscar por ID, actualizar).
4. Construir clase de lógica (verificar estado, actualizar, asignar fechaModificacion).
5. Crear validaciones (rangos, SKU inmutable).
6. Crear restricciones (no editar INACTIVOS).

**Frontend**
7. Construir la pantalla (modal de edición con SKU deshabilitado; edición deshabilitada si está INACTIVO).
8. Implementar validaciones en el modal.
9. Integrar con el backend (200/400/404/409/500).

**Pruebas**
10. Probar los 5 criterios.

### Respuestas
- **¿Cuántos campos?** Los mismos 6 del material, con SKU deshabilitado (5 editables). El backlog no lista explícitamente los campos del modal; solo menciona SKU y Pureza y dice "datos válidos". Se asume que son los de HU-01, **a confirmar**. El backend asigna `fechaModificacion`.
- **Validaciones:**
  - Valores fuera de rango (ej. Pureza > 100 %) → 400.
  - Intento de modificar SKU → 400.
  - ID inexistente → 404.
  - Material INACTIVO → 409.
  - Fallo de BD → rollback y 500 con traceId.
- **Restricciones:**
  - SKU inmutable.
  - Solo se edita un material en estado ACTIVO.
  - Pureza 0–100 % (rango citado en el criterio 2).
  - El resto de reglas de campo: No definido en el backlog para esta HU.
- **Calidad:** 5 casos de prueba. Verificar que ante 400/404/409/500 no cambie ningún dato (rollback) y que se actualice `fechaModificacion` en el caso exitoso.

---

## HU-04 — Desactivar un material (borrado lógico)

**Endpoint:** `DELETE /materiales/{id}` | **Pantalla:** Gestión de Materiales → Inhabilitar

### Pasos
**Backend**
1. Clases de tipo modelo: ajustar el estado de Material (ACTIVO/INACTIVO).
2. Construir clase de acceso a datos (buscar material, consultar fórmulas que lo usan, cambiar estado).
3. Construir clase de lógica (cambio de estado a INACTIVO, sin eliminar el registro).
4. Crear validaciones (estructura de la petición).
5. Crear restricciones (no inhabilitar si está en fórmulas vigentes, ni si ya está INACTIVO).

**Frontend**
6. Construir la pantalla (botón "Inhabilitar", modal de confirmación, toast, modal de conflicto).
7. Implementar validaciones (acción deshabilitada si ya está INACTIVO).
8. Integrar con el backend (200/400/404/409/500).

**Pruebas**
9. Probar los 6 criterios.

### Respuestas
- **¿Cuántos campos?** Ninguno de entrada. Solo el identificador `{id}` en la ruta y la confirmación en el modal.
- **Validaciones:**
  - Petición sin parámetros adicionales ni estructura inválida → 400.
  - ID inexistente → 404.
  - Material en alguna fórmula BORRADOR, EVALUANDO_PATENTE o APROBADA → 409.
  - Material ya INACTIVO → 409.
  - Fallo de BD → rollback y 500 con traceId.
- **Restricciones:**
  - Borrado lógico: el registro no se elimina.
  - Un material INACTIVO sigue siendo consultable por GET, pero no puede usarse en nuevas fórmulas.
  - Solo se inhabilita un material ACTIVO que no esté en fórmulas vigentes.
- **Calidad:** 6 casos de prueba. Verificar que el registro permanezca en BD con estado INACTIVO, que siga consultable por GET y que en el 500 se conserve el estado previo.

---

## HU-05 — Registrar una nueva fórmula

**Endpoint:** `POST /formulas` | **Pantalla:** Gestión de Fórmulas → Nueva Fórmula

### Pasos
**Backend**
1. Clases de tipo modelo: Fórmula y Componente de fórmula (material + porcentaje).
2. Crear entidad en la BD: Fórmula (código `form-NNN`, Nombre, Aplicación Técnica, Versión, Estado) y relación con sus Componentes.
3. Crear restricciones (nombre único, código secuencial, versión 1, estado BORRADOR).
4. Construir clase de acceso a datos (insertar fórmula + componentes en una transacción, buscar por nombre, verificar materiales).
5. Construir clase de lógica (generar código, validar suma, asignar estado y versión iniciales).
6. Crear validaciones (obligatorios, suma = 100 %, materiales activos).

**Frontend**
7. Construir la pantalla (formulario con Nombre, Aplicación Técnica y tabla/selector de componentes con porcentaje; toast y banner).
8. Implementar validaciones (resaltar campos omitidos, suma de porcentajes).
9. Integrar con el backend (201/400/409/422/500).

**Pruebas**
10. Probar los 6 criterios.

### Respuestas
- **¿Cuántos campos?** 3 de entrada: Nombre, Aplicación Técnica y Componentes (lista de materiales con su porcentaje). El backend genera código `form-NNN`, versión 1 y estado BORRADOR.
- **Validaciones:**
  - Nombre, Aplicación Técnica y Componentes obligatorios → 400 (se resaltan los campos omitidos).
  - Suma de porcentajes exactamente 100.00 % → 400.
  - Nombre de fórmula único → 409.
  - Componentes existentes y ACTIVOS → 422.
  - Fallo de BD → rollback completo y 500 con traceId.
- **Restricciones:**
  - Solo materiales ACTIVOS como componentes.
  - Estado inicial BORRADOR, versión 1, código secuencial `form-NNN`.
  - Tipo/tamaño de Nombre y Aplicación Técnica, rango por componente y mínimo de componentes: No definido en el backlog.
- **Calidad:** 6 casos de prueba. Verificar suma = 100.00 %, mensajes con la lista de campos/materiales, rollback completo (fórmula y componentes) y traceId en el 500.

---

## HU-06 — Consultar una fórmula

**Endpoint:** `GET /formulas/{id}` | **Pantalla:** Ver Detalle de fórmula | **Rol:** Ingeniero de laboratorio / Encargado de fabricación

### Pasos
**Backend**
1. Clases de tipo modelo: respuesta de detalle de Fórmula con Componentes y `patentesEnConflicto`.
2. Construir clase de acceso a datos (buscar fórmula con sus componentes).
3. Construir clase de lógica (armar detalle; incluir `patentesEnConflicto` si está RECHAZADA_POR_INFRACCION).
4. Crear validaciones (query params/filtros).

**Frontend**
5. Construir la pantalla (modal de ficha técnica, badge de estado, tabla embebida de componentes, sección roja de patentes en conflicto, Empty State, toast y modal de error).
6. Implementar validaciones en los filtros.
7. Integrar con el backend (200/400/404/500).

**Pruebas**
8. Probar los 5 criterios.

### Respuestas
- **¿Cuántos campos?** 6 en la ficha (solo lectura): ID, Nombre, Aplicación Técnica, Versión, Estado (badge) y tabla de Componentes. Más 1 condicional: `patentesEnConflicto` (solo si el estado es RECHAZADA_POR_INFRACCION).
- **Validaciones:**
  - Parámetros/filtros válidos (ej. `version=abc`, `estado=INVALIDO`) → 400.
  - ID inexistente → 404.
  - Error de lectura de BD al resolver componentes → 500 con traceId.
- **Restricciones:**
  - Solo lectura.
  - Si el estado es RECHAZADA_POR_INFRACCION, se muestra la lista de patentes en conflicto resaltada en rojo.
  - Detalle de campos de cada componente en la tabla: No definido en el backlog.
- **Calidad:** 5 casos de prueba. Verificar la sección roja de patentes, el Empty State con botón "Volver a Gestión de Fórmulas" y el traceId en el modal de error.
- **Observación del backlog:** el criterio 1 lista los badges BORRADOR, EVALUANDO_PATENTE y APROBADA, pero otras HU usan también RECHAZADA_POR_INFRACCION y DESCONTINUADA. Conviene confirmar la lista completa de estados.

---

## HU-07 — Modificar los materiales o proporciones de una fórmula

**Endpoint:** `PUT /formulas/{id}` | **Pantalla:** Gestión de Fórmulas → Editar

### Pasos
**Backend**
1. Clases de tipo modelo: Fórmula y Componentes (solicitud de actualización).
2. Crear entidad en la BD: soporte para la versión de la fórmula (incremento +1 en cada edición).
3. Construir clase de acceso a datos (buscar fórmula, actualizar fórmula y componentes en transacción).
4. Construir clase de lógica (incrementar versión, dejar estado en BORRADOR).
5. Crear validaciones (suma = 100 %, obligatorios, materiales activos).
6. Crear restricciones (solo editable en BORRADOR o RECHAZADA_POR_INFRACCION).

**Frontend**
7. Construir la pantalla (modal/pantalla de edición; opción deshabilitada en estados protegidos; toast).
8. Implementar validaciones (suma de porcentajes, obligatorios).
9. Integrar con el backend (200/400/404/409/422/500).

**Pruebas**
10. Probar los 6 criterios.

### Respuestas
- **¿Cuántos campos?** Componentes (materiales y porcentajes) como campo explícito del criterio. El backlog menciona "campos obligatorios" sin enumerarlos; se asume que son los de HU-05 (Nombre, Aplicación Técnica, Componentes), **a confirmar**. El backend incrementa la versión.
- **Validaciones:**
  - Suma de porcentajes = 100.00 % o faltan campos obligatorios → 400.
  - ID inexistente → 404.
  - Estado EVALUANDO_PATENTE, APROBADA o DESCONTINUADA → 409.
  - Material inactivo o inexistente → 422.
  - Fallo de BD → rollback completo y 500 con traceId.
- **Restricciones:**
  - Solo se edita en estado BORRADOR o RECHAZADA_POR_INFRACCION.
  - Al guardar, la versión sube +1 y el estado queda BORRADOR.
  - Solo componentes ACTIVOS.
- **Calidad:** 6 casos de prueba. Verificar el incremento de versión, que el estado pase a BORRADOR desde RECHAZADA_POR_INFRACCION y que ante errores no cambie nada (rollback).

---

## HU-08 — Descontinuar una fórmula

**Endpoint:** `DELETE /formulas/{id}` | **Pantalla:** Gestión de Fórmulas → Descontinuar

### Pasos
**Backend**
1. Clases de tipo modelo: estado DESCONTINUADA en Fórmula.
2. Construir clase de acceso a datos (buscar fórmula, cambiar estado).
3. Construir clase de lógica (cambio de estado a DESCONTINUADA, sin eliminar el registro).
4. Crear validaciones (estructura de la petición).
5. Crear restricciones (estados permitidos).

**Frontend**
6. Construir la pantalla (acción "Descontinuar", modal de confirmación, toast, modal de error).
7. Implementar validaciones (acción inhabilitada en estados protegidos).
8. Integrar con el backend (200/400/404/409/500).

**Pruebas**
9. Probar los 5 criterios.

### Respuestas
- **¿Cuántos campos?** Ninguno de entrada. Solo `{id}` en la ruta y la confirmación en el modal.
- **Validaciones:**
  - Cuerpo/parámetros no permitidos → 400.
  - ID inexistente → 404.
  - Estado EVALUANDO_PATENTE o ya DESCONTINUADA → 409.
  - Fallo de BD → rollback (mantiene estado anterior) y 500 con traceId.
- **Restricciones:**
  - Solo se descontinúa desde BORRADOR, APROBADA o RECHAZADA_POR_INFRACCION.
  - No se puede desde EVALUANDO_PATENTE ni desde DESCONTINUADA.
  - Es borrado lógico: el estado cambia a DESCONTINUADA.
- **Calidad:** 5 casos de prueba. Verificar cada estado permitido y cada estado bloqueado, y que el 500 conserve el estado anterior.

---

## HU-09 — Solicitar la verificación legal (evaluación de patente)

**Endpoint:** `POST /formulas/{id}/evaluar-patente` | **Pantalla:** Gestión de Fórmulas → Ver Detalle → "Solicitar Evaluación de Patente"

### Pasos
**Backend**
1. Clases de tipo modelo: solicitud/respuesta de evaluación y ticket.
2. Crear entidad en la BD: guardar `ticketEvaluacionId` en la Fórmula y estado EVALUANDO_PATENTE.
3. Construir clase de acceso a datos (buscar fórmula, materiales, actualizar estado y ticket).
4. Construir clase de lógica (verificar estado y materiales, invocar Patent Sweep, generar ticket, cambiar estado).
5. Construir la integración con el servicio externo Patent Sweep (timeout > 5 s, errores 5xx).
6. Crear validaciones y restricciones (estado BORRADOR, materiales ACTIVOS).

**Frontend**
7. Construir la pantalla (botón en el detalle y mensajes de éxito/error).
8. Implementar validaciones (botón habilitado solo en BORRADOR).
9. Integrar con el backend (202/404/409/422/503).

**Pruebas**
10. Probar los 5 criterios, con Patent Sweep simulado (éxito, timeout y error 5xx).

### Respuestas
- **¿Cuántos campos?** Ninguno de entrada: es una acción (botón) sobre `{id}`. Genera `ticketEvaluacionId`.
- **Validaciones:**
  - Fórmula en estado BORRADOR → si no, 409.
  - ID inexistente → 404.
  - Todos los materiales ACTIVOS → si no, 422 y no se llama a Patent Sweep.
  - Timeout > 5 s o error HTTP 5xx de Patent Sweep → rollback y 503 con traceId.
- **Restricciones:**
  - Solo desde BORRADOR y con materiales ACTIVOS.
  - Al enviarse, el estado pasa a EVALUANDO_PATENTE y la respuesta es 202 Accepted.
  - Ante falla externa, la fórmula se mantiene en BORRADOR.
- **Calidad:** 5 casos de prueba con Patent Sweep simulado (mock). Verificar que en 422/503 la fórmula siga en BORRADOR y que en 503 el mensaje incluya la causa del error y el traceId.

---

## HU-10 — Verificación legal de la composición (dictamen de patente)

**Endpoint:** `POST /formulas/{id}/dictamen-patente` (lo envía Patent Sweep)

### Pasos
**Backend**
1. Clases de tipo modelo: dictamen (resultado, ticketEvaluacionId, patentesEnConflicto).
2. Crear entidad en la BD: almacenar `patentesEnConflicto` asociado a la Fórmula y los estados APROBADA / RECHAZADA_POR_INFRACCION.
3. Construir clase de acceso a datos (buscar fórmula, actualizar estado y patentes).
4. Construir clase de lógica (aplicar el dictamen según el resultado).
5. Crear validaciones (coherencia del payload).
6. Crear restricciones (solo en EVALUANDO_PATENTE y con ticket coincidente).

**Frontend**
7. Reflejar el resultado en la ficha de HU-06 (badge de estado y sección de patentes). El backlog no describe una pantalla propia para esta HU.

**Pruebas**
8. Probar los 6 criterios, enviando dictámenes simulados de Patent Sweep.

### Respuestas
- **¿Cuántos campos?** 3 en el payload: `resultado` (SIN_INFRACCION / INFRACCION), `ticketEvaluacionId` y `patentesEnConflicto` (arreglo). Más el `{id}` de la fórmula en la ruta.
- **Validaciones:**
  - Datos incoherentes o faltantes (ej. INFRACCION con `patentesEnConflicto` vacío, o falta `ticketEvaluacionId`) → 400.
  - ID inexistente → 404.
  - Fórmula que no está en EVALUANDO_PATENTE, o ticket que no coincide → 409.
  - Fallo de BD → rollback y 500 con traceId.
- **Restricciones:**
  - Solo se acepta si la fórmula está en EVALUANDO_PATENTE y el ticket coincide con el trámite activo.
  - SIN_INFRACCION → APROBADA; INFRACCION → RECHAZADA_POR_INFRACCION, guardando la lista de patentes (mínimo 1 código).
  - Formato de los códigos de patente: No definido en el backlog.
- **Calidad:** 6 casos de prueba con dictámenes simulados. Verificar ambos resultados, que no cambie el estado en 400/409/500 y que las patentes guardadas se muestren luego en HU-06.
- **Observación del backlog:** el enunciado habla de "consultar la verificación legal", pero los criterios describen la **recepción** del dictamen enviado por Patent Sweep. Conviene aclarar si falta una consulta desde la GUI o si el enunciado debe reescribirse.

---

## 3. Resumen comparativo

| HU | Acción | Campos | Códigos de error | Pantalla propia |
|----|--------|--------|------------------|-----------------|
| HU-01 | Registrar material | 6 (5 diligenciables) | 400, 409, 500 | Sí |
| HU-02 | Consultar material | 9 de ficha + filtros | 400, 404, 500 | Sí |
| HU-03 | Modificar material | 6 (SKU bloqueado) | 400, 404, 409, 500 | Sí |
| HU-04 | Inhabilitar material | 0 (solo ID) | 400, 404, 409, 500 | Sí (modal) |
| HU-05 | Registrar fórmula | 3 | 400, 409, 422, 500 | Sí |
| HU-06 | Consultar fórmula | 6 + 1 condicional | 400, 404, 500 | Sí |
| HU-07 | Modificar fórmula | Componentes (+ obligatorios por confirmar) | 400, 404, 409, 422, 500 | Sí |
| HU-08 | Descontinuar fórmula | 0 (solo ID) | 400, 404, 409, 500 | Sí (modal) |
| HU-09 | Solicitar evaluación de patente | 0 (acción) | 404, 409, 422, 503 | Botón en detalle |
| HU-10 | Recibir dictamen de patente | 3 (payload) | 400, 404, 409, 500 | No descrita |

## 4. Puntos a confirmar con el equipo

1. HU-03 y HU-07: el backlog no enumera los campos del formulario de edición.
2. HU-06: lista completa de estados mostrados en el badge.
3. HU-10: enunciado ("consultar") vs. criterios (recibir dictamen).
4. HU-01 y HU-05: tamaños máximos de campos y lista de Unidades de Medida.
5. HU-02: lista de filtros disponibles en la barra de búsqueda.
