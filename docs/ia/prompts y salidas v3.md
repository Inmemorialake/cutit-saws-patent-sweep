# Prompts y salidas — Product Backlog Equipo 1 (Materiales/Fórmulas) Corregidas
**Curso:** 750021C Desarrollo de software II — Caso Cutit Saws Ltd.

Este documento reúne el prompt generador, el prompt validador y las salidas producidas por ambos, luego de revisar una segunda vez lo planteado.

---

## 1. Prompt generador

```xml
<system_prompt>

Actúas como un Analista de Requisitos e Ingeniero de Software Senior, especializado en arquitecturas orientadas a servicios (SOA) y diseño de APIs RESTful. Tu marco normativo abarca ISO/IEC/IEEE 29148:2018, el modelo de Mike Cohn, los criterios INVEST y la metodología BDD. Eres técnicamente riguroso, preciso en contratos de API REST y mantienes la atomicidad del backlog.

</system_prompt>

<problem_statement>

Cutit Saws Ltd. fabrica motosierras hidráulicas de diamante. Para resolver cuellos de botella e incompatibilidad en su hub homegrown, la dirección definió construir una plataforma backend modular de 5 APIs REST independientes (Materiales/Fórmulas, Patent Sweep, Fabricación, Inventario, Notificaciones). Alcance exclusivo: Backend e integración SOA (sin UI, sin pagos, sin autenticación compleja).

Dominio de la API (Equipo 1 - Materiales/Fórmulas):
* Responsabilidad: Gestión centralizada del catálogo de materiales/insumos técnicos, estructuración de recetas/fórmulas de fabricación y solicitud de verificación de propiedad intelectual.
* Endpoints mínimos: POST /materiales, GET /materiales/{id}, POST /formulas, GET /formulas/{id}, POST /formulas/{id}/evaluar-patente.
* Interconexión SOA:
  - CONSUME DE: API de Patent Sweep (Equipo 2) vía cliente HTTP para validación de riesgo de infracción.
  - ES CONSUMIDA POR: API de Fabricación (Equipo 3) para consultar las fórmulas aprobadas y recetas de cada producto.
  - ES CONSUMIDA POR: API de Patent Sweep (Equipo 2) para análisis periódicos de componentes.

Roles del Sistema:
* Ingeniero de Laboratorio (Usuario humano que gestiona el catálogo de insumos y diseña las fórmulas de recubrimiento).
* Sistema de Fabricación (API Externa que consume recetas aprobadas para producción).
* Sistema Patent Sweep (API Externa que notifica o responde sobre el dictamen legal de la fórmula).

</problem_statement>

<input_epics>

El analista humano ya realizó el proceso de abstracción y levantó 9 Historias de Usuario Épicas (en formato Mike Cohn sin Criterios de Aceptación). Tu tarea NO es descomponer ni crear nuevas Historias de Usuario, sino enrichment de estas 9 Épicas generándoles sus Criterios de Aceptación en BDD.

Lista de Épicas de Entrada:
1. HU-01 (Crear materia): Yo como Ingeniero de Laboratorio, requiero registrar un material nuevo con sus propiedades, para poder incluirlo como componente en el diseño de fórmulas.
2. HU-02 (Consultar material): Yo como Ingeniero de laboratorio requiero consultar las especificaciones de un material registrado, para verificar sus características físicas y químicas antes de usarlo en una fórmula.
3. HU-03 (Actualizar material): Yo como Ingeniero de Laboratorio, requiero modificar los datos de un material registrado, para mantener actualizada la información de los materiales.
4. HU-04 (Eliminar/Inhabilitar material): Yo como Ingeniero de Laboratorio, requiero desactivar un material, para evitar que sea utilizado en futuras formulaciones por estar descontinuado.
5. HU-05 (Crear Fórmula): Yo como Ingeniero de Laboratorio, requiero registrar una nueva fórmula para que sea usada en la fabricación de las cuchillas.
6. HU-06 (Consultar Fórmula): Yo como ingeniero de laboratorio, requiero consultar una fórmula para conocer su composición y en qué punto de la validación se encuentra.
7. HU-07 (Actualizar Fórmula): Yo como ingeniero de laboratorio, requiero modificar los materiales o proporciones de una fórmula existente, para corregirla o mejorarla antes de su uso en producción.
8. HU-08 (Eliminar Fórmula): Yo como ingeniero de laboratorio, requiero descontinuar una fórmula, para evitar que el departamento de Fabricación produzca un producto con una fórmula que ya no está vigente.
9. HU-09 (Evaluar Patente): Yo como Ingeniero de Laboratorio, requiero solicitar la verificación legal de la composición de una fórmula, para garantizar que la mezcla no infrinja propiedad intelectual antes de ser aprobada para fabricación.

</input_epics>

<generation_rules>

Para CADA UNA de las 9 Épicas proporcionadas:
1. Mantén intacta la redacción del enunciado Mike Cohn proporcionado.
2. Genera un MÍNIMO DE 4 Criterios de Aceptación (CA) por Historia de Usuario en formato BDD (Dado / Cuando / Entonces). Puedes generar más CAs si la complejidad lógica del endpoint o la integración SOA lo justifica estrictamente.
3. Cobertura Estricta Obligatoria (Mínimo 4 CAs por HU):
   - CA-01 (Éxito): Flujo principal o camino feliz con código HTTP 200 OK o 201 Created.
   - CA-02 (Validación Negocio): Error por parámetros inválidos, campos faltantes o tipos de datos incorrectos con código HTTP 400 Bad Request.
   - CA-03 (Conflicto / Recurso): Error por recurso inexistente o conflicto de estado/duplicidad con código HTTP 404 Not Found o 409 Conflict.
   - CA-04 (Excepción SOA / Servidor): Error por falla de servidor, timeout o indisponibilidad en llamadas inter-API con código HTTP 500 Internal Server Error o 503 Service Unavailable.
   - CAs adicionales (Opcionales): Escenarios alternativos de negocio o estados intermedios si el endpoint lo amerita.
4. No dupliques ni alteres el ID ni el nombre de las HUs.

</generation_rules>

<limits_and_constraints>

* No inventes endpoints para Inventario, Notificaciones o Fabricación que no hayan sido mencionados como puntos de integración.
* Declara de forma transparente cualquier "Supuesto asumido" sobre campos JSON o estados del sistema en una sección al final de cada HU.

</limits_and_constraints>

<output_format>

Presenta el resultado estructurado de la siguiente forma para cada una de las 9 HUs:

### [ID de la HU] - [Nombre Corto]
* **Enunciado Épico:** "Yo como [Rol], requiero [Acción], para [Motivo]."
* **Endpoint HTTP Asociado:** [Método VERBO /ruta]
* **Criterios de Aceptación (BDD):**
  - **CA-01 (Éxito):** **Dado** [contexto inicial], **Cuando** [acción ejecutada], **Entonces** [resultado esperado con código HTTP 200/201].
  - **CA-02 (Validación Negocio):** **Dado** [contexto], **Cuando** [acción con datos o formatos inválidos], **Entonces** [resultado con mensaje de error y código HTTP 400].
  - **CA-03 (Conflicto / Recurso):** **Dado** [contexto], **Cuando** [intento de acceso a recurso inexistente o duplicado], **Entonces** [resultado con código HTTP 404 o 409].
  - **CA-04 (Excepción / SOA):** **Dado** [contexto de integración o del sistema], **Cuando** [ocurre un timeout o falla interna], **Entonces** [resultado con código HTTP 500 o 503].
* **Supuestos Asumidos:** [Breve lista de supuestos sobre el contrato JSON o estados]

</output_format>
```

---

## 2. Prompt validador

```xml
<system_prompt>

Actúas como un Auditor de Requisitos Senior e Ingeniero de Calidad de Software (QA), completamente independiente de la célula de desarrollo que generó el backlog. Tu función es auditar con imparcialidad técnica cada Historia de Usuario (HU) y Criterio de Aceptación (CA) bajo las normas ISO/IEC/IEEE 29148:2018, los criterios INVEST y la metodología BDD/SMART. Evalúas el documento suministrado como un artefacto externo de ingeniería, sin autocomplacencia y exigiendo máxima rigurosidad técnica.

</system_prompt>

<audit_framework>

1. Evaluación de Historias de Usuario (HU):

   - Criterios INVEST (Independent, Negotiable, Valuable, Estimable, Small, Testable).
   - Regla de Estimación: La dimensión "Estimable" DEBE estar explícitamente marcada como "Pendiente de validación por el equipo de desarrollo". Si la IA o el autor se autoadjudicó un puntaje de estimación (ej. Puntos de Historia), se considera una falla grave.
   - Trazabilidad y Estructura: Verificación del formato estándar Mike Cohn ("Yo como [Rol], requiero [Acción], para [Motivo]") y concordancia con el caso Cutit Saws.

2. Evaluación de Criterios de Aceptación (CA):

   - Criterios SMART (Specific, Measurable, Achievable, Relevant, Time-boxed) en formato BDD (Dado / Cuando / Entonces).
   - Regla de Factibilidad: La dimensión "Achievable" DEBE estar explícitamente marcada como "Pendiente de validación por el equipo de desarrollo".
   - Cuestionario Obligatorio de 5 Preguntas por cada CA:

     1. ¿Indica explícitamente el endpoint HTTP y método/verbo de la acción?
     2. ¿Describe los campos, payload JSON o parámetros de entrada relevantes?
     3. ¿Describe las validaciones, código de estado HTTP y mensajes de error estructurados?
     4. ¿Describe las restricciones técnicas, reglas de negocio o estados de inmutabilidad que aplican?
     5. ¿Representa una única condición atómica, independiente y verificable?

3. Cobertura Estricta de Matrices HTTP y Errores SOA:

   - Verificación de la presencia obligatoria de la matriz mínima de 4 escenarios por HU:

     * Flujo de Éxito / Camino Feliz (HTTP 200 OK / 201 Created).
     * Error de Validación de Negocio (HTTP 400 Bad Request).
     * Recurso No Encontrado o Conflicto de Estado (HTTP 404 Not Found / 409 Conflict / 422 Unprocessable Entity).
     * Excepción Técnica o Falla de Integración SOA (HTTP 500 Internal Server Error / 503 Service Unavailable / 504 Gateway Timeout).
     
   - Verificación de la diferenciación explícita entre fallos de lógica de negocio y fallos de infraestructura/comunicación inter-servicio (SOA).
   
</audit_framework>

<problem_statement>

Contexto de la Auditoría (Cutit Saws Ltd. - Equipo 1: API Materiales y Fórmulas):

* Alcance del Módulo: Backend e integración SOA para la gestión centralizada del catálogo de materiales/insumos técnicos, estructuración y versionado de recetas/fórmulas de fabricación, y solicitud de verificación legal de propiedad intelectual.

* Endpoints Mínimos del Servicio:
  - POST /materiales
  - GET /materiales/{id}
  - PUT /materiales/{id}
  - DELETE /materiales/{id}
  - POST /formulas
  - GET /formulas/{id}
  - PUT /formulas/{id}
  - DELETE /formulas/{id}
  - POST /formulas/{id}/evaluar-patente
  
* Puntos de Integración SOA:

  - CONSUME DE: API de Patent Sweep (Equipo 2) para análisis de infracción legal.
  - ES CONSUMIDA POR: API de Fabricación (Equipo 3) para consulta de fórmulas aprobadas y recetas.
  - ES CONSUMIDA POR: API de Patent Sweep (Equipo 2) para barridos periódicos.
  
</problem_statement>

<evaluation_thresholds>

Aplica de forma estricta los siguientes umbrales para determinar el veredicto de cada HU:

APROBADA:

- Cumple el 100% de INVEST (Estimable marcado como "Pendiente").
- Todos sus CAs cumplen SMART (Achievable marcado como "Pendiente") y responden afirmativamente a las 5 preguntas del cuestionario.
- Cubre los 4 escenarios HTTP obligatorios (Éxito, Validación 400, Conflicto/Inexistencia 404/409 y Excepción SOA/Servidor 500/503).

APROBADA CON OBSERVACIONES:

- Presenta máximo 1 falla menor en INVEST (excluyendo "Testable" e "Independent", las cuales son críticas).
- O presenta hasta 2 CAs con 1 falla menor en SMART o en el cuestionario de 5 preguntas.

RECHAZADA:

- Falla en las dimensiones críticas "Testable" o "Independent".
- Carece de alguno de los 4 escenarios HTTP obligatorios (por ejemplo, omitió el manejo de errores SOA 500/503 o la validación de negocio 400).
- El autor/documento se autoadjudicó un valor numérico/veredicto en "Estimable" o "Achievable".
- 2 o más CAs fallan en "Specific" o "Measurable".

</evaluation_thresholds>

<limits_and_constraints>

* Prohibido emitir opiniones generales o abstractas (ej. "El texto se ve claro"); toda observación debe estar rigurosamente vinculada a un criterio INVEST, SMART o del cuestionario atómico.
* No alterar ni relajar los umbrales de veredicto por autocomplacencia.

</limits_and_constraints>

<output_format>

Presenta el informe estructurado de la siguiente manera:

### Reporte de Auditoría: [ID de la HU] - [Nombre Corto]

1. **Matriz INVEST de la Historia de Usuario:**

| Criterio | Estado (Cumple / Falla / Pendiente) | Justificación Técnica |
| :--- | :--- | :--- |
| Independent | | |
| Negotiable | | |
| Valuable | | |
| Estimable | | |
| Small | | |
| Testable | | |

2. **Evaluación de Criterios de Aceptación (BDD):**

(Para cada CA de la HU):

* **[ID del CA]:**
  - **Tabla SMART:**
  | Dimensión | Estado | Justificación |
  | :--- | :--- | :--- |
  | Specific | | |
  | Measurable | | |
  | Achievable | | |
  | Relevant | | |
  | Time-boxed | | |

  - **Cuestionario Atómico (5 Preguntas):**

  1. ¿Endpoint/Método?: [Sí/No + detalle]
  2. ¿Campos/Payload?: [Sí/No + detalle]
  3. ¿Código HTTP/Error?: [Sí/No + detalle]
  4. ¿Restricciones/Reglas?: [Sí/No + detalle]
  5. ¿Condición Atómica?: [Sí/No + detalle]

3. **Veredicto Final de la HU:** **[APROBADA / APROBADA CON OBSERVACIONES / RECHAZADA]**

* **Justificación del Veredicto:** [Explicación detallada según los umbrales].
* **Acción Requerida / Sugerencia:** [Ajuste puntual si aplica].

---

### Tabla Resumen Final del Backlog

| ID HU | Nombre Épico | Total CAs | Veredicto Final | Principal Hallazgo / Observación |
| :--- | :--- | :--- | :--- | :--- |

</output_format>
```

---

## 3. Salida del generador — Product Backlog

## HU-01: Crear materia

**Épico:** Yo como Ingeniero de Laboratorio, requiero registrar un material nuevo con sus propiedades, para poder incluirlo como componente en el diseño de fórmulas.

**Endpoint:** `POST /materiales`

### Criterios de Aceptación

```gherkin
Escenario: Éxito
  Dado que el Ingeniero de Laboratorio cuenta con los datos técnicos válidos de un nuevo insumo (nombre, código CAS, unidad de medida, densidad y propiedades físico-químicas)
  Cuando envía una solicitud POST /materiales con la estructura JSON requerida
  Entonces el sistema registra el material en el catálogo, asigna un ID único y retorna un código HTTP 201 Created con el recurso creado en el cuerpo del mensaje.

Escenario: Validación Negocio
  Dado que el Ingeniero de Laboratorio intenta registrar un material omitiendo campos obligatorios (como el código CAS o la unidad de medida) o enviando tipos de datos inconsistentes (ej. densidad negativa)
  Cuando ejecuta la solicitud POST /materiales
  Entonces el sistema rechaza el registro, no persiste la información y retorna un código HTTP 400 Bad Request con el detalle estructurado de las validaciones fallidas.

Escenario: Conflicto / Recurso
  Dado que un material ya se encuentra previamente registrado en la base de datos con el mismo código CAS o nombre comercial
  Cuando el Ingeniero de Laboratorio envía una solicitud POST /materiales con dicho código o nombre
  Entonces el sistema detecta la duplicidad y responde con un código HTTP 409 Conflict notificando la existencia del recurso.

Escenario: Excepción / SOA
  Dado que la base de datos de la API de Materiales experimenta un fallo de conexión o un timeout durante la transacción de inserción
  Cuando el cliente procesa la solicitud POST /materiales
  Entonces el sistema captura la excepción no controlada y retorna un código HTTP 500 Internal Server Error con un identificador de traza para auditoría.

```
### Supuestos

- El payload JSON incluye los campos obligatorios: codigoCAS (string único), nombre (string), unidadMedida (enum), densidad (float) y propiedades (object).
- El estado inicial de un material recién creado es ACTIVO.

## HU-02: Consultar material

**Épico:** Yo como Ingeniero de laboratorio requiero consultar las especificaciones de un material registrado, para verificar sus características físicas y químicas antes de usarlo en una fórmula.

**Endpoint:** `GET /materiales/{id}`

### Criterios de Aceptación

```gherkin
Escenario: Éxito
  Dado que existe un material registrado con el identificador mat-101 en el catálogo
  Cuando el usuario ejecuta una solicitud GET /materiales/mat-101
  Entonces el sistema responde con un código HTTP 200 OK devolviendo el objeto JSON completo con las especificaciones físicas, químicas y el estado actual del material.

Escenario: Validación Negocio
  Dado que se solicita la consulta de un material especificando un identificador con un formato inválido (por ejemplo, caracteres especiales no permitidos en el UUID/ID)
  Cuando el cliente realiza la petición GET /materiales/ID_INVALIDO!
  Entonces el sistema valida el parámetro de ruta y retorna un código HTTP 400 Bad Request.

Escenario: Conflicto / Recurso
  Dado que no existe ningún material registrado asociado al identificador enviado en la ruta
  Cuando el Ingeniero de Laboratorio realiza la petición GET /materiales/mat-999
  Entonces el sistema retorna un código HTTP 404 Not Found indicando que el recurso solicitado no fue encontrado.

Escenario: Excepción / SOA
  Cuando se ejecuta la solicitud GET /materiales/{id}
  Entonces el sistema retorna un código HTTP 500 Internal Server Error o 503 Service Unavailable informando la indisponibilidad momentánea del servicio.

```
### Supuestos

- Los materiales en estado INACTIVO o DESCONTINUADO siguen siendo consultables por ID para mantener la trazabilidad histórica de fórmulas pasadas.

## HU-03: Actualizar material

**Épico:** Yo como Ingeniero de Laboratorio, requiero modificar los datos de un material registrado, para mantener actualizada la información de los materiales.

**Endpoint:** `PUT /materiales/{id}`

### Criterios de Aceptación

```gherkin
Escenario: Éxito
  Cuando el Ingeniero de Laboratorio envía una solicitud PUT /materiales/mat-101 con los datos modificados (ej. actualización de ficha de seguridad o propiedades físicas)
  Entonces el sistema actualiza la entidad, registra la fecha de modificación y retorna un código HTTP 200 OK con la información actualizada.

Escenario: Validación Negocio
  Cuando la petición PUT /materiales/mat-101 incluye valores fuera de rango técnico (ej. porcentaje de pureza mayor a 100% o campos requeridos vacíos)
  Entonces el sistema no aplica la modificación y retorna un código HTTP 400 Bad Request especificando el error.

Escenario: Conflicto / Recurso
  Dado que se intenta actualizar un material cuyo identificador mat-888 no existe en el sistema
  Cuando se ejecuta la petición PUT /materiales/mat-888
  Entonces el sistema responde con un código HTTP 404 Not Found.

Escenario: Excepción / SOA
  Cuando se procesa la actualización
  Entonces el sistema realiza un rollback de la transacción y retorna un código HTTP 500 Internal Server Error.

```
### Supuestos

- El método PUT requiere el cuerpo completo del objeto de material.
- El código CAS no es modificable una vez creado para preservar la integridad referencial.

## HU-04: Eliminar/Inhabilitar material

**Épico:** Yo como Ingeniero de Laboratorio, requiero desactivar un material, para evitar que sea utilizado en futuras formulaciones por estar descontinuado.

**Endpoint:** `DELETE /materiales/{id}`

### Criterios de Aceptación

```gherkin
Escenario: Éxito
  Dado que el material mat-101 está en estado ACTIVO y no está vinculado a fórmulas en estado de borradores activos
  Cuando el Ingeniero de Laboratorio envía una petición DELETE /materiales/mat-101
  Entonces el sistema efectúa un borrado lógico cambiando el estado del material a INACTIVO y responde con un código HTTP 200 OK (o 204 No Content).

Escenario: Validación Negocio
  Dado que se envía una solicitud para desactivar un material mat-101 sin cumplir con las previas reglas de confirmación requeridas por el header o cuerpo de la solicitud
  Cuando el cliente realiza el llamado DELETE /materiales/mat-101
  Entonces el sistema invalida la solicitud y responde con un código HTTP 400 Bad Request.

Escenario: Conflicto / Recurso
  Dado que el material mat-101 se encuentra en uso activo dentro de una o más fórmulas en proceso de evaluación o aprobación
  Cuando el usuario intenta la desactivación vía DELETE /materiales/mat-101
  Entonces el sistema impide la acción para proteger la integridad del dominio y retorna un código HTTP 409 Conflict.

Escenario: Excepción / SOA
  Cuando el sistema procesa el llamado DELETE /materiales/{id}
  Entonces el sistema responde con un código HTTP 500 Internal Server Error.

```
### Supuestos

- No se realiza borrado físico en base de datos (HARD DELETE); se aplica una inhabilitación lógica (SOFT DELETE).

## HU-05: Crear Fórmula

**Épico:** Yo como Ingeniero de Laboratorio, requiero registrar una nueva fórmula para que sea usada en la fabricación de las cuchillas.

**Endpoint:** `POST /formulas`

### Criterios de Aceptación

```gherkin
Escenario: Éxito
  Cuando el Ingeniero de Laboratorio envía la solicitud POST /formulas con el nombre de la mezcla, aplicación técnica y componentes
  Entonces el sistema guarda la fórmula en estado BORRADOR y devuelve un código HTTP 201 Created con el ID asignado.

Escenario: Validación Negocio
  Cuando se envía la petición POST /formulas
  Entonces el sistema invalida el registro por inconsistencia aritmética y responde con un código HTTP 400 Bad Request.

Escenario: Conflicto / Recurso
  Dado que el identificador de fórmula especificado en el cuerpo ya existe o presenta duplicidad en el código técnico
  Cuando se envía POST /formulas
  Entonces el sistema rechaza el registro con un código HTTP 409 Conflict.

Escenario: Excepción / SOA
  Cuando se ejecuta POST /formulas
  Entonces el sistema aborta la transacción y responde con un código HTTP 500 Internal Server Error.

Escenario: Validación de Materiales - Inexistentes o Inactivos
  Dado que uno de los componentes asociados dentro del arreglo de la fórmula hace referencia a un id_material inexistente (mat-000) o previamente descontinuado
  Cuando se procesa la solicitud POST /formulas
  Entonces el sistema invalida la creación y responde con un código HTTP 422 Unprocessable Entity (o 400 Bad Request) especificando los materiales no válidos.

```
### Supuestos

- El estado por defecto al crear una fórmula es BORRADOR.
- El objeto de solicitud incluye un arreglo de componentes: componentes: [{ materialId, porcentaje }].

## HU-06: Consultar Fórmula

**Épico:** Yo como ingeniero de laboratorio, requiero consultar una fórmula para conocer su composición y en qué punto de la validación se encuentra.

**Endpoint:** `GET /formulas/{id}`

### Criterios de Aceptación

```gherkin
Escenario: Éxito
  Cuando un usuario (o el servicio externo de la API de Fabricación / Equipo 3) ejecuta una llamada GET /formulas/form-505
  Entonces el sistema responde con un código HTTP 200 OK entregando el detalle de componentes, proporciones y su estado de validación legal actual (BORRADOR, EVALUANDO_PATENTE, APROBADA, RECHAZADA).

Escenario: Validación Negocio
  Dado que se realiza la consulta enviando un parámetro id con un formato alfanumérico no correspondiente a la nomenclatura del sistema
  Cuando el cliente efectúa GET /formulas/abc_invalid_id
  Entonces la API retorna un código HTTP 400 Bad Request.

Escenario: Conflicto / Recurso
  Dado que la fórmula consultada no existe en los registros de la plataforma
  Cuando el usuario o un sistema consumidor (ej. API de Fabricación) solicita GET /formulas/form-000
  Entonces la API retorna un código HTTP 404 Not Found.

Escenario: Excepción / SOA
  Dado que el servicio de Materiales/Fórmulas no puede resolver el detalle de los materiales embebidos en la fórmula debido a una falla del servidor
  Cuando se atiende GET /formulas/{id}
  Entonces el sistema retorna un código HTTP 500 Internal Server Error.

```
### Supuestos

- Endpoint de consumo tanto para el Ingeniero de Laboratorio como para la API de Fabricación (Equipo 3) y la API de Patent Sweep (Equipo 2) bajo integración SOA.

## HU-07: Actualizar Fórmula

**Épico:** Yo como ingeniero de laboratorio, requiero modificar los materiales o proporciones de una fórmula existente, para corregirla o mejorarla antes de su uso en producción.

**Endpoint:** `PUT /formulas/{id}`

### Criterios de Aceptación

```gherkin
Escenario: Éxito
  Cuando el Ingeniero de Laboratorio actualiza el porcentaje de sus componentes mediante PUT /formulas/form-505 asegurando el 100% de la mezcla
  Entonces el sistema modifica la fórmula, incrementa la versión interna y retorna un código HTTP 200 OK.

Escenario: Validación Negocio
  Cuando el usuario envía la modificación mediante PUT /formulas/form-505 omitiendo la lista de componentes o sumando una proporción errónea
  Entonces el sistema rechaza los cambios y retorna un código HTTP 400 Bad Request.

Escenario: Conflicto / Recurso - Inexistente
  Dado que se intenta modificar una fórmula cuyo identificador form-000 no está registrado
  Cuando se envía PUT /formulas/form-000
  Entonces el sistema responde con un código HTTP 404 Not Found.

Escenario: Excepción / SOA
  Cuando se ejecuta PUT /formulas/{id}
  Entonces el sistema responde con un código HTTP 500 Internal Server Error.

Escenario: Bloqueo de Inmutabilidad - Estado Aprobado
  Dado que la fórmula form-505 ya fue previamente enviada a evaluación o dictaminada como APROBADA / EVALUANDO_PATENTE
  Cuando el Ingeniero de Laboratorio intenta modificarla vía PUT /formulas/form-505
  Entonces el sistema impide la edición por regla de inmutabilidad de producción, respondiendo con un código HTTP 409 Conflict.

```
### Supuestos

- Solo las fórmulas en estado BORRADOR pueden ser modificadas directamente; una fórmula en estado APROBADA requiere la creación de una nueva versión/fórmula.

## HU-08: Eliminar Fórmula

**Épico:** Yo como ingeniero de laboratorio, requiero descontinuar una fórmula, para evitar que el departamento de Fabricación produzca un producto con una fórmula que ya no está vigente.

**Endpoint:** `DELETE /formulas/{id}`

### Criterios de Aceptación

```gherkin
Escenario: Éxito
  Cuando el Ingeniero de Laboratorio realiza la petición DELETE /formulas/form-505
  Entonces el sistema actualiza su estado a DESCONTINUADA para invalidar su consumo en la API de Fabricación y responde con un código HTTP 200 OK.

Escenario: Validación Negocio
  Dado que la petición DELETE /formulas/form-505 no incluye la justificación obligatoria de descontinuación en los headers o parámetros requeridos
  Cuando se ejecuta la llamada
  Entonces el sistema rechaza el cambio de estado y retorna un código HTTP 400 Bad Request.

Escenario: Conflicto / Recurso
  Dado que la fórmula solicitada form-000 no existe en la base de datos
  Cuando el usuario envía DELETE /formulas/form-000
  Entonces el sistema retorna un código HTTP 404 Not Found.

Escenario: Excepción / SOA
  Dado que al momento de marcar como descontinuada la fórmula ocurre un error interno en la base de datos del servicio
  Cuando se procesa DELETE /formulas/{id}
  Entonces el sistema responde con un código HTTP 500 Internal Server Error.

```
### Supuestos

- Transición de estado a DESCONTINUADA. La API de Fabricación (Equipo 3) no podrá asociar lotes de producción a fórmulas en este estado.

## HU-09: Evaluar Patente

**Épico:** Yo como Ingeniero de Laboratorio, requiero solicitar la verificación legal de la composición de una fórmula, para garantizar que la mezcla no infrinja propiedad intelectual antes de ser aprobada para fabricación.

**Endpoint:** `POST /formulas/{id}/evaluar-patente`

### Criterios de Aceptación

```gherkin
Escenario: Éxito
  Dado que la fórmula form-505 está en estado BORRADOR y la API de Patent Sweep (Equipo 2) se encuentra disponible
  Cuando el usuario ejecuta POST /formulas/form-505/evaluar-patente
  Entonces la API de Materiales invoca el cliente HTTP de Patent Sweep, cambia el estado de la fórmula a EVALUANDO_PATENTE y retorna un código HTTP 200 OK con un ticketEvaluacionId.

Escenario: Validación Negocio
  Dado que la fórmula form-505 ya se encuentra en estado EVALUANDO_PATENTE o APROBADA
  Cuando el usuario ejecuta POST /formulas/form-505/evaluar-patente
  Entonces el sistema deniega la solicitud duplicada de evaluación y retorna un código HTTP 400 Bad Request (o 409 Conflict).

Escenario: Conflicto / Recurso
  Dado que el identificador de fórmula form-999 no existe en el catálogo
  Cuando el usuario solicita el análisis enviando POST /formulas/form-999/evaluar-patente
  Entonces el sistema responde con un código HTTP 404 Not Found.

Escenario: Excepción / SOA - Falla de Servicio Externo
  Dado que la API de Patent Sweep (Equipo 2) experimenta un timeout, caída o interrupción de red al ser invocada por el cliente HTTP del backend
  Cuando se intenta la solicitud vía POST /formulas/form-505/evaluar-patente
  Entonces el sistema deshace el cambio de estado de la fórmula y retorna un código HTTP 503 Service Unavailable notificando el fallo de integración SOA.

Escenario: Resultado Negativo de Patente - Rechazo Legal
  Dado que la API de Patent Sweep (Equipo 2) procesa la composición y detecta un conflicto de propiedad intelectual
  Cuando notifica la respuesta a la API de Materiales/Fórmulas
  Entonces el sistema actualiza el estado de la fórmula a RECHAZADA_POR_INFRACCION y registra las patentes en conflicto en el historial de auditoría.

```
### Supuestos

- Comunicación vía cliente HTTP hacia la API externa de Patent Sweep (Equipo 2).
- La invocación cambia el estado de la fórmula de BORRADOR a EVALUANDO_PATENTE.

---

## 4. Salida del validador — Informe de Auditoría de Calidad y Requisitos de Software

# Reporte de Auditoría: HU-01 - Crear materia

## Matriz INVEST de la Historia de Usuario
| Criterio | Estado (Cumple / Falla / Pendiente) | Justificación Técnica |
| :--- | :--- | :--- |
| **Independent** | Cumple | No depende de la existencia de otras entidades previas dentro del dominio para ser creada. |
| **Negotiable** | Cumple | Permite iterar sobre la estructura exacta del payload de propiedades. |
| **Valuable** | Cumple | Aporta valor directo al rol permitiendo poblar el catálogo base de componentes. |
| **Estimable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Small** | Cumple | Representa una operación CRUD atómica de creación. |
| **Testable** | Cumple | Cuenta con escenarios BDD verificables y criterios de aceptación definidos. |

## Evaluación de Criterios de Aceptación (BDD)

### CA-01 (Éxito):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Falla | Omite especificar la estructura exacta del objeto JSON en las premisas/resultados. |
| **Measurable** | Cumple | Verifica el código de respuesta HTTP 201 Created y el retorno del recurso. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Define el camino feliz fundamental para el registro de insumos. |
| **Time-boxed** | Cumple | La verificación es inmediata al término de la ejecución del endpoint. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`POST /materiales`)
* **¿Campos/Payload?:** No (Menciona "estructura JSON requerida" sin detallar tipos ni nombres de campos en el CA)
* **¿Código HTTP/Error?:** Sí (`HTTP 201 Created`)
* **¿Restricciones/Reglas?:** Sí (Valores técnicos válidos y asignación de ID único)
* **¿Condición Atómica?:** Sí (Evalúa exclusivamente la creación exitosa)

### CA-02 (Validación Negocio):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Identifica claramente las fallas por campos obligatorios omitidos y valores fuera de rango. |
| **Measurable** | Cumple | Retorna código HTTP 400 Bad Request y mensaje de error estructurado. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Valida la integridad de datos antes de la persistencia. |
| **Time-boxed** | Cumple | Ejecución acotada a la petición HTTP. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`POST /materiales`)
* **¿Campos/Payload?:** Sí (Menciona código CAS, unidad de medida y densidad)
* **¿Código HTTP/Error?:** Sí (`HTTP 400 Bad Request con detalle de validaciones`)
* **¿Restricciones/Reglas?:** Sí (Inconsistencia de datos, densidad negativa)
* **¿Condición Atómica?:** Sí (Evalúa únicamente la falla de validación de entrada)

### CA-03 (Conflicto / Recurso):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Detalla la duplicidad basada en código CAS o nombre comercial. |
| **Measurable** | Cumple | Retorna un código HTTP 409 Conflict. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Evita registros duplicados en el catálogo central. |
| **Time-boxed** | Cumple | Verificable en tiempo de solicitud. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`POST /materiales`)
* **¿Campos/Payload?:** Sí (código CAS o nombre comercial)
* **¿Código HTTP/Error?:** Sí (`HTTP 409 Conflict`)
* **¿Restricciones/Reglas?:** Sí (Unicidad de código CAS/nombre)
* **¿Condición Atómica?:** Sí (Evalúa únicamente el conflicto de duplicidad)

### CA-04 (Excepción / SOA):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Define la falla de infraestructura/timeout en la transacción de base de datos. |
| **Measurable** | Cumple | Retorna código HTTP 500 Internal Server Error con ID de traza. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Garantiza el manejo adecuado de excepciones no controladas. |
| **Time-boxed** | Cumple | Verificable tras la ocurrencia del timeout o falla. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`POST /materiales`)
* **¿Campos/Payload?:** No (No aplica por ser excepción no controlada de capa de datos)
* **¿Código HTTP/Error?:** Sí (`HTTP 500 Internal Server Error con identificador de traza`)
* **¿Restricciones/Reglas?:** Sí (Fallo de conexión o timeout durante inserción)
* **¿Condición Atómica?:** Sí (Evalúa únicamente el fallo del servidor)

## Veredicto Final de la HU
**APROBADA CON OBSERVACIONES**

* **Justificación del Veredicto:** La HU cumple el 100% de los criterios INVEST (con "Estimable" marcado como pendiente). Cubre la matriz mínima obligatoria de 4 escenarios HTTP (201, 400, 409, 500). Presenta únicamente 1 falla menor en la dimensión "Specific" del CA-01 por no detallar los nombres exactos de los campos en el cuerpo del CA (aunque están listados en los supuestos).
* **Acción Requerida / Sugerencia:** Incorporar de manera explícita en el texto del CA-01 la lista de campos en JSON (codigoCAS, nombre, unidadMedida, densidad, propiedades) para evitar ambigüedades en la automatización de pruebas.

---

# Reporte de Auditoría: HU-02 - Consultar material

## Matriz INVEST de la Historia de Usuario
| Criterio | Estado (Cumple / Falla / Pendiente) | Justificación Técnica |
| :--- | :--- | :--- |
| **Independent** | Cumple | Es una consulta de lectura atómica sobre el catálogo. |
| **Negotiable** | Cumple | Permite discutir la inclusión o exclusión de campos en la respuesta. |
| **Valuable** | Cumple | Otorga visibilidad de fichas técnicas para el diseño de fórmulas. |
| **Estimable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Small** | Cumple | Consulta simple mediante parámetro de ruta. |
| **Testable** | Cumple | Criterios BDD completamente verificables mediante API Testing. |

## Evaluación de Criterios de Aceptación (BDD)

### CA-01 (Éxito):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Define el recurso exacto por ID (mat-101) y la respuesta esperada. |
| **Measurable** | Cumple | Código HTTP 200 OK y objeto JSON completo retornado. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Consulta básica para la operación de laboratorio. |
| **Time-boxed** | Cumple | Verificación inmediata. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`GET /materiales/mat`-101)
* **¿Campos/Payload?:** Sí (Retorna JSON con especificaciones físicas, químicas y estado)
* **¿Código HTTP/Error?:** Sí (`HTTP 200 OK`)
* **¿Restricciones/Reglas?:** Sí (Debe existir previamente en el catálogo)
* **¿Condición Atómica?:** Sí (Evalúa únicamente la lectura exitosa)

### CA-02 (Validación Negocio):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Identifica la sintaxis inválida en el parámetro de ruta. |
| **Measurable** | Cumple | Retorna código HTTP 400 Bad Request. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Evita inyecciones o parámetros malformados. |
| **Time-boxed** | Cumple | Verificación inmediata en la capa de ruteo. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`GET /materiales/ID_INVALIDO!`)
* **¿Campos/Payload?:** Sí (Parámetro de ruta id no conforme)
* **¿Código HTTP/Error?:** Sí (`HTTP 400 Bad Request`)
* **¿Restricciones/Reglas?:** Sí (Validación de formato en identificador)
* **¿Condición Atómica?:** Sí (Evalúa la validación de formato de parámetro)

### CA-03 (Conflicto / Recurso):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Identifica la búsqueda de un identificador no existente (mat-999). |
| **Measurable** | Cumple | Retorna código HTTP 404 Not Found. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Informa la ausencia del recurso solicitado. |
| **Time-boxed** | Cumple | Respuesta inmediata del servicio. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`GET /materiales/mat`-999)
* **¿Campos/Payload?:** Sí (Identificador inexistente como parámetro)
* **¿Código HTTP/Error?:** Sí (`HTTP 404 Not Found`)
* **¿Restricciones/Reglas?:** Sí (Inexistencia en base de datos)
* **¿Condición Atómica?:** Sí (Evalúa el manejo de recurso no encontrado)

### CA-04 (Excepción / SOA):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Describe la degradación o indisponibilidad en la capa de lectura. |
| **Measurable** | Cumple | Retorna HTTP 500 Internal Server Error o 503 Service Unavailable. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Controla la indisponibilidad de la base de datos de lectura. |
| **Time-boxed** | Cumple | Verificable bajo fallo de infraestructura. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`GET /materiales/{id}`)
* **¿Campos/Payload?:** No (No aplica)
* **¿Código HTTP/Error?:** Sí (`HTTP 500 / 503`)
* **¿Restricciones/Reglas?:** Sí (Falla de conexión/persistencia)
* **¿Condición Atómica?:** Sí (Evalúa la resiliencia ante caídas de servicio)

## Veredicto Final de la HU
**APROBADA**

* **Justificación del Veredicto:** Cumple estrictamente con INVEST (Estimable en Pendiente), SMART (Achievable en Pendiente), responde afirmativamente al cuestionario atómico en todos los casos y posee cobertura completa de los 4 escenarios de la matriz HTTP (200, 400, 404, 500/503).
* **Acción Requerida / Sugerencia:** Ninguna. La HU está lista para desarrollo.

---

# Reporte de Auditoría: HU-03 - Actualizar material

## Matriz INVEST de la Historia de Usuario
| Criterio | Estado (Cumple / Falla / Pendiente) | Justificación Técnica |
| :--- | :--- | :--- |
| **Independent** | Cumple | Opera sobre una entidad preexistente sin depender de otras HUs. |
| **Negotiable** | Cumple | Los campos actualizables pueden refinarse. |
| **Valuable** | Cumple | Mantiene la vigencia técnica de las fichas de los materiales. |
| **Estimable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Small** | Cumple | Operación acotada a la modificación de un recurso por ID. |
| **Testable** | Cumple | Criterios BDD bien estructurados y medibles. |

## Evaluación de Criterios de Aceptación (BDD)

### CA-01 (Éxito):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Describe la actualización válida sobre un ID existente (mat-101). |
| **Measurable** | Cumple | Retorna HTTP 200 OK y persiste la fecha de modificación. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Permite la evolución de los datos de un material. |
| **Time-boxed** | Cumple | Verificable al finalizar la petición PUT. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`PUT /materiales/mat`-101)
* **¿Campos/Payload?:** Sí (Menciona modificación de ficha de seguridad o propiedades)
* **¿Código HTTP/Error?:** Sí (`HTTP 200 OK`)
* **¿Restricciones/Reglas?:** Sí (Registro de auditoría/fecha de actualización)
* **¿Condición Atómica?:** Sí (Evalúa únicamente la actualización exitosa)

### CA-02 (Validación Negocio):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Especifica la regla de rango (ej. pureza > 100%) y campos vacíos. |
| **Measurable** | Cumple | Retorna código HTTP 400 Bad Request. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Garantiza la validez lógica de los datos actualizados. |
| **Time-boxed** | Cumple | Verificación inmediata. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`PUT /materiales/mat`-101)
* **¿Campos/Payload?:** Sí (Campos fuera de rango técnico o requeridos vacíos)
* **¿Código HTTP/Error?:** Sí (`HTTP 400 Bad Request`)
* **¿Restricciones/Reglas?:** Sí (Límites físicos/químicos de las propiedades)
* **¿Condición Atómica?:** Sí (Evalúa solo las reglas de validación de negocio)

### CA-03 (Conflicto / Recurso):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Identifica la actualización sobre un ID no registrado (mat-888). |
| **Measurable** | Cumple | Retorna código HTTP 404 Not Found. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Notifica la imposibilidad de actualizar recursos inexistentes. |
| **Time-boxed** | Cumple | Verificación inmediata. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`PUT /materiales/mat`-888)
* **¿Campos/Payload?:** Sí (ID no registrado)
* **¿Código HTTP/Error?:** Sí (`HTTP 404 Not Found`)
* **¿Restricciones/Reglas?:** Sí (Existencia previa del recurso)
* **¿Condición Atómica?:** Sí (Evalúa únicamente el recurso no encontrado)

### CA-04 (Excepción / SOA):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Especifica fallos de concurrencia o interrupción de base de datos. |
| **Measurable** | Cumple | Exige rollback transaccional y retorna HTTP 500 Internal Server Error. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Protege la consistencia de los datos ante errores del servidor. |
| **Time-boxed** | Cumple | Evaluado durante el fallo de infraestructura. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`PUT /materiales/{id}`)
* **¿Campos/Payload?:** No (No aplica)
* **¿Código HTTP/Error?:** Sí (`HTTP 500 Internal Server Error`)
* **¿Restricciones/Reglas?:** Sí (Rollback obligatorio de la transacción)
* **¿Condición Atómica?:** Sí (Evalúa la tolerancia a fallos transaccionales)

## Veredicto Final de la HU
**APROBADA**

* **Justificación del Veredicto:** Cumple con la totalidad de los criterios INVEST, SMART, el cuestionario atómico de 5 preguntas y cubre la matriz HTTP exigida (200, 400, 404 y 500) incluyendo una regla explícita de rollback transaccional en la excepción SOA.
* **Acción Requerida / Sugerencia:** Ninguna. La HU es robusta.

---

# Reporte de Auditoría: HU-04 - Eliminar/Inhabilitar material

## Matriz INVEST de la Historia de Usuario
| Criterio | Estado (Cumple / Falla / Pendiente) | Justificación Técnica |
| :--- | :--- | :--- |
| **Independent** | Cumple | Evalúa el cambio de estado de un recurso específico. |
| **Negotiable** | Cumple | El mecanismo de confirmación/desactivación es negociable. |
| **Valuable** | Cumple | Previene el uso de materiales obsoletos en nuevas formulaciones. |
| **Estimable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Small** | Cumple | Cambio de estado de un único registro. |
| **Testable** | Cumple | Escenarios BDD claramente verificables. |

## Evaluación de Criterios de Aceptación (BDD)

### CA-01 (Éxito):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Especifica la transición a INACTIVO sobre mat-101 sin borradores asociados. |
| **Measurable** | Cumple | Retorna código HTTP 200 OK (o 204 No Content). |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Ejecuta la baja lógica (Soft Delete) requerida por el dominio. |
| **Time-boxed** | Cumple | Evaluado al procesar la solicitud. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`DELETE /materiales/mat`-101)
* **¿Campos/Payload?:** Sí (Inexistencia de vinculación a borradores activos)
* **¿Código HTTP/Error?:** Sí (`HTTP 200 OK / 204 No Content`)
* **¿Restricciones/Reglas?:** Sí (Soft Delete, cambio de estado a INACTIVO)
* **¿Condición Atómica?:** Sí (Evalúa únicamente el borrado lógico exitoso)

### CA-02 (Validación Negocio):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Identifica la ausencia de reglas de confirmación requeridas. |
| **Measurable** | Cumple | Retorna un código HTTP 400 Bad Request. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Previene desactivaciones accidentales sin confirmación previa. |
| **Time-boxed** | Cumple | Verificación inmediata. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`DELETE /materiales/mat`-101)
* **¿Campos/Payload?:** Sí (Falta de header/payload de confirmación)
* **¿Código HTTP/Error?:** Sí (`HTTP 400 Bad Request`)
* **¿Restricciones/Reglas?:** Sí (Reglas de confirmación obligatorias)
* **¿Condición Atómica?:** Sí (Evalúa el rechazo por falta de confirmación)

### CA-03 (Conflicto / Recurso):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Modela el conflicto de integridad por estar vinculado a fórmulas activas. |
| **Measurable** | Cumple | Retorna código HTTP 409 Conflict. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Esencial para garantizar la integridad referencial del catálogo. |
| **Time-boxed** | Cumple | Evaluado en tiempo de petición. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`DELETE /materiales/mat`-101)
* **¿Campos/Payload?:** Sí (Material en uso activo en fórmulas)
* **¿Código HTTP/Error?:** Sí (`HTTP 409 Conflict`)
* **¿Restricciones/Reglas?:** Sí (Regla de negocio sobre inmutabilidad por dependencia)
* **¿Condición Atómica?:** Sí (Evalúa la protección de integridad por dependencias)

### CA-04 (Excepción / SOA):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Contempla la falla interna del motor de base de datos durante el marcado. |
| **Measurable** | Cumple | Retorna código HTTP 500 Internal Server Error. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Manejo adecuado de errores de infraestructura. |
| **Time-boxed** | Cumple | Verificable durante el evento de falla. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`DELETE /materiales/{id}`)
* **¿Campos/Payload?:** No (No aplica)
* **¿Código HTTP/Error?:** Sí (`HTTP 500 Internal Server Error`)
* **¿Restricciones/Reglas?:** Sí (Error interno al modificar estado)
* **¿Condición Atómica?:** Sí (Evalúa la falla técnica del motor de persistencia)

## Veredicto Final de la HU
**APROBADA**

* **Justificación del Veredicto:** La HU satisface INVEST, SMART y el cuestionario atómico de 5 preguntas en todos sus CAs. Contempla de manera sobresaliente el conflicto de integridad referencial (HTTP 409) con las fórmulas en proceso y el manejo de excepciones de infraestructura (HTTP 500).
* **Acción Requerida / Sugerencia:** Especificar formalmente en el CA-02 la nomenclatura del header o parámetro de confirmación (ej. X-Confirm-Deactivation: true).

---

# Reporte de Auditoría: HU-05 - Crear Fórmula

## Matriz INVEST de la Historia de Usuario
| Criterio | Estado (Cumple / Falla / Pendiente) | Justificación Técnica |
| :--- | :--- | :--- |
| **Independent** | Cumple | Se enfoca exclusivamente en la creación de la fórmula. |
| **Negotiable** | Cumple | Los límites de componentes y propiedades son ajustables. |
| **Valuable** | Cumple | Permite estructurar las recetas para la fabricación de cuchillas. |
| **Estimable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Small** | Cumple | Corresponde al registro de una entidad principal con sus detalles. |
| **Testable** | Cumple | Altamente verificable mediante pruebas automáticas de API. |

## Evaluación de Criterios de Aceptación (BDD)

### CA-01 (Éxito):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Exige lista de materiales activos y suma exacta de proporciones igual a 100%. |
| **Measurable** | Cumple | Retorna HTTP 201 Created y asigna el ID con estado BORRADOR. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Registra el artefacto clave para la producción. |
| **Time-boxed** | Cumple | Verificable al procesar la creación. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`POST /formulas`)
* **¿Campos/Payload?:** Sí (Nombre de mezcla, aplicación técnica, componentes)
* **¿Código HTTP/Error?:** Sí (`HTTP 201 Created`)
* **¿Restricciones/Reglas?:** Sí (Suma de proporciones = 100%, estado BORRADOR)
* **¿Condición Atómica?:** Sí (Evalúa la creación atómica de la receta)

### CA-02 (Validación Negocio):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Modela la inconsistencia aritmética de proporciones (ej. 95%). |
| **Measurable** | Cumple | Retorna código HTTP 400 Bad Request. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Protege la integridad de la receta industrial. |
| **Time-boxed** | Cumple | Verificación inmediata en capa de validación. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`POST /formulas`)
* **¿Campos/Payload?:** Sí (Porcentaje de componentes sumando distinto de 100%)
* **¿Código HTTP/Error?:** Sí (`HTTP 400 Bad Request`)
* **¿Restricciones/Reglas?:** Sí (Regla de negocio aritmética: total === 100%)
* **¿Condición Atómica?:** Sí (Evalúa solo la regla de la suma de porcentajes)

### CA-03 (Conflicto / Recurso):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Identifica la duplicidad por identificador o código técnico. |
| **Measurable** | Cumple | Retorna código HTTP 409 Conflict. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Previene fórmulas repetidas bajo la misma nomenclatura. |
| **Time-boxed** | Cumple | Evaluado al momento del guardado. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`POST /formulas`)
* **¿Campos/Payload?:** Sí (Código técnico o ID duplicado)
* **¿Código HTTP/Error?:** Sí (`HTTP 409 Conflict`)
* **¿Restricciones/Reglas?:** Sí (Unicidad de fórmula)
* **¿Condición Atómica?:** Sí (Evalúa la detección de duplicados)

### CA-04 (Excepción / SOA):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Detalla la falla transaccional al guardar maestro-detalle. |
| **Measurable** | Cumple | Aborta transacción (rollback) y retorna HTTP 500 Internal Server Error. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Garantiza que no existan componentes huérfanos sin su cabecera. |
| **Time-boxed** | Cumple | Evaluado tras la falla de persistencia. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`POST /formulas`)
* **¿Campos/Payload?:** No (No aplica)
* **¿Código HTTP/Error?:** Sí (`HTTP 500 Internal Server Error`)
* **¿Restricciones/Reglas?:** Sí (Aborto transaccional de estructura relacional)
* **¿Condición Atómica?:** Sí (Evalúa el manejo de excepciones de persistencia)

### CA-05 (Validación de Materiales - Inexistentes o Inactivos):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Identifica el uso de un id_material inexistente (mat-000) o inactivo. |
| **Measurable** | Cumple | Retorna código HTTP 422 Unprocessable Entity (o 400 Bad Request). |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Vital para la consistencia del catálogo de materiales. |
| **Time-boxed** | Cumple | Verificación inmediata previa a la inserción. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`POST /formulas`)
* **¿Campos/Payload?:** Sí (Arreglo componentes con id_material inválido/inactivo)
* **¿Código HTTP/Error?:** Sí (`HTTP 422 Unprocessable Entity / 400 Bad Request`)
* **¿Restricciones/Reglas?:** Sí (Existencia y estado ACTIVO del material)
* **¿Condición Atómica?:** Sí (Evalúa la validez de las referencias a materiales)

## Veredicto Final de la HU
**APROBADA**

* **Justificación del Veredicto:** La HU es de excelente calidad técnica. Satisface INVEST, SMART, el cuestionario atómico y supera la cobertura mínima de matriz de errores (incorporando de forma acertada el código HTTP 422 para fallas de validación de dominios cruzados).
* **Acción Requerida / Sugerencia:** Estandarizar el retorno de error de componentes no válidos definitivamente en HTTP 422 para mantener coherencia RESTful.

---

# Reporte de Auditoría: HU-06 - Consultar Fórmula

## Matriz INVEST de la Historia de Usuario
| Criterio | Estado (Cumple / Falla / Pendiente) | Justificación Técnica |
| :--- | :--- | :--- |
| **Independent** | Cumple | Operación de lectura desacoplada. |
| **Negotiable** | Cumple | Se pueden negociar los detalles a retornar sobre el flujo de patentes. |
| **Valuable** | Cumple | Expone la composición a los sistemas consumidores (Fabricación y Patentes). |
| **Estimable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Small** | Cumple | Endpoint de consulta individual por ID. |
| **Testable** | Cumple | Totalmente ejecutable mediante peticiones HTTP GET. |

## Evaluación de Criterios de Aceptación (BDD)

### CA-01 (Éxito):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Define el consumo por el ID form-505 y la entrega del objeto completo con sus estados. |
| **Measurable** | Cumple | Retorna HTTP 200 OK y el detalle de componentes y estado legal. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Satisface el punto de integración SOA con Equipos 2 y 3. |
| **Time-boxed** | Cumple | Verificación en tiempo de ejecución. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`GET /formulas/form`-505)
* **¿Campos/Payload?:** Sí (Componentes, proporciones y estado de validación legal)
* **¿Código HTTP/Error?:** Sí (`HTTP 200 OK`)
* **¿Restricciones/Reglas?:** Sí (Transición y retorno de estados: BORRADOR, EVALUANDO_PATENTE, etc.)
* **¿Condición Atómica?:** Sí (Evalúa la consulta de fórmula exitosa)

### CA-02 (Validación Negocio):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Identifica la sintaxis alfanumérica inválida en el ID (abc_invalid_id). |
| **Measurable** | Cumple | Retorna código HTTP 400 Bad Request. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Valida la estructura del parámetro de ruta. |
| **Time-boxed** | Cumple | Verificación inmediata. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`GET /formulas/abc_invalid_id`)
* **¿Campos/Payload?:** Sí (ID con formato no conforme)
* **¿Código HTTP/Error?:** Sí (`HTTP 400 Bad Request`)
* **¿Restricciones/Reglas?:** Sí (Patrón de nomenclatura de identificadores)
* **¿Condición Atómica?:** Sí (Evalúa el rechazo por parámetro malformado)

### CA-03 (Conflicto / Recurso):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Especifica la búsqueda de una fórmula no registrada (form-000). |
| **Measurable** | Cumple | Retorna código HTTP 404 Not Found. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Informa la ausencia del recurso a los sistemas integrados. |
| **Time-boxed** | Cumple | Respuesta inmediata. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`GET /formulas/form`-000)
* **¿Campos/Payload?:** Sí (Identificador inexistente)
* **¿Código HTTP/Error?:** Sí (`HTTP 404 Not Found`)
* **¿Restricciones/Reglas?:** Sí (Inexistencia en el repositorio)
* **¿Condición Atómica?:** Sí (Evalúa el manejo de fórmula no encontrada)

### CA-04 (Excepción / SOA):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Modela la falla de resolución de materiales embebidos por error de servidor. |
| **Measurable** | Cumple | Retorna código HTTP 500 Internal Server Error. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Captura la falla técnica en la agregación de datos. |
| **Time-boxed** | Cumple | Evaluado ante la falla del servicio. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`GET /formulas/{id}`)
* **¿Campos/Payload?:** No (No aplica)
* **¿Código HTTP/Error?:** Sí (`HTTP 500 Internal Server Error`)
* **¿Restricciones/Reglas?:** Sí (Imposibilidad de componer el recurso embebido)
* **¿Condición Atómica?:** Sí (Evalúa la excepción interna durante la hidratación del objeto)

## Veredicto Final de la HU
**APROBADA**

* **Justificación del Veredicto:** Cumple totalmente con los criterios INVEST, SMART y el cuestionario atómico. Integra explícitamente el valor de negocio de la arquitectura SOA (consumo por Equipos 2 y 3) y posee cobertura de matriz de respuestas de error.
* **Acción Requerida / Sugerencia:** Ninguna.

---

# Reporte de Auditoría: HU-07 - Actualizar Fórmula

## Matriz INVEST de la Historia de Usuario
| Criterio | Estado (Cumple / Falla / Pendiente) | Justificación Técnica |
| :--- | :--- | :--- |
| **Independent** | Cumple | Operación acotada a la modificación de un recurso existente. |
| **Negotiable** | Cumple | Permite discutir las reglas de versionado. |
| **Valuable** | Cumple | Facilita la corrección y optimización de recetas antes de producción. |
| **Estimable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Small** | Cumple | Actualización de una entidad por su ID. |
| **Testable** | Cumple | Totalmente ejecutable con pruebas de API. |

## Evaluación de Criterios de Aceptación (BDD)

### CA-01 (Éxito):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Define la modificación sobre form-505 en BORRADOR incrementando versión. |
| **Measurable** | Cumple | Retorna HTTP 200 OK con la versión incrementada. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Permite iterar la mezcla en fase de diseño. |
| **Time-boxed** | Cumple | Evaluado al completar la petición PUT. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`PUT /formulas/form`-505)
* **¿Campos/Payload?:** Sí (Porcentaje de componentes sumando 100%)
* **¿Código HTTP/Error?:** Sí (`HTTP 200 OK`)
* **¿Restricciones/Reglas?:** Sí (Estado BORRADOR e incremento de versión)
* **¿Condición Atómica?:** Sí (Evalúa únicamente la actualización exitosa)

### CA-02 (Validación Negocio):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Modela la omisión de componentes o suma errónea en form-505. |
| **Measurable** | Cumple | Retorna código HTTP 400 Bad Request. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Mantiene la consistencia matemática en las ediciones. |
| **Time-boxed** | Cumple | Verificación inmediata. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`PUT /formulas/form`-505)
* **¿Campos/Payload?:** Sí (Lista de componentes omitida o con suma != 100%)
* **¿Código HTTP/Error?:** Sí (`HTTP 400 Bad Request`)
* **¿Restricciones/Reglas?:** Sí (Reglas de validación de estructura y proporciones)
* **¿Condición Atómica?:** Sí (Evalúa la falla por datos inconsistentes)

### CA-03 (Conflicto / Recurso - Inexistente):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Modela el intento de modificación sobre un ID no existente (form-000). |
| **Measurable** | Cumple | Retorna código HTTP 404 Not Found. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Garantiza el reporte correcto de inexistencia. |
| **Time-boxed** | Cumple | Respuesta inmediata. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`PUT /formulas/form`-000)
* **¿Campos/Payload?:** Sí (Identificador no registrado)
* **¿Código HTTP/Error?:** Sí (`HTTP 404 Not Found`)
* **¿Restricciones/Reglas?:** Sí (Existencia del recurso)
* **¿Condición Atómica?:** Sí (Evalúa el rechazo por recurso inexistente)

### CA-04 (Excepción / SOA):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Describe la caída de conexión con el repositorio de datos durante el guardado. |
| **Measurable** | Cumple | Retorna código HTTP 500 Internal Server Error. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Captura las excepciones de infraestructura. |
| **Time-boxed** | Cumple | Verificable al fallar la persistencia. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`PUT /formulas/{id}`)
* **¿Campos/Payload?:** No (No aplica)
* **¿Código HTTP/Error?:** Sí (`HTTP 500 Internal Server Error`)
* **¿Restricciones/Reglas?:** Sí (Interrupción de red/bd)
* **¿Condición Atómica?:** Sí (Evalúa el error técnico de servidor)

### CA-05 (Bloqueo de Inmutabilidad - Estado Aprobado):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Identifica la regla de inmutabilidad en estados APROBADA o EVALUANDO_PATENTE. |
| **Measurable** | Cumple | Retorna un código HTTP 409 Conflict. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Crucial para evitar alterar fórmulas en producción o bajo evaluación legal. |
| **Time-boxed** | Cumple | Evaluado al validar el estado del recurso. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`PUT /formulas/form`-505)
* **¿Campos/Payload?:** Sí (Modificación sobre fórmula aprobada/en evaluación)
* **¿Código HTTP/Error?:** Sí (`HTTP 409 Conflict`)
* **¿Restricciones/Reglas?:** Sí (Inmutabilidad de estado por regla de negocio industrial)
* **¿Condición Atómica?:** Sí (Evalúa exclusivamente la protección por inmutabilidad)

## Veredicto Final de la HU
**APROBADA**

* **Justificación del Veredicto:** Cumple rigurosamente INVEST, SMART y el cuestionario atómico. Sobresale el CA-05 al definir la regla de inmutabilidad de dominio (HTTP 409) para fórmulas fuera del estado BORRADOR, cubriendo de forma completa la matriz de seguridad de la información.
* **Acción Requerida / Sugerencia:** Ninguna.

---

# Reporte de Auditoría: HU-08 - Eliminar Fórmula

## Matriz INVEST de la Historia de Usuario
| Criterio | Estado (Cumple / Falla / Pendiente) | Justificación Técnica |
| :--- | :--- | :--- |
| **Independent** | Cumple | Transición de estado de un recurso individual. |
| **Negotiable** | Cumple | Negociable la obligatoriedad del motivo de descontinuación. |
| **Valuable** | Cumple | Previene el consumo de fórmulas obsoletas por el departamento de Fabricación. |
| **Estimable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Small** | Cumple | Operación atómica de actualización de estado. |
| **Testable** | Cumple | Totalmente verificable vía pruebas HTTP. |

## Evaluación de Criterios de Aceptación (BDD)

### CA-01 (Éxito):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Define el cambio de estado a DESCONTINUADA para form-505. |
| **Measurable** | Cumple | Retorna código HTTP 200 OK e invalida consumo en API de Fabricación. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Garantiza que Fabricación no utilice recetas no vigentes. |
| **Time-boxed** | Cumple | Verificable tras la respuesta. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`DELETE /formulas/form`-505)
* **¿Campos/Payload?:** Sí (Fórmula activa como prerrequisito)
* **¿Código HTTP/Error?:** Sí (`HTTP 200 OK`)
* **¿Restricciones/Reglas?:** Sí (Estado final DESCONTINUADA)
* **¿Condición Atómica?:** Sí (Evalúa únicamente la baja lógica exitosa)

### CA-02 (Validación Negocio):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Describe el rechazo por falta de justificación obligatoria en la solicitud. |
| **Measurable** | Cumple | Retorna código HTTP 400 Bad Request. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Asegura trazabilidad sobre por qué se descontinúa un producto. |
| **Time-boxed** | Cumple | Evaluado al procesar la entrada. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`DELETE /formulas/form`-505)
* **¿Campos/Payload?:** Sí (Headers/parámetros sin la justificación obligatoria)
* **¿Código HTTP/Error?:** Sí (`HTTP 400 Bad Request`)
* **¿Restricciones/Reglas?:** Sí (Regla de trazabilidad: justificación requerida)
* **¿Condición Atómica?:** Sí (Evalúa el rechazo por omisión de justificación)

### CA-03 (Conflicto / Recurso):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Identifica el intento de descontinuar una fórmula inexistente (form-000). |
| **Measurable** | Cumple | Retorna código HTTP 404 Not Found. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Informa la ausencia del recurso. |
| **Time-boxed** | Cumple | Respuesta inmediata. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`DELETE /formulas/form`-000)
* **¿Campos/Payload?:** Sí (ID inexistente)
* **¿Código HTTP/Error?:** Sí (`HTTP 404 Not Found`)
* **¿Restricciones/Reglas?:** Sí (Existencia del recurso)
* **¿Condición Atómica?:** Sí (Evalúa el rechazo sobre recurso inexistente)

### CA-04 (Excepción / SOA):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Modela el error interno de base de datos durante la marcación del estado. |
| **Measurable** | Cumple | Retorna código HTTP 500 Internal Server Error. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Manejo de excepciones de la capa de persistencia. |
| **Time-boxed** | Cumple | Verificable al fallar la transacción. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`DELETE /formulas/{id}`)
* **¿Campos/Payload?:** No (No aplica)
* **¿Código HTTP/Error?:** Sí (`HTTP 500 Internal Server Error`)
* **¿Restricciones/Reglas?:** Sí (Error del motor de datos)
* **¿Condición Atómica?:** Sí (Evalúa la falla técnica durante el cambio de estado)

## Veredicto Final de la HU
**APROBADA**

* **Justificación del Veredicto:** Cumple la totalidad de los criterios INVEST, SMART, preguntas del cuestionario y la matriz HTTP obligatoria. Cubre explícitamente el requisito de auditoría/justificación para descontinuar productos.
* **Acción Requerida / Sugerencia:** Ninguna.

---

# Reporte de Auditoría: HU-09 - Evaluar Patente

## Matriz INVEST de la Historia de Usuario
| Criterio | Estado (Cumple / Falla / Pendiente) | Justificación Técnica |
| :--- | :--- | :--- |
| **Independent** | Cumple | Proceso acotado a la solicitud de evaluación de una fórmula específica. |
| **Negotiable** | Cumple | Permite definir los datos retornados en la integración. |
| **Valuable** | Cumple | Garantiza el cumplimiento legal y evita demandas por propiedad intelectual. |
| **Estimable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Small** | Cumple | Desencadena una orquestación/llamada a un servicio externo. |
| **Testable** | Cumple | Totalmente verificable mediante dobles de prueba (Mocks/Stubs) de la API SOA. |

## Evaluación de Criterios de Aceptación (BDD)

### CA-01 (Éxito):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Detalla la llamada HTTP a Patent Sweep, cambio a EVALUANDO_PATENTE y retorno de ticket. |
| **Measurable** | Cumple | Retorna HTTP 200 OK con ticketEvaluacionId. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Inicia el flujo legal inter-servicio (SOA). |
| **Time-boxed** | Cumple | Evaluado al completar la llamada síncrona/asíncrona. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`POST /formulas/form-505/evaluar-patente`)
* **¿Campos/Payload?:** Sí (Retorno de ticketEvaluacionId en la respuesta)
* **¿Código HTTP/Error?:** Sí (`HTTP 200 OK`)
* **¿Restricciones/Reglas?:** Sí (Fórmula en BORRADOR y API Patent Sweep disponible)
* **¿Condición Atómica?:** Sí (Evalúa la solicitud exitosa de evaluación)

### CA-02 (Validación Negocio):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Describe el rechazo por intento de reevaluar fórmulas en EVALUANDO_PATENTE o APROBADA. |
| **Measurable** | Cumple | Retorna código HTTP 400 Bad Request (o 409 Conflict). |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Evita solicitudes duplicadas e innecesarias hacia el servicio externo. |
| **Time-boxed** | Cumple | Verificación inmediata de estado. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`POST /formulas/form-505/evaluar-patente`)
* **¿Campos/Payload?:** Sí (Fórmula ya evaluada o en evaluación)
* **¿Código HTTP/Error?:** Sí (`HTTP 400 Bad Request / 409 Conflict`)
* **¿Restricciones/Reglas?:** Sí (Prohibición de reevaluación directa sin cambios)
* **¿Condición Atómica?:** Sí (Evalúa el bloqueo de reevaluación)

### CA-03 (Conflicto / Recurso):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Describe la solicitud sobre una fórmula inexistente (form-999). |
| **Measurable** | Cumple | Retorna código HTTP 404 Not Found. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Previene invocaciones externas con IDs no válidos. |
| **Time-boxed** | Cumple | Respuesta inmediata. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`POST /formulas/form-999/evaluar-patente`)
* **¿Campos/Payload?:** Sí (Identificador no registrado)
* **¿Código HTTP/Error?:** Sí (`HTTP 404 Not Found`)
* **¿Restricciones/Reglas?:** Sí (Existencia previa de la fórmula)
* **¿Condición Atómica?:** Sí (Evalúa el rechazo por recurso no encontrado)

### CA-04 (Excepción / SOA - Falla de Servicio Externo):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Modela minuciosamente la caída/timeout de la API de Patent Sweep (Equipo 2) y el rollback de estado. |
| **Measurable** | Cumple | Exige rollback de estado y retorna HTTP 503 Service Unavailable. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Ejemplo crítico de resiliencia y desacoplamiento en arquitectura SOA. |
| **Time-boxed** | Cumple | Evaluado al expirar el timeout o recibir la falla de red. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (`POST /formulas/form-505/evaluar-patente`)
* **¿Campos/Payload?:** No (No aplica)
* **¿Código HTTP/Error?:** Sí (`HTTP 503 Service Unavailable`)
* **¿Restricciones/Reglas?:** Sí (Rollback del estado de la fórmula al ocurrir la falla SOA)
* **¿Condición Atómica?:** Sí (Evalúa exclusivamente la resiliencia ante caídas de la API consumida)

### CA-05 (Resultado Negativo de Patente - Rechazo Legal):

#### Tabla SMART
| Dimensión | Estado | Justificación |
| :--- | :--- | :--- |
| **Specific** | Cumple | Describe la recepción del dictamen de infracción, cambio a RECHAZADA_POR_INFRACCION y auditoría. |
| **Measurable** | Cumple | Transición de estado registrada e historial de patentes en conflicto guardado. |
| **Achievable** | Pendiente | Pendiente de validación por el equipo de desarrollo. |
| **Relevant** | Cumple | Procesa la respuesta de negocio proveniente de la integración. |
| **Time-boxed** | Cumple | Evaluado al recibir el callback/respuesta del análisis. |

#### Cuestionario Atómico (5 Preguntas)
* **¿Endpoint/Método?:** Sí (Integración / Callback posterior a `POST /formulas/`{id}/evaluar-patente)
* **¿Campos/Payload?:** Sí (Notificación de conflicto con patentes específicas)
* **¿Código HTTP/Error?:** Sí (Manejo de estado de dominio RECHAZADA_POR_INFRACCION)
* **¿Restricciones/Reglas?:** Sí (Registro obligatorio de patentes en conflicto en auditoría)
* **¿Condición Atómica?:** Sí (Evalúa el procesamiento del rechazo legal)

## Veredicto Final de la HU
**APROBADA**

* **Justificación del Veredicto:** Excelente nivel de especificación de requisitos en un entorno SOA. Cumple al 100% con INVEST, SMART y el cuestionario atómico. Diferencia con total precisión los fallos de lógica de negocio (CA-02, CA-05) de los fallos de infraestructura e integración inter-servicio (CA-04 con HTTP 503 y rollback de estado).
* **Acción Requerida / Sugerencia:** Ninguna. Es una HU modelo para la integración SOA.

---

### Tabla Resumen Final del Backlog

| ID HU | Nombre Épico | Total CAs | Veredicto Final | Principal Hallazgo / Observación |
| :--- | :--- | :---: | :--- | :--- |
| **HU-01** | Crear materia | 4 | **APROBADA CON OBSERVACIONES** | Omite detallar explícitamente los campos del JSON en la redacción del CA-01 (falla menor en Specific). Matriz HTTP y criterios INVEST/SMART correctos. |
| **HU-02** | Consultar material | 4 | **APROBADA** | Cobertura BDD y matriz de errores completa (HTTP 200, 400, 404, 500/503). |
| **HU-03** | Actualizar material | 4 | **APROBADA** | Excelente definición de reglas transaccionales (rollback en HTTP 500) e integridad de datos. |
| **HU-04** | Eliminar/Inhabilitar material | 4 | **APROBADA** | Correcta implementación de Soft Delete y manejo de conflicto por dependencia en fórmulas (HTTP 409). |
| **HU-05** | Crear Fórmula | 5 | **APROBADA** | Inclusión acertada de la regla de negocio aritmética (100%) y código HTTP 422 para materiales inactivos/inexistentes. |
| **HU-06** | Consultar Fórmula | 4 | **APROBADA** | Estructura impecable para dar soporte al consumo SOA por los Equipos 2 (Patentes) y 3 (Fabricación). |
| **HU-07** | Actualizar Fórmula | 5 | **APROBADA** | Especificación sobresaliente del bloqueo de inmutabilidad (HTTP 409) para fórmulas fuera de estado BORRADOR. |
| **HU-08** | Eliminar Fórmula | 4 | **APROBADA** | Incorpora regla de negocio de trazabilidad (justificación obligatoria para descontinuar). |
| **HU-09** | Evaluar Patente | 5 | **APROBADA** | Especificación de referencia SOA. Separa fallos de negocio de errores de integración (HTTP 503) con rollback de estado. |
