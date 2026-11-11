# Prompts y salida — Product Backlog Equipo 1 (Materiales/Fórmulas)
**Curso:** 750021C Desarrollo de software II — Caso Cutit Saws Ltd.

Este documento reúne el prompt generador, el prompt validador y las salidas producidas por ambos.

---

## 1. Prompt generador

```xml
<system_prompt>

Actúas como un Analista de Requisitos e Ingeniero de Software Senior, especializado en arquitecturas orientadas a servicios (SOA) y diseño de APIs RESTful. Tu marco normativo abarca ISO/IEC/IEEE 29148:2018, el modelo de Mike Cohn, los criterios INVEST y la metodología SMART. Eres técnicamente riguroso, preciso en contratos de API y evitas la inflación innecesaria del backlog.

<problem_statement>

Cutit Saws Ltd. fabrica motosierras hidráulicas de diamante. Para resolver cuellos de botella e incompatibilidad en su hub homegrown, la dirección definió construir una plataforma backend modular de 5 APIs REST independientes (Materiales/Fórmulas, Patent Sweep, Fabricación, Inventario, Notificaciones). Alcance exclusivo: Backend e integración SOA (sin UI, sin pagos, sin autenticación compleja).

Dominio de la API (Equipo 1 - Materiales/Fórmulas):

* Responsabilidad: Gestión centralizada del catálogo de materiales e insumos técnicos, estructuración de recetas/fórmulas de fabricación y evaluación de riesgos de propiedad intelectual.
* Endpoints mínimos (Tabla Formal de APIs, Sección 11): POST /materiales, GET /materiales/{id}.
* Interconexión SOA:
  * CONSUME de: API de Patent Sweep (Equipo 2) vía cliente HTTP para validación cruzada de riesgo de infracción.
  * ES CONSUMIDA por: API de Fabricación (Equipo 3) para validar lotes de producción contra fórmulas registradas.

Roles del Sistema:

* Técnico / Ingeniero de Materiales (Registra y consulta insumos y fórmulas).
* Analista de Cumplimiento / Patentes (Evalúa el riesgo legal/técnico).
* API de Fabricación (Consumidor del sistema a través de contratos de integración).

</problem_statement>

<epics_scope>

El backlog de esta célula debe estructurarse a partir de las siguientes 3 Historias Épicas de dominio:

1. EP-01: Gestión del Catálogo de Materiales e Insumos
   * Definición: "Como Ingeniero de Materiales, quiero registrar, actualizar y consultar los materiales utilizados en la fabricación de cuchillas, para mantener un catálogo centralizado y confiable de insumos con sus propiedades técnicas."
   * Integración SOA: Expone el catálogo base que será consultado para la estructuración de fórmulas y disponibilidad.
2. EP-02: Gestión de Fórmulas y Recetas de Fabricación
   * Definición: "Como Ingeniero de Materiales, quiero registrar y consultar las fórmulas de fabricación estructurando los materiales y sus proporciones exactas, para garantizar la trazabilidad y estandarización de las recetas de producción."
   * Integración SOA: Consume los materiales validados en EP-01 y expone las fórmulas aprobadas para que la API de Fabricación valide órdenes de producción.
3. EP-03: Verificación de Riesgo de Patentes en Materiales y Fórmulas
   * Definición: "Como Analista de Cumplimiento, quiero verificar el estado legal y de patentes asociado a los materiales y fórmulas registrados, para anticipar riesgos de infracción de propiedad intelectual antes de pasar a fabricación."
   * Integración SOA: Consume de forma síncrona/asíncrona la API de Patent Sweep (Equipo 2) enviando los componentes de la fórmula para recibir el dictamen de riesgo.

</epics_scope>

<generation_rules>

Una vez aprobada la fase previa por el usuario:

* Descompón las 3 Épicas en un total de 4 a 7 Historias de Usuario (HUs) pequeñas, independientes y BDD.
* Asigna cada HU a su Épica de origen (EP-01, EP-02 o EP-03).
* Para cada HU, genera de 2 a 4 Criterios de Aceptación en formato Dado/Cuando/Entonces.
* Cobertura Obligatoria en CA: Toda HU debe incluir al menos un CA de flujo de éxito (camino feliz, código HTTP 200/201) y al menos un CA de flujo de excepción o falla (error de negocio 400/409 o falla técnica 500/503).
* Diferenciación de Errores: Separa los mensajes de error de validación de datos de negocio de las fallas de integración SOA entre APIs.

</generation_rules>

<anti_duplication_rule>

No generes HUs redundantes para diferentes roles si realizan la misma operación técnica sobre el contrato de la API. Consolida el objetivo en una sola HU parametrizada.

</anti_duplication_rule>

<limits_and_constraints>

* No inventes campos técnicos ni contratos JSON sin sustentarlo en el texto o declararlo explícitamente como "Supuesto asumido".
* No generes endpoints ni reglas para las APIs de los Equipos 2, 3, 4 o 5; solo menciónalas como puntos de integración externa.
* No evalúes de forma definitiva "Estimable" ni "Achievable".

</limits_and_constraints>

<output_format>

Estructura final de la entrega (tras superar el Paso 0):

1. Lista numerada de HUs (ID | Épica Origen | Cita Fuente | Cuestionario INVEST | HUs con formato Cohn | Criterios de Aceptación BDD | Supuestos Asumidos).
2. Tabla Resumen Final del Backlog (ID | Épica Origen | Nombre de la HU | # CA Éxito | # CA Falla | Supuestos clave).

</output_format>
```

---

## 2. Prompt validador

```xml
<system_prompt>

Actúas como un Auditor de Requisitos Senior e Ingeniero de Calidad de Software (QA), completamente independiente de la célula que generó el backlog. Tu función es auditar con imparcialidad técnica cada Historia de Usuario (HU) y Criterio de Aceptación (CA) bajo las normas ISO/IEC/IEEE 29148:2018, INVEST y SMART. Evalúas el documento suministrado como si fuera un artefacto externo, sin autocomplacencia y exigiendo máxima rigurosidad técnica.

<audit_framework>

1. Evaluación de Historias de Usuario (HU):
   - Criterios INVEST (Independent, Negotiable, Valuable, Estimable, Small, Testable).
   - Regla de Estimación: La dimensión "Estimable" DEBE estar explícitamente marcada como "Pendiente de validación por el equipo". Si la IA o el autor se autoadjudicó un puntaje de estimación, se considera una falla grave.
   - Trazabilidad: Verificación de la cita textual directa del documento fuente (Caso Cutit Saws).

2. Evaluación de Criterios de Aceptación (CA):
   - Criterios SMART (Specific, Measurable, Achievable, Relevant, Time-boxed).
   - Regla de Factibilidad: La dimensión "Achievable" DEBE estar explícitamente marcada como "Pendiente de validación por el equipo".
   - Cuestionario Obligatorio de 5 Preguntas por cada CA:
     * ¿Indica en qué endpoint/evento ocurre la acción?
     * ¿Describe los campos, datos y payload relevantes?
     * ¿Describe validaciones, códigos de estado HTTP y mensajes de error?
     * ¿Describe restricciones técnicas o de negocio si aplican?
     * ¿Representa una única condición atómica y verificable?

3. Cobertura de Flujos y Errores SOA:
   - Verificación de existencia de al menos un CA de flujo de éxito (camino feliz, 200/201) y al menos un CA de excepción (error de negocio 400/409 o falla técnica 500/503).
   - Verificación de diferenciación explícita entre fallos de negocio y fallos de integración SOA.

</audit_framework>

<problem_statement>

Contexto de la Auditoría (Cutit Saws Ltd. - Equipo 1 Materiales/Fórmulas):

* Backend e integración SOA de 5 APIs REST independientes.
* Alcance del Equipo 1: Gestión de materiales, recetas/fórmulas y validación de riesgo de patentes.
* Endpoints Mínimos: POST /materiales, GET /materiales/{id}.
* Integración Externa: Consume de Patent Sweep (Equipo 2) y es consumida por Fabricación (Equipo 3).

</problem_statement>

<evaluation_thresholds>

Aplica de forma estricta los siguientes umbrales cuantitativos para determinar el veredicto final de cada HU:

* APROBADA:
  * Cumple el 100% de INVEST (Estimable correctamente marcado como "Pendiente").
  * Todos sus CAs cumplen SMART (Achievable correctamente marcado como "Pendiente") y responden afirmativamente al cuestionario de 5 preguntas.
  * Posee cita fuente textual válida y cobertura completa de éxito y falla.
* APROBADA CON OBSERVACIONES:
  * Presenta máximo 1 falla menor en INVEST (excluyendo "Testable" e "Independent", las cuales son críticas).
  * O presenta hasta 2 CAs con 1 falla menor en SMART o en alguna pregunta del cuestionario.
* RECHAZADA:
  * Falla en las dimensiones críticas "Testable" e "Independent".
  * Carece de cita de fuente textual del caso.
  * Le falta el CA de flujo de éxito o el CA de flujo de falla.
  * El autor/documento se autoadjudicó veredicto en "Estimable" o "Achievable".
  * 2 o más CAs fallan en "Specific" o "Measurable".

</evaluation_thresholds>

<limits_and_constraints>

* Prohibido hacer opiniones generales o abstractas ("El texto se ve bien"); toda observación debe estar vinculada a un criterio INVEST, SMART o del cuestionario.
* No alteres ni relajes los umbrales de veredicto por autocomplacencia.

</limits_and_constraints>

<output_format>

Estructura de la auditoría por cada Historia de Usuario:

1. Matriz INVEST de la HU (Tabla de 6 filas: Criterio | Estado: Cumple/Falla/Pendiente | Justificación).
2. Evaluación de Criterios de Aceptación (Por cada CA: Tabla SMART de 5 filas + Respuestas al Cuestionario de 5 Preguntas).
3. Veredicto Final de la HU (Aprobada / Aprobada con observaciones / Rechazada) con su justificación basada en los umbrales.
4. Sugerencia puntual de ajuste (solo si aplica).
5. Tabla Resumen Final del Backlog (ID HU | Veredicto | Principal Hallazgo / Acción Requerida).

</output_format>
```

---

## 3. Salida del generador — Product Backlog

### HU-01 | Registro de Insumos y Materiales Técnicos

* **Épica Origen:** EP-01 (Gestión del Catálogo de Materiales e Insumos)
* **Cita Fuente:** *"Como Ingeniero de Materiales, quiero registrar... los materiales utilizados en la fabricación de cuchillas, para mantener un catálogo centralizado..."*
* **Cuestionario INVEST:**
  * **Independent:** Sí. No depende de fórmulas ni de la integración con Patent Sweep para registrar un insumo base.
  * **Negotiable:** Sí. El esquema exacto de propiedades técnicas opcionales se ajusta según el insumo.
  * **Valuable:** Sí. Permite centralizar la materia prima antes de construir cualquier receta de fabricación.
  * **Small:** Sí. Se acota exclusivamente al alta y persistencia técnica del insumo.
  * **Testable:** Sí. Verificable mediante códigos de respuesta HTTP y estados de persistencia.
* **Formato Mike Cohn:**
  **Como** Técnico / Ingeniero de Materiales
  **Quiero** registrar nuevos materiales e insumos técnicos con sus propiedades físicas y químicas en el sistema
  **Para** disponer de un catálogo centralizado y confiable que alimente las fórmulas de fabricación.

**Criterios de Aceptación (BDD)**

```gherkin
Escenario: Registro exitoso de un nuevo material técnico (Camino Feliz)
  Dado que el Ingeniero de Materiales envía una solicitud POST a "/materiales"
  Y el cuerpo de la petición contiene un nombre único ("Polvo de Diamante Grado Industrial") y propiedades válidas
  Cuando la API procesa la solicitud
  Entonces la API debe responder con un código de estado HTTP 201 Created
  Y debe retornar el objeto del material registrado con un "id" generado y la fecha de creación.

Escenario: Intento de registro con datos requeridos faltantes o inválidos (Error de Negocio)
  Dado que el Ingeniero de Materiales envía una solicitud POST a "/materiales"
  Y el cuerpo de la petición omite el nombre del material o envía un valor nulo en las propiedades técnicas requeridas
  Cuando la API valida el contrato de entrada
  Entonces la API debe retornar un código de estado HTTP 400 Bad Request
  Y el cuerpo de la respuesta debe incluir un mensaje de error detallando los campos faltantes o inválidos.

Escenario: Duplicidad de material en el catálogo (Error de Negocio)
  Dado que el material con el código técnico "MAT-DIA-001" ya existe en el sistema
  Cuando el Ingeniero de Materiales envía una solicitud POST a "/materiales" intentando registrar el mismo código
  Entonces la API debe responder con un código de estado HTTP 409 Conflict
  Y el mensaje de error debe indicar "El material con el código especificado ya existe en el catálogo".
```

**Supuestos Asumidos:** Se asume que cada material cuenta con una clave técnica o código identificador único definido por el equipo de ingeniería.

---

### HU-02 | Consulta de Catálogo y Detalle de Materiales

* **Épica Origen:** EP-01 (Gestión del Catálogo de Materiales e Insumos)
* **Cita Fuente:** *"Como Ingeniero de Materiales, quiero... consultar los materiales utilizados en la fabricación de cuchillas, para mantener un catálogo centralizado y confiable de insumos con sus propiedades técnicas."*
* **Cuestionario INVEST:**
  * **Independent:** Sí. La lectura no altera el estado de la base de datos ni de otros servicios.
  * **Negotiable:** Sí. Los parámetros de filtrado y paginación son ajustables.
  * **Valuable:** Sí. Permite a los ingenieros y a otros módulos validar los insumos existentes.
  * **Small:** Sí. Centrada en la recuperación de lectura por ID o listado.
  * **Testable:** Sí. Verificable mediante payload de respuesta y códigos 200/404.
* **Formato Mike Cohn:**
  **Como** Técnico / Ingeniero de Materiales
  **Quiero** consultar la información técnica detallada de un material mediante su identificador
  **Para** verificar sus especificaciones técnicas antes de incluirlo en una fórmula de producción.

**Criterios de Aceptación (BDD)**

```gherkin
Escenario: Búsqueda exitosa de un material por identificador (Camino Feliz)
  Dado que existe un material registrado con el ID "mat-883" en el sistema
  Cuando el usuario envía una solicitud GET a "/materiales/mat-883"
  Entonces la API debe responder con un código de estado HTTP 200 OK
  Y el cuerpo JSON debe contener la información del material, incluyendo nombre, código e identificadores técnicos.

Escenario: Búsqueda de un material inexistente (Error de Negocio)
  Dado que el identificador "mat-999" no está registrado en la base de datos
  Cuando el usuario envía una solicitud GET a "/materiales/mat-999"
  Entonces la API debe retornar un código de estado HTTP 404 Not Found
  Y un mensaje de error indicando "El material solicitado no fue encontrado".
```

**Supuestos Asumidos:** No se requieren permisos de autenticación especiales para la lectura dentro del backend SOA (según el alcance definido).

---

### HU-03 | Estructuración y Registro de Fórmulas de Fabricación

* **Épica Origen:** EP-02 (Gestión de Fórmulas y Recetas de Fabricación)
* **Cita Fuente:** *"Como Ingeniero de Materiales, quiero registrar y consultar las fórmulas de fabricación estructurando los materiales y sus proporciones exactas..."*
* **Cuestionario INVEST:**
  * **Independent:** Depende del registro previo de materiales (HU-01), pero su desarrollo técnico es independiente.
  * **Negotiable:** Sí. La estructura de porcentajes o pesos es parametrizable.
  * **Valuable:** Sí. Es la base técnica para la producción y validación de órdenes en la API de Fabricación.
  * **Small:** Sí. Acotada al alta y validación de integridad de las proporciones.
  * **Testable:** Sí. Evaluando sumatoria de proporciones y la validez de los componentes.
* **Formato Mike Cohn:**
  **Como** Técnico / Ingeniero de Materiales
  **Quiero** estructurar y registrar una nueva fórmula de fabricación asociando materiales registrados y sus proporciones
  **Para** garantizar la estandarización técnica de las recetas de producción.

**Criterios de Aceptación (BDD)**

```gherkin
Escenario: Registro exitoso de una fórmula de fabricación (Camino Feliz)
  Dado que el Ingeniero de Materiales envía una solicitud POST a "/formulas"
  Y la solicitud incluye una lista de IDs de materiales válidos existentes y proporciones que suman el 100%
  Cuando la API valida los insumos y guarda la fórmula
  Entonces la API debe retornar un código de estado HTTP 201 Created
  Y el payload debe devolver el ID de la fórmula registrada y su estado inicial "En Borrador".

Escenario: Registro de fórmula con materiales no existentes (Error de Negocio)
  Dado que la solicitud POST a "/formulas" incluye el ID de material "mat-000" que no existe en el catálogo
  Cuando la API verifica la existencia de los componentes
  Entonces la API debe responder con un código de estado HTTP 400 Bad Request
  Y el mensaje de error debe indicar "Uno o más materiales especificados no existen en el catálogo centralizado".

Escenario: Proporciones de materiales no equivalentes al 100% (Error de Negocio)
  Dado que la suma de las proporciones de los materiales enviados en la fórmula es de 85%
  Cuando la API realiza la validación matemática de la receta
  Entonces la API debe responder con un código de estado HTTP 422 Unprocessable Entity
  Y el cuerpo de la respuesta debe especificar que las proporciones de la fórmula deben sumar exactamente 100%.
```

**Supuestos Asumidos:** Se asume que las fórmulas se componen de porcentajes que deben sumar 100% o unidades absolutas según la unidad de medida registrada en el material.

---

### HU-04 | Consulta de Fórmulas para Consumo SOA de Fabricación

* **Épica Origen:** EP-02 (Gestión de Fórmulas y Recetas de Fabricación)
* **Cita Fuente:** *"...y expone las fórmulas aprobadas para que la API de Fabricación valide órdenes de producción."*
* **Cuestionario INVEST:**
  * **Independent:** Sí. Exposición de contrato de lectura para un sistema externo consumidor.
  * **Negotiable:** Sí. Los campos retornados pueden omitir datos legales internos no requeridos para producción.
  * **Valuable:** Sí. Es el punto de integración clave para el flujo de fabricación (Equipo 3).
  * **Small:** Sí. Endpoint de lectura optimizado por ID de fórmula.
  * **Testable:** Sí. Verificable mediante llamada cliente HTTP desde el contexto de la API de Fabricación.
* **Formato Mike Cohn:**
  **Como** API de Fabricación (Consumidor del sistema)
  **Quiero** consultar las fórmulas aprobadas y su desglose de materiales vía contrato REST
  **Para** validar si un lote de producción cumple con la receta registrada antes de iniciar el proceso.

**Criterios de Aceptación (BDD)**

```gherkin
Escenario: Consulta exitosa de fórmula aprobada por la API de Fabricación (Camino Feliz)
  Dado que la API de Fabricación realiza una petición GET a "/formulas/{id}"
  Y la fórmula con dicho ID existe y se encuentra en estado "Aprobada"
  Cuando la API procesa la solicitud
  Entonces la API debe responder con un código de estado HTTP 200 OK
  Y el JSON de respuesta debe detallar los insumos, proporciones y tolerancia técnica necesaria para la producción.

Escenario: Consulta de fórmula no aprobada o archivada (Error de Negocio)
  Dado que la fórmula "form-102" existe pero está en estado "Rechazada" o "Inactiva"
  Cuando la API de Fabricación envía una solicitud GET a "/formulas/form-102"
  Entonces la API debe responder con un código de estado HTTP 409 Conflict o 400 Bad Request
  Y el mensaje de error debe indicar "La fórmula solicitada no está habilitada para procesos de producción".
```

**Supuestos Asumidos:** La API de Fabricación requiere explícitamente conocer el estado de aprobación de la fórmula dentro del payload.

---

### HU-05 | Evaluación de Riesgo de Propiedad Intelectual vía Patent Sweep

* **Épica Origen:** EP-03 (Verificación de Riesgo de Patentes en Materiales y Fórmulas)
* **Cita Fuente:** *"Como Analista de Cumplimiento, quiero verificar el estado legal y de patentes... Consume de forma síncrona/asíncrona la API de Patent Sweep (Equipo 2)..."*
* **Cuestionario INVEST:**
  * **Independent:** Depende funcionalmente de la existencia de una fórmula o lista de componentes, pero el contrato de integración es independiente.
  * **Negotiable:** Sí. La estrategia de resiliencia (circuit breaker/retry) puede afinarse según la latencia de la API externa.
  * **Valuable:** Sí. Mitiga riesgos legales por infracción de patentes en materiales o mezclas.
  * **Small:** Sí. Centrado en la comunicación HTTP de cliente hacia la API de Patent Sweep.
  * **Testable:** Sí. Mediante dobles de prueba (mocks) del servicio externo y simulación de fallas de red.
* **Formato Mike Cohn:**
  **Como** Analista de Cumplimiento / Patentes
  **Quiero** solicitar la verificación de riesgo de patentes para una fórmula o material enviando sus componentes a la API de Patent Sweep
  **Para** obtener un dictamen que indique si la mezcla infringe derechos de propiedad intelectual.

**Criterios de Aceptación (BDD)**

```gherkin
Escenario: Verificación síncrona exitosa de riesgo de patentes (Camino Feliz)
  Dado que el Analista de Cumplimiento solicita la verificación de la fórmula "form-501" mediante POST a "/formulas/form-501/evaluar-patente"
  Y la API del Equipo 2 (Patent Sweep) está disponible y responde con un dictamen de riesgo "Bajo"
  Cuando la API de Materiales procesa la respuesta externa
  Entonces la API debe responder con un código HTTP 200 OK
  Y debe guardar el dictamen de riesgo y el identificador de análisis en el registro de la fórmula.

Escenario: Fallo técnico de integración SOA por indisponibilidad de Patent Sweep (Falla Técnica SOA)
  Dado que el Analista de Cumplimiento solicita la evaluación de patentes para una fórmula
  Cuando la API de Materiales invoca el cliente HTTP de la API de Patent Sweep y esta responde con error 503 Service Unavailable o expira por timeout
  Entonces la API de Materiales debe manejar la falla sin detener el sistema
  Y debe responder con un código de estado HTTP 502 Bad Gateway
  Y el mensaje de error debe indicar "No fue posible conectar con el servicio de Patent Sweep (Equipo 2). Reintente más tarde".

Escenario: Componentes de fórmula ambiguos para el análisis legal (Error de Negocio)
  Dado que la API de Patent Sweep retorna un código 400 Bad Request debido a especificaciones técnicas incompletas en la fórmula
  Cuando la API de Materiales recibe dicha respuesta
  Entonces la API debe retornar un código HTTP 422 Unprocessable Entity
  Y notificar al usuario que los datos del material no son suficientes para llevar a cabo el análisis legal.
```

**Supuestos Asumidos:** La integración entre la API de Materiales y la API de Patent Sweep se realiza mediante peticiones HTTP/REST síncronas con reintentos configurados.

---

### HU-06 | Actualización del Estado de Aprobación Legal de la Fórmula

* **Épica Origen:** EP-03 (Verificación de Riesgo de Patentes en Materiales y Fórmulas)
* **Cita Fuente:** *"Como Analista de Cumplimiento, quiero verificar el estado legal y de patentes asociado a los materiales y fórmulas registrados, para anticipar riesgos de infracción..."*
* **Cuestionario INVEST:**
  * **Independent:** Sí. Modifica el ciclo de vida de la fórmula según el análisis legal previo.
  * **Negotiable:** Sí. Las transiciones de estado válidas (Aprobado, Bloqueado, En Revisión) son parametrizables.
  * **Valuable:** Sí. Bloquea el paso a fabricación de fórmulas con dictamen de riesgo alto.
  * **Small:** Sí. Operación de actualización de estado puntual.
  * **Testable:** Sí. Verificable cambiando los estados mediante un PATCH/PUT y validando la regla de negocio.
* **Formato Mike Cohn:**
  **Como** Analista de Cumplimiento / Patentes
  **Quiero** actualizar el estado legal de una fórmula (Aprobada / Rechazada por Infracción)
  **Para** evitar que la API de Fabricación consuma fórmulas con riesgo legal.

**Criterios de Aceptación (BDD)**

```gherkin
Escenario: Aprobación legal exitosa de una fórmula (Camino Feliz)
  Dado que la fórmula "form-501" tiene un dictamen de patentes sin infracciones
  Cuando el Analista de Cumplimiento envía una solicitud PATCH a "/formulas/form-501/estado-legal" con el estado "APROBADO_LEGAL"
  Entonces la API debe responder con un código HTTP 200 OK
  Y el estado de la fórmula debe cambiar a "Lista para Producción".

Escenario: Intento de aprobación de una fórmula con conflicto de patentes activo (Error de Negocio)
  Dado que la fórmula "form-502" tiene un dictamen de riesgo de patentes "INFRACCION_DETECTADA"
  Cuando el Analista intenta cambiar el estado a "APROBADO_LEGAL"
  Entonces la API debe rechazar la solicitud con un código HTTP 409 Conflict
  Y el mensaje debe indicar "No se puede aprobar una fórmula con infracciones de patente activas".
```

**Supuestos Asumidos:** Transición de estados mediante un patrón de máquina de estados en el atributo de estado de la fórmula.

---

### Tabla Resumen del Backlog

| ID | Épica Origen | Nombre de la HU | # CA Éxito | # CA Falla | Supuestos clave |
| :---- | :---- | :---- | :---- | :---- | :---- |
| **HU-01** | EP-01 | Registro de Insumos y Materiales Técnicos | 1 | 2 | Clave o código técnico único por material. |
| **HU-02** | EP-01 | Consulta de Catálogo y Detalle de Materiales | 1 | 1 | Lectura directa sin filtros de seguridad complejos en backend. |
| **HU-03** | EP-02 | Estructuración y Registro de Fórmulas de Fabricación | 1 | 2 | Las proporciones de los materiales deben sumar 100%. |
| **HU-04** | EP-02 | Consulta de Fórmulas para Consumo SOA de Fabricación | 1 | 1 | La API de Fabricación requiere validar el estado de aprobación. |
| **HU-05** | EP-03 | Evaluación de Riesgo de IP vía Patent Sweep | 1 | 2 | Integración HTTP síncrona con el servicio del Equipo 2. |
| **HU-06** | EP-03 | Actualización del Estado de Aprobación Legal | 1 | 1 | Control de estados mediante reglas de negocio predefinidas. |

---

## 4. Salida del validador — Informe de Auditoría de Calidad y Requisitos de Software

Auditoría sobre el backlog entregado para la API de Materiales/Fórmulas (Equipo 1) de Cutit Saws Ltd.

### HU-01 | Registro de Insumos y Materiales Técnicos

#### 1. Matriz Criterios INVEST

| Criterio | Estado | Justificación |
| :---- | :---- | :---- |
| **Independent** | **Cumple** | No requiere de fórmulas (EP-02) ni de la integración con Patent Sweep (EP-03) para persistir un material base. |
| **Negotiable** | **Cumple** | Permite flexibilidad en las propiedades técnicas físicas/químicas según la categoría del insumo. |
| **Valuable** | **Cumple** | Aporta valor de negocio directo al centralizar el catálogo técnico de insumos. |
| **Estimable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Small** | **Cumple** | Acotada exclusivamente al alta y validación del contrato de entrada de un material. |
| **Testable** | **Cumple** | Verificable mediante códigos de respuesta HTTP (201, 400, 409) e insumos persistidos. |

* **Trazabilidad:** Cita fuente válida (*"Como Ingeniero de Materiales, quiero registrar... los materiales utilizados en la fabricación de cuchillas..."*).

#### 2. Evaluación de Criterios de Aceptación (SMART + Cuestionario QA)

##### CA-01.1: Registro exitoso de un nuevo material técnico (Camino Feliz)

| Dimensión | Estado | Justificación |
| :---- | :---- | :---- |
| **Specific** | **Falla** | No especifica la estructura mínima del payload de solicitud ni el campo obligatorio que genera la colisión técnica. |
| **Measurable** | **Cumple** | Retorna código HTTP 201 Created y campos de salida "id" y fecha. |
| **Achievable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Relevant** | **Cumple** | Valida el flujo principal de alta en el catálogo. |
| **Time-boxed** | **Cumple** | Operación síncrona evaluable en el alcance del sprint. |

* **Cuestionario de 5 Preguntas:**
  1. *¿Indica en qué endpoint/evento ocurre la acción?* **Sí** (POST /materiales).
  2. *¿Describe los campos, datos y payload relevantes?* **No** (Omite los campos requeridos del cuerpo, ej. codigoTecnico, nombre, unidadMedida).
  3. *¿Describe validaciones, códigos de estado HTTP y mensajes de error?* **Sí** (HTTP 201 Created).
  4. *¿Describe restricciones técnicas o de negocio si aplican?* **Parcial** (Menciona "nombre único" pero no define la regla en la clave técnica).
  5. *¿Representa una única condición atómica y verificable?* **Sí**.

##### CA-01.2: Intento de registro con datos requeridos faltantes o inválidos (Error de Negocio)

| Dimensión | Estado | Justificación |
| :---- | :---- | :---- |
| **Specific** | **Cumple** | Detalla la condición de falla por omisión de datos obligatorios o valores nulos. |
| **Measurable** | **Cumple** | Retorna HTTP 400 Bad Request y un array/cuerpo de errores de validación. |
| **Achievable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Relevant** | **Cumple** | Asegura la integridad de los datos antes de persistir. |
| **Time-boxed** | **Cumple** | Ejecución síncrona. |

* **Cuestionario de 5 Preguntas:**
  1. *¿Indica en qué endpoint/evento ocurre la acción?* **Sí** (POST /materiales).
  2. *¿Describe los campos, datos y payload relevantes?* **Sí** (Identifica ausencia de nombre o nulos en propiedades obligatorias).
  3. *¿Describe validaciones, códigos de estado HTTP y mensajes de error?* **Sí** (HTTP 400 Bad Request con detalle de campos).
  4. *¿Describe restricciones técnicas o de negocio si aplican?* **Sí** (Validación de contrato OpenAPI).
  5. *¿Representa una única condición atómica y verificable?* **Sí**.

##### CA-01.3: Duplicidad de material en el catálogo (Error de Negocio)

| Dimensión | Estado | Justificación |
| :---- | :---- | :---- |
| **Specific** | **Cumple** | Define la condición de duplicidad basada en la clave/código técnico "MAT-DIA-001". |
| **Measurable** | **Cumple** | Retorna HTTP 409 Conflict y mensaje de error específico. |
| **Achievable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Relevant** | **Cumple** | Previene la duplicación de insumos en la base de datos. |
| **Time-boxed** | **Cumple** | Ejecución síncrona. |

* **Cuestionario de 5 Preguntas:**
  1. *¿Indica en qué endpoint/evento ocurre la acción?* **Sí** (POST /materiales).
  2. *¿Describe los campos, datos y payload relevantes?* **Sí** (Menciona la clave técnica "MAT-DIA-001").
  3. *¿Describe validaciones, códigos de estado HTTP y mensajes de error?* **Sí** (HTTP 409 Conflict y mensaje "El material con el código especificado ya existe en el catálogo").
  4. *¿Describe restricciones técnicas o de negocio si aplican?* **Sí** (Unicidad de código técnico).
  5. *¿Representa una única condición atómica y verificable?* **Sí**.

#### 3. Veredicto Final de la HU-01

**APROBADA CON OBSERVACIONES**

* **Justificación:** Cumple con INVEST e ISO/IEC/IEEE 29148:2018. "Estimable" y "Achievable" están marcados correctamente como "Pendiente". Presenta solo 1 falla menor de especificación en el payload del CA-01.1.
* **Sugerencia de Ajuste:** En CA-01.1, especificar los campos obligatorios del payload:
  `Y el cuerpo de la petición contiene "codigoTecnico": "MAT-DIA-001", "nombre": "Polvo de Diamante" y "unidadMedida": "GRAMOS"`

---

### HU-02 | Consulta de Catálogo y Detalle de Materiales

#### 1. Matriz Criterios INVEST

| Criterio | Estado | Justificación |
| :---- | :---- | :---- |
| **Independent** | **Cumple** | Operación de lectura (GET) desacoplada de la lógica de escritura o de otros módulos. |
| **Negotiable** | **Cumple** | El esquema de respuesta en el detalle es parametrizable. |
| **Valuable** | **Cumple** | Permite a los usuarios y sistemas externos consultar especificaciones de materiales. |
| **Estimable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Small** | **Cumple** | Centrada únicamente en la consulta por identificador (GET /materiales/{id}). |
| **Testable** | **Cumple** | Evaluable a través de códigos HTTP 200 OK y 404 Not Found. |

* **Trazabilidad:** Cita fuente válida (*"Como Ingeniero de Materiales, quiero... consultar los materiales utilizados en la fabricación de cuchillas..."*).

#### 2. Evaluación de Criterios de Aceptación (SMART + Cuestionario QA)

##### CA-02.1: Búsqueda exitosa de un material por identificador (Camino Feliz)

| Dimensión | Estado | Justificación |
| :---- | :---- | :---- |
| **Specific** | **Cumple** | Define el endpoint con parámetro de ruta mat-883 y los atributos esperados en el JSON. |
| **Measurable** | **Cumple** | Retorna HTTP 200 OK y la entidad correspondiente. |
| **Achievable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Relevant** | **Cumple** | Permite verificar la existencia y ficha técnica del material. |
| **Time-boxed** | **Cumple** | Operación de lectura de baja latencia. |

* **Cuestionario de 5 Preguntas:**
  1. *¿Indica en qué endpoint/evento ocurre la acción?* **Sí** (GET /materiales/mat-883).
  2. *¿Describe los campos, datos y payload relevantes?* **Sí** (Menciona nombre, código e identificadores técnicos en el JSON).
  3. *¿Describe validaciones, códigos de estado HTTP y mensajes de error?* **Sí** (HTTP 200 OK).
  4. *¿Describe restricciones técnicas o de negocio si aplican?* **Sí** (El ID debe existir en la BD).
  5. *¿Representa una única condición atómica y verificable?* **Sí**.

##### CA-02.2: Búsqueda de un material inexistente (Error de Negocio)

| Dimensión | Estado | Justificación |
| :---- | :---- | :---- |
| **Specific** | **Cumple** | Define la condición de búsqueda para un recurso inexistente (mat-999). |
| **Measurable** | **Cumple** | Retorna HTTP 404 Not Found y mensaje de error indicativo. |
| **Achievable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Relevant** | **Cumple** | Garantiza el manejo correcto de recursos no encontrados. |
| **Time-boxed** | **Cumple** | Operación síncrona. |

* **Cuestionario de 5 Preguntas:**
  1. *¿Indica en qué endpoint/evento ocurre la acción?* **Sí** (GET /materiales/mat-999).
  2. *¿Describe los campos, datos y payload relevantes?* **Sí** (Identificador inexistente).
  3. *¿Describe validaciones, códigos de estado HTTP y mensajes de error?* **Sí** (HTTP 404 Not Found con mensaje "El material solicitado no fue encontrado").
  4. *¿Describe restricciones técnicas o de negocio si aplican?* **Sí** (Recurso no registrado).
  5. *¿Representa una única condición atómica y verificable?* **Sí**.

#### 3. Veredicto Final de la HU-02

**APROBADA**

* **Justificación:** Cumple al 100% con INVEST y SMART ("Estimable" y "Achievable" en "Pendiente"). Responde positivamente a todas las preguntas del cuestionario y posee cita de fuente textual.

---

### HU-03 | Estructuración y Registro de Fórmulas de Fabricación

#### 1. Matriz Criterios INVEST

| Criterio | Estado | Justificación |
| :---- | :---- | :---- |
| **Independent** | **Cumple** | Aunque requiere referencias a materiales, su desarrollo e interfaz de contrato es independiente. |
| **Negotiable** | **Cumple** | Las unidades de medición y estructura de la receta se pueden adaptar. |
| **Valuable** | **Cumple** | Clave para estructurar las recetas de producción del negocio. |
| **Estimable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Small** | **Cumple** | Enfocada únicamente en la creación y validación matemática de la fórmula. |
| **Testable** | **Cumple** | Verificable mediante respuestas HTTP (201, 400, 422). |

* **Trazabilidad:** Cita fuente válida (*"Como Ingeniero de Materiales, quiero registrar y consultar las fórmulas de fabricación estructurando los materiales..."*).

#### 2. Evaluación de Criterios de Aceptación (SMART + Cuestionario QA)

##### CA-03.1: Registro exitoso de una fórmula de fabricación (Camino Feliz)

| Dimensión | Estado | Justificación |
| :---- | :---- | :---- |
| **Specific** | **Cumple** | Especifica el endpoint POST /formulas, validación de insumos y condición matemática del 100%. |
| **Measurable** | **Cumple** | Retorna HTTP 201 Created, ID de fórmula y estado "En Borrador". |
| **Achievable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Relevant** | **Cumple** | Permite el alta de recetas de producción válidas. |
| **Time-boxed** | **Cumple** | Operación síncrona. |

* **Cuestionario de 5 Preguntas:**
  1. *¿Indica en qué endpoint/evento ocurre la acción?* **Sí** (POST /formulas).
  2. *¿Describe los campos, datos y payload relevantes?* **Sí** (Lista de IDs de materiales, proporciones y estado inicial).
  3. *¿Describe validaciones, códigos de estado HTTP y mensajes de error?* **Sí** (HTTP 201 Created).
  4. *¿Describe restricciones técnicas o de negocio si aplican?* **Sí** (Suma de proporciones = 100%, existencia de materiales).
  5. *¿Representa una única condición atómica y verificable?* **Sí**.

##### CA-03.2: Registro de fórmula con materiales no existentes (Error de Negocio)

| Dimensión | Estado | Justificación |
| :---- | :---- | :---- |
| **Specific** | **Cumple** | Especifica la falla al incluir un identificador inexistente (mat-000). |
| **Measurable** | **Cumple** | Retorna HTTP 400 Bad Request con mensaje descriptivo. |
| **Achievable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Relevant** | **Cumple** | Evita referencias huérfanas o compuestas por insumos inválidos. |
| **Time-boxed** | **Cumple** | Operación síncrona. |

* **Cuestionario de 5 Preguntas:**
  1. *¿Indica en qué endpoint/evento ocurre la acción?* **Sí** (POST /formulas).
  2. *¿Describe los campos, datos y payload relevantes?* **Sí** (ID de material no registrado mat-000).
  3. *¿Describe validaciones, códigos de estado HTTP y mensajes de error?* **Sí** (HTTP 400 Bad Request).
  4. *¿Describe restricciones técnicas o de negocio si aplican?* **Sí** (Integridad referencial en catálogo).
  5. *¿Representa una única condición atómica y verificable?* **Sí**.

##### CA-03.3: Proporciones de materiales no equivalentes al 100% (Error de Negocio)

| Dimensión | Estado | Justificación |
| :---- | :---- | :---- |
| **Specific** | **Cumple** | Define la regla de negocio explícita con un caso del 85% de suma total. |
| **Measurable** | **Cumple** | Retorna HTTP 422 Unprocessable Entity y detalle del fallo. |
| **Achievable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Relevant** | **Cumple** | Mantiene la consistencia físico-química de la receta. |
| **Time-boxed** | **Cumple** | Operación síncrona. |

* **Cuestionario de 5 Preguntas:**
  1. *¿Indica en qué endpoint/evento ocurre la acción?* **Sí** (POST /formulas).
  2. *¿Describe los campos, datos y payload relevantes?* **Sí** (Proporciones con suma = 85%).
  3. *¿Describe validaciones, códigos de estado HTTP y mensajes de error?* **Sí** (HTTP 422 Unprocessable Entity).
  4. *¿Describe restricciones técnicas o de negocio si aplican?* **Sí** (Regla de negocio: la suma de proporciones debe dar unívocamente 100%).
  5. *¿Representa una única condición atómica y verificable?* **Sí**.

#### 3. Veredicto Final de la HU-03

**APROBADA**

* **Justificación:** Cumple al 100% con las reglas de evaluación, cobertura de camino feliz/falla de negocio y restricciones SMART/INVEST.

---

### HU-04 | Consulta de Fórmulas para Consumo SOA de Fabricación

#### 1. Matriz Criterios INVEST

| Criterio | Estado | Justificación |
| :---- | :---- | :---- |
| **Independent** | **Cumple** | Expone un contrato de lectura consumido por un sistema externo (Equipo 3). |
| **Negotiable** | **Cumple** | La estructura final del DTO para Fabricación es negociable. |
| **Valuable** | **Cumple** | Habilita la integración SOA para la validación de órdenes de producción. |
| **Estimable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Small** | **Cumple** | Centrada en la consulta por identificador (GET /formulas/{id}). |
| **Testable** | **Cumple** | Evaluable mediante códigos HTTP 200, 400 o 409. |

* **Trazabilidad:** Cita fuente válida (*"...y expone las fórmulas aprobadas para que la API de Fabricación valide órdenes de producción."*).

#### 2. Evaluación de Criterios de Aceptación (SMART + Cuestionario QA)

##### CA-04.1: Consulta exitosa de fórmula aprobada por la API de Fabricación (Camino Feliz)

| Dimensión | Estado | Justificación |
| :---- | :---- | :---- |
| **Specific** | **Cumple** | Especifica la invocación a GET /formulas/{id} sobre una fórmula en estado "Aprobada". |
| **Measurable** | **Cumple** | Retorna HTTP 200 OK y el detalle de la receta con tolerancias técnicas. |
| **Achievable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Relevant** | **Cumple** | Asegura que Fabricación solo consuma recetas aptas. |
| **Time-boxed** | **Cumple** | Operación síncrona. |

* **Cuestionario de 5 Preguntas:**
  1. *¿Indica en qué endpoint/evento ocurre la acción?* **Sí** (GET /formulas/{id}).
  2. *¿Describe los campos, datos y payload relevantes?* **Sí** (Insumos, proporciones y tolerancias técnicas).
  3. *¿Describe validaciones, códigos de estado HTTP y mensajes de error?* **Sí** (HTTP 200 OK).
  4. *¿Describe restricciones técnicas o de negocio si aplican?* **Sí** (La fórmula debe estar en estado "Aprobada").
  5. *¿Representa una única condición atómica y verificable?* **Sí**.

##### CA-04.2: Consulta de fórmula no aprobada o archivada (Error de Negocio)

| Dimensión | Estado | Justificación |
| :---- | :---- | :---- |
| **Specific** | **Falla** | Presenta ambigüedad al indicar dos códigos HTTP posibles ("409 Conflict o 400 Bad Request"). |
| **Measurable** | **Parcial** | Al ser ambiguo el código de respuesta, la prueba automatizada no tiene una aserción única. |
| **Achievable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Relevant** | **Cumple** | Evita la fabricación de productos con fórmulas inactivas o rechazadas. |
| **Time-boxed** | **Cumple** | Operación síncrona. |

* **Cuestionario de 5 Preguntas:**
  1. *¿Indica en qué endpoint/evento ocurre la acción?* **Sí** (GET /formulas/form-102).
  2. *¿Describe los campos, datos y payload relevantes?* **Sí** (Fórmula form-102 en estado no válido).
  3. *¿Describe validaciones, códigos de estado HTTP y mensajes de error?* **No** (Indica ambiguamente "409 Conflict o 400 Bad Request").
  4. *¿Describe restricciones técnicas o de negocio si aplican?* **Sí** (Rechazo de fórmulas no habilitadas).
  5. *¿Representa una única condición atómica y verificable?* **No** (Doble alternativa de código HTTP).

#### 3. Veredicto Final de la HU-04

**APROBADA CON OBSERVACIONES**

* **Justificación:** Cumple con INVEST e ISO/IEC/IEEE 29148:2018. Presenta 1 falla menor de especificación por ambigüedad de código HTTP en el CA-04.2.
* **Sugerencia de Ajuste:** Definir un único código de estado HTTP en CA-04.2 para evitar ambigüedades en las pruebas automatizadas:
  `Entonces la API debe responder con un código de estado HTTP 409 Conflict`

---

### HU-05 | Evaluación de Riesgo de Propiedad Intelectual vía Patent Sweep

#### 1. Matriz Criterios INVEST

| Criterio | Estado | Justificación |
| :---- | :---- | :---- |
| **Independent** | **Cumple** | Contrato de integración enfocado en la comunicación con la API de Patent Sweep (Equipo 2). |
| **Negotiable** | **Cumple** | La estrategia de timeout o reintentos es adaptable. |
| **Valuable** | **Cumple** | Previene riesgos legales antes de autorizar la fabricación. |
| **Estimable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Small** | **Cumple** | Acotada a la invocación externa y procesamiento de la respuesta de patentes. |
| **Testable** | **Cumple** | Verificable con pruebas de integración y mocks (HTTP 200, 502, 422). |

* **Trazabilidad:** Cita fuente válida (*"...Consume de forma síncrona/asíncrona la API de Patent Sweep (Equipo 2)..."*).

#### 2. Evaluación de Criterios de Aceptación (SMART + Cuestionario QA)

##### CA-05.1: Verificación síncrona exitosa de riesgo de patentes (Camino Feliz)

| Dimensión | Estado | Justificación |
| :---- | :---- | :---- |
| **Specific** | **Cumple** | Define la acción sobre POST /formulas/form-501/evaluar-patente y respuesta de la API externa. |
| **Measurable** | **Cumple** | Retorna HTTP 200 OK y persiste el dictamen "Bajo". |
| **Achievable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Relevant** | **Cumple** | Integra la validación legal SOA en el flujo. |
| **Time-boxed** | **Cumple** | Operación síncrona con timeout configurado. |

* **Cuestionario de 5 Preguntas:**
  1. *¿Indica en qué endpoint/evento ocurre la acción?* **Sí** (POST /formulas/form-501/evaluar-patente).
  2. *¿Describe los campos, datos y payload relevantes?* **Sí** (Dictamen de riesgo "Bajo" e ID de análisis).
  3. *¿Describe validaciones, códigos de estado HTTP y mensajes de error?* **Sí** (HTTP 200 OK).
  4. *¿Describe restricciones técnicas o de negocio si aplican?* **Sí** (Disponibilidad del Equipo 2).
  5. *¿Representa una única condición atómica y verificable?* **Sí**.

##### CA-05.2: Fallo técnico de integración SOA por indisponibilidad de Patent Sweep (Falla Técnica SOA)

| Dimensión | Estado | Justificación |
| :---- | :---- | :---- |
| **Specific** | **Cumple** | Describe explícitamente el fallo por timeout o HTTP 503 en la API externa del Equipo 2. |
| **Measurable** | **Cumple** | Retorna HTTP 502 Bad Gateway y mensaje de error desacoplado. |
| **Achievable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Relevant** | **Cumple** | Garantiza la resiliencia y el aislamiento de errores de integración SOA. |
| **Time-boxed** | **Cumple** | Manejo del límite de tiempo (timeout). |

* **Cuestionario de 5 Preguntas:**
  1. *¿Indica en qué endpoint/evento ocurre la acción?* **Sí** (Llamada cliente HTTP hacia Patent Sweep desde la API de Materiales).
  2. *¿Describe los campos, datos y payload relevantes?* **Sí** (Simulación de HTTP 503 / Timeout).
  3. *¿Describe validaciones, códigos de estado HTTP y mensajes de error?* **Sí** (HTTP 502 Bad Gateway y mensaje "No fue posible conectar con el servicio de Patent Sweep (Equipo 2)...").
  4. *¿Describe restricciones técnicas o de negocio si aplican?* **Sí** (Diferenciación de fallo técnico SOA respecto a un error de negocio).
  5. *¿Representa una única condición atómica y verificable?* **Sí**.

##### CA-05.3: Componentes de fórmula ambiguos para el análisis legal (Error de Negocio)

| Dimensión | Estado | Justificación |
| :---- | :---- | :---- |
| **Specific** | **Cumple** | Modela el caso donde la API externa rechaza la solicitud (HTTP 400) por datos incompletos en la fórmula. |
| **Measurable** | **Cumple** | Retorna HTTP 422 Unprocessable Entity hacia el usuario. |
| **Achievable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Relevant** | **Cumple** | Mapea correctamente un error de datos de origen durante la integración. |
| **Time-boxed** | **Cumple** | Operación síncrona. |

* **Cuestionario de 5 Preguntas:**
  1. *¿Indica en qué endpoint/evento ocurre la acción?* **Sí** (POST /formulas/{id}/evaluar-patente).
  2. *¿Describe los campos, datos y payload relevantes?* **Sí** (Respuesta HTTP 400 de Patent Sweep por inconsistencia técnica).
  3. *¿Describe validaciones, códigos de estado HTTP y mensajes de error?* **Sí** (HTTP 422 Unprocessable Entity).
  4. *¿Describe restricciones técnicas o de negocio si aplican?* **Sí** (Mapeo de errores entre fronteras de servicios).
  5. *¿Representa una única condición atómica y verificable?* **Sí**.

#### 3. Veredicto Final de la HU-05

**APROBADA**

* **Justificación:** Excelente cobertura de flujos. Es la única HU que implementa y diferencia un **fallo técnico de integración SOA (HTTP 502 Bad Gateway)** frente a fallos de negocio (HTTP 422). Cumple al 100% con los criterios de auditoría.

---

### HU-06 | Actualización del Estado de Aprobación Legal de la Fórmula

#### 1. Matriz Criterios INVEST

| Criterio | Estado | Justificación |
| :---- | :---- | :---- |
| **Independent** | **Cumple** | Modificación puntual del atributo de estado en el ciclo de vida de la fórmula. |
| **Negotiable** | **Cumple** | Los nombres de la máquina de estados se pueden ajustar con el negocio. |
| **Valuable** | **Cumple** | Controla el cambio de estado para bloquear recetas con riesgo legal. |
| **Estimable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Small** | **Cumple** | Operación acotada a una actualización parcial (PATCH). |
| **Testable** | **Cumple** | Evaluable a través de respuestas HTTP 200 OK y 409 Conflict. |

* **Trazabilidad:** Cita fuente válida (*"Como Analista de Cumplimiento, quiero verificar el estado legal y de patentes asociado a los materiales y fórmulas registrados..."*).

#### 2. Evaluación de Criterios de Aceptación (SMART + Cuestionario QA)

##### CA-06.1: Aprobación legal exitosa de una fórmula (Camino Feliz)

| Dimensión | Estado | Justificación |
| :---- | :---- | :---- |
| **Specific** | **Cumple** | Define la petición PATCH /formulas/form-501/estado-legal con el valor "APROBADO_LEGAL". |
| **Measurable** | **Cumple** | Retorna HTTP 200 OK y actualiza el estado a "Lista para Producción". |
| **Achievable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Relevant** | **Cumple** | Habilita el ciclo de vida de la fórmula para ser consumida por Fabricación. |
| **Time-boxed** | **Cumple** | Operación síncrona. |

* **Cuestionario de 5 Preguntas:**
  1. *¿Indica en qué endpoint/evento ocurre la acción?* **Sí** (PATCH /formulas/form-501/estado-legal).
  2. *¿Describe los campos, datos y payload relevantes?* **Sí** (Payload con estado "APROBADO_LEGAL").
  3. *¿Describe validaciones, códigos de estado HTTP y mensajes de error?* **Sí** (HTTP 200 OK).
  4. *¿Describe restricciones técnicas o de negocio si aplican?* **Sí** (La fórmula debe tener dictamen previo sin infracciones).
  5. *¿Representa una única condición atómica y verificable?* **Sí**.

##### CA-06.2: Intento de aprobación de una fórmula con conflicto de patentes activo (Error de Negocio)

| Dimensión | Estado | Justificación |
| :---- | :---- | :---- |
| **Specific** | **Cumple** | Define el intento de transición no permitida sobre form-502 con dictamen "INFRACCION_DETECTADA". |
| **Measurable** | **Cumple** | Retorna HTTP 409 Conflict con mensaje de error explícito. |
| **Achievable** | **Pendiente** | **Pendiente de validación por el equipo.** |
| **Relevant** | **Cumple** | Protege al negocio contra violaciones de propiedad intelectual. |
| **Time-boxed** | **Cumple** | Operación síncrona. |

* **Cuestionario de 5 Preguntas:**
  1. *¿Indica en qué endpoint/evento ocurre la acción?* **Sí** (PATCH /formulas/form-502/estado-legal).
  2. *¿Describe los campos, datos y payload relevantes?* **Sí** (Estado previo "INFRACCION_DETECTADA").
  3. *¿Describe validaciones, códigos de estado HTTP y mensajes de error?* **Sí** (HTTP 409 Conflict y mensaje "No se puede aprobar una fórmula con infracciones de patente activas").
  4. *¿Describe restricciones técnicas o de negocio si aplican?* **Sí** (Transición inválida en la máquina de estados).
  5. *¿Representa una única condición atómica y verificable?* **Sí**.

#### 3. Veredicto Final de la HU-06

**APROBADA**

* **Justificación:** Cumple al 100% con INVEST y SMART ("Estimable" y "Achievable" en "Pendiente"). Cobertura de caso feliz y validación de regla de negocio completa.

---

### Tabla Resumen Final de la Auditoría

| ID HU | Veredicto | Principal Hallazgo / Acción Requerida |
| :---- | :---- | :---- |
| **HU-01** | **APROBADA CON OBSERVACIONES** | Falla menor de especificación en CA-01.1. Se sugiere explicitar la estructura del payload obligatoria (codigoTecnico, nombre, unidadMedida). |
| **HU-02** | **APROBADA** | Cumplimiento total de INVEST, SMART y trazabilidad. "Estimable" y "Achievable" en "Pendiente". |
| **HU-03** | **APROBADA** | Cumplimiento total. Gran definición de validación de negocio (regla matemática del 100% con HTTP 422). |
| **HU-04** | **APROBADA CON OBSERVACIONES** | Ambigüedad en código de error HTTP en CA-04.2 ("409 Conflict o 400 Bad Request"). Se debe unificar a un único código (ej. 409 Conflict). |
| **HU-05** | **APROBADA** | Cumplimiento total. Sobresaliente manejo de integración SOA con diferenciación explícita de falla técnica (HTTP 502 Bad Gateway). |
| **HU-06** | **APROBADA** | Cumplimiento total. Correcta implementación de reglas de negocio para la máquina de estados de patentes. |
