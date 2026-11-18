# Product Backlog v1 — API Materiales/Fórmulas (Equipo 1)

23 de septiembre de 2026 · Andrés Gerardo González Rosero

## Contexto

Este backlog descompone las épicas EP-01, EP-02 y EP-03 en 6 historias de usuario (HU) para la API de Materiales/Fórmulas del Equipo 1, caso Cutit Saws (750021C Desarrollo de Software II, 2026-2).

**Marco:** ISO/IEC/IEEE 29148:2018; HU en modelo de Cohn validadas con INVEST; criterios de aceptación (CA) en Dado/Cuando/Entonces validados con SMART. Estimable (INVEST) y Achievable (SMART) quedan pendientes de validación por el equipo en todas las HU.

**Endpoints (fijos, sin agregar otros):** `POST /materiales` y `GET /materiales/{id}` (Caso de Estudio, §11).

**Integraciones:** consume Patent Sweep (Equipo 2); es consumida por Fabricación (Equipo 3) y por Patent Sweep.

**Decisiones derivadas del Paso 0:**

- Una fórmula se modela como un material con composición no vacía, porque no se agregan endpoints.
- La acción "actualizar" de EP-01 queda sin HU: exigiría un endpoint nuevo.
- El dictamen de patentes se expone dentro de la respuesta de `GET /materiales/{id}`.
- Si Patent Sweep falla, la fórmula queda en "pendiente de verificación" (confirmado por el equipo).
- Atributos, proporciones, contrato con Patent Sweep, efecto del dictamen y roles siguen abiertos: se registran como supuestos.

## Glosario de dominio

| Término | Definición operativa | Fuente |
| --- | --- | --- |
| Material (insumo) | Elemento físico usado en el laboratorio para fabricar cuchillas. Recurso de `POST /materiales` y `GET /materiales/{id}`. El caso no define sus atributos. | Procesos y Operación Actual; Caso de Estudio §11 |
| Material base | Material registrado sin composición (ver S3). | Supuesto S3 |
| Fórmula (receta) | Especificación predefinida de qué materiales y en qué proporción se aplican para fabricar una cuchilla. Se registra como material con composición no vacía (ver S3). | Procesos y Operación Actual; EP-02 |
| Composición | Conjunto de pares (material registrado, proporción) que forman una fórmula. | EP-02 |
| Cuchilla | Producto que sale del laboratorio; alcance de esta API. | Caso de Estudio §11 |
| Cadena | Producto ensamblado a partir de cuchillas; responsabilidad de Fabricación (Equipo 3). | Procesos y Operación Actual |
| Dictamen de riesgo de patente | Resultado que Patent Sweep devuelve sobre una fórmula; sus valores los define el Equipo 2. | EP-03; Caso de Estudio §11 |
| Pendiente de verificación | Estado de riesgo de una fórmula cuando Patent Sweep no respondió o respondió con error. | Respuesta del equipo al Paso 0 |

## Supuestos asumidos

Ocho supuestos cubren lo que el caso no define; cada HU indica cuáles usa.

| ID | Supuesto asumido | Origen |
| --- | --- | --- |
| S1 | Roles: "responsable del laboratorio de Cutit" (EP-01, EP-02) y "encargado de innovación/PI" (EP-03). | Lista de usuarios del prompt; el caso solo menciona "el laboratorio de Cutit". |
| S2 | Campos del material: nombre y tipo (obligatorios); composición (opcional). | Ejemplo HU-01 del prompt; el caso no define atributos. |
| S3 | Una fórmula se registra con `POST /materiales` como material con composición no vacía; un material base tiene composición vacía. | Restricción de no agregar endpoints. |
| S4 | Cada componente tiene un ID de material y una proporción numérica mayor que 0. Unidad y regla de suma (p. ej., 100 %) pendientes. | EP-02 ("proporciones exactas"). |
| S5 | Al registrar una fórmula se envían sus componentes a Patent Sweep y se guarda el dictamen tal como llega. Los materiales base no se verifican. Endpoint, payload y valores del dictamen se acuerdan con el Equipo 2. | Integración SOA de EP-03. |
| S6 | El dictamen es informativo: no bloquea el registro ni la consulta. | Efecto del dictamen sin definir; el caso solo dice "anticipar". |
| S7 | El valor del timeout hacia Patent Sweep se acuerda con el Equipo 2. | El estado "pendiente de verificación" sí está confirmado. |
| S8 | Códigos HTTP: 201 al crear, 200 al consultar, 400 por validación, 404 por recurso inexistente. | Propuesta del Paso 0, no objetada. |

## EP-01: Gestión del Catálogo de Materiales e Insumos

Dos HU cubren registro y consulta; la actualización queda fuera por la restricción de endpoints.

### HU-01: Registrar material base

**Fuente:** "la fabricación de las cuchillas en el laboratorio de Cutit [...] requiere el uso de materiales específicos" (Procesos y Operación Actual).

Como responsable del laboratorio de Cutit, quiero registrar un material base con su nombre y tipo, para que quede disponible como componente de las fórmulas de fabricación de cuchillas.

- **CA1 (Éxito):** Dado un `POST /materiales` con nombre y tipo y sin composición, cuando se procesa, entonces responde 201 con un identificador único y el material queda consultable en `GET /materiales/{id}`.
- **CA2 (Falla):** Dado un `POST /materiales` sin nombre o sin tipo, cuando se valida, entonces responde 400, indica qué campo falta y no crea ningún registro.

**Supuestos:** S1, S2, S3, S8. **Estimable / Achievable:** pendiente de validación por el equipo.

### HU-02: Consultar material por identificador

**Fuente:** endpoint mínimo "GET /materiales/{id}" (Caso de Estudio, §11 Tabla Formal de APIs).

Como responsable del laboratorio de Cutit, quiero consultar un material por su identificador, para verificar los datos con que quedó registrado.

- **CA1 (Éxito):** Dado un ID existente, cuando se hace `GET /materiales/{id}`, entonces responde 200 con nombre y tipo idénticos a los registrados.
- **CA2 (Falla):** Dado un ID que no existe, cuando se hace `GET /materiales/{id}`, entonces responde 404 y no devuelve datos de ningún material.

**Supuestos:** S1, S2, S8. **Estimable / Achievable:** pendiente de validación por el equipo.

## EP-02: Gestión de Fórmulas y Recetas de Fabricación

Dos HU: registrar la fórmula con su composición y exponerla a Fabricación, ambas sobre los endpoints existentes.

### HU-03: Registrar fórmula con su composición

**Fuente:** "centralice la gestión de materiales y fórmulas [...] garantizando trazabilidad" (Problemas del Negocio y Necesidades).

Como responsable del laboratorio de Cutit, quiero registrar una fórmula indicando qué materiales registrados la componen y en qué proporción, para estandarizar la receta con que se fabrica cada cuchilla.

- **CA1 (Éxito):** Dado un `POST /materiales` con nombre, tipo y una composición en la que cada componente referencia un material existente con proporción mayor que 0, cuando se procesa, entonces responde 201 con un ID y guarda exactamente esos componentes y proporciones.
- **CA2 (Falla):** Dado un componente que referencia un ID de material inexistente, cuando se valida, entonces responde 400 indicando el ID no encontrado y no crea la fórmula.
- **CA3 (Borde):** Dado un componente sin proporción o con proporción ≤ 0, cuando se valida, entonces responde 400 indicando el componente inválido y no crea la fórmula.

**Supuestos:** S1, S2, S3, S4, S8. **Estimable / Achievable:** pendiente de validación por el equipo.

### HU-04: Exponer la composición de una fórmula a Fabricación

**Fuente:** "Equipo 3 – Fabricación [...] Consume APIs: Materiales/Fórmulas" (Caso de Estudio, §11).

Como API de Fabricación (Equipo 3), quiero obtener la composición de una fórmula por su identificador, para validar mis registros de fabricación contra una receta registrada.

- **CA1 (Éxito):** Dado el ID de una fórmula existente, cuando se hace `GET /materiales/{id}`, entonces responde 200 con la lista de componentes (ID de material y proporción) idéntica a la registrada.
- **CA2 (Falla):** Dado un ID inexistente, cuando se consulta, entonces responde 404, de modo que Fabricación pueda distinguir una fórmula no registrada.

**Supuestos:** S3, S4, S8. **Estimable / Achievable:** pendiente de validación por el equipo.

## EP-03: Verificación de Riesgo de Patentes

Dos HU: verificar cada fórmula contra Patent Sweep al registrarla y exponer su estado de riesgo; ambas dependen del contrato con el Equipo 2.

### HU-05: Verificar riesgo de patente al registrar una fórmula

**Fuente:** "se realiza un barrido periódico de patentes [...] con el fin de anticipar riesgos de infracción o de competencia" (Procesos y Operación Actual); "Equipo 1 [...] Consume APIs: Patent Sweep" (Caso de Estudio, §11).

Como encargado de innovación/PI, quiero que cada fórmula registrada se someta a verificación en Patent Sweep, para anticipar riesgos de infracción antes de que pase a fabricación.

- **CA1 (Éxito):** Dado un registro de fórmula válido y Patent Sweep disponible, cuando se registra, entonces se envían sus componentes a Patent Sweep y el dictamen devuelto queda asociado a la fórmula sin modificaciones.
- **CA2 (Falla):** Dado que Patent Sweep no responde dentro del timeout acordado o responde con error, cuando se registra la fórmula, entonces se crea igual (201) con estado de riesgo "pendiente de verificación".
- **CA3 (Borde):** Dado un dictamen que indica riesgo, cuando se recibe, entonces el registro no se bloquea y el dictamen se almacena tal cual.

**Supuestos:** S1, S5, S6, S7. **Estimable / Achievable:** pendiente de validación por el equipo.

### HU-06: Consultar el estado de riesgo de una fórmula

**Fuente:** "con el fin de anticipar riesgos de infracción o de competencia" (Procesos y Operación Actual).

Como encargado de innovación/PI, quiero consultar el estado de riesgo de patente de una fórmula, para decidir si requiere revisión antes de fabricarse.

- **CA1 (Éxito):** Dada una fórmula con dictamen, cuando se hace `GET /materiales/{id}`, entonces la respuesta incluye el estado de riesgo idéntico al recibido de Patent Sweep.
- **CA2 (Borde):** Dada una fórmula cuya verificación falló, cuando se consulta, entonces la respuesta incluye el estado "pendiente de verificación".

**Supuestos:** S1, S5, S7. **Estimable / Achievable:** pendiente de validación por el equipo.

## Tabla resumen

6 HU y 14 CA en total; todas las HU tienen al menos un CA de éxito y uno de falla o borde.

| ID | Épica | HU | # CA | Supuestos |
| --- | --- | --- | --- | --- |
| HU-01 | EP-01 | Registrar material base | 2 | S1, S2, S3, S8 |
| HU-02 | EP-01 | Consultar material por identificador | 2 | S1, S2, S8 |
| HU-03 | EP-02 | Registrar fórmula con su composición | 3 | S1, S2, S3, S4, S8 |
| HU-04 | EP-02 | Exponer la composición a Fabricación | 2 | S3, S4, S8 |
| HU-05 | EP-03 | Verificar riesgo al registrar una fórmula | 3 | S1, S5, S6, S7 |
| HU-06 | EP-03 | Consultar el estado de riesgo | 2 | S1, S5, S7 |

## Autorrevisión y pendientes

Cinco puntos quedan abiertos tras la revisión INVEST/SMART; ninguno se puede cerrar sin el equipo o el profesor.

- **Brecha en EP-01:** "actualizar" no tiene HU porque exige un endpoint nuevo (`PUT` o `PATCH`). Consultarlo con el profesor.
- **Independencia (INVEST):** HU-04 y HU-06 necesitan datos creados por HU-03 y HU-05. Se mitiga con fixtures en pruebas; tenerlo en cuenta al priorizar.
- **Medible (SMART):** CA2 de HU-05 solo es medible cuando se fije el timeout.
- **Fórmulas anidadas:** no se sabe si una fórmula puede tener otra fórmula como componente.
- **Reintento de pendientes:** no hay mecanismo para reverificar fórmulas en "pendiente de verificación"; candidata a HU cuando exista el contrato con Patent Sweep.

### Agenda para la reunión con el Equipo 2 (Patent Sweep)

- [ ] Qué endpoint llamar: `POST /patentes/busqueda` o `GET /patentes/estado`.
- [ ] Qué payload enviar: IDs de material o la composición completa.
- [ ] Qué valores puede tener el dictamen.
- [ ] Cuál será el timeout.
- [ ] Para qué consumirá el Equipo 2 `GET /materiales/{id}` (evitar dependencia circular en el despliegue).

## Respuesta del validador

### Auditoría del Product Backlog v1: API Materiales/Fórmulas (Equipo 1)

**Notas de método**

- **Escala.** Cumple / Falla menor / Falla / Pendiente. En Estimable y Achievable solo verifico la marca "pendiente de validación por el equipo".
- **Independent.** Una HU falla I si su único comportamiento es leer lo que otra HU produce. Si solo depende de datos que se pueden sembrar con fixtures, cumple.
- **Time-bound (hallazgo transversal H-T).** Ningún CA de los 14 fija un tiempo máximo de respuesta, y el caso no trae un requisito no funcional de latencia. Si lo contara como falla en cada CA, ninguna HU podría quedar en "Aprobada con observaciones" y la rúbrica no dice qué pasa en ese caso. Por eso lo registro una sola vez y lo dejo fuera del conteo por HU. Si prefieres contarlo por CA, avísame y recalculo.

---

#### HU-01: Registrar material base

| INVEST | Resultado | Justificación |
|---|---|---|
| Independent | Cumple | Su contrato (S2) no depende de otra HU. Ojo: el CA1 se verifica con `GET` (HU-02). |
| Negotiable | Cumple | Campos y códigos quedan como supuestos (S2, S8), no como diseño cerrado. |
| Valuable | Cumple | Sin materiales base no se pueden componer fórmulas (EP-02). |
| Estimable | Pendiente ✓ | Está marcado correctamente. |
| Small | Cumple | Es una sola operación con dos CA. |
| Testable | Cumple | Ambos CA tienen un resultado HTTP observable. |

**CA1 (Éxito)**

| SMART | Resultado | Cuestionario | Resultado |
|---|---|---|---|
| S | Cumple: indica endpoint, campos y ausencia de composición | ¿Dónde? | Cumple: `POST /materiales` |
| M | Cumple: 201, ID y consultable | ¿Campos? | Cumple: nombre, tipo, composición vacía |
| A | Pendiente ✓ | ¿Validaciones/error? | No aplica (éxito) |
| R | Cumple | ¿Restricciones? | Cumple: el caso no define ninguna (S2) |
| T | H-T | ¿Única condición? | Cumple: un solo resultado, aunque acopla la prueba con HU-02 |

**CA2 (Falla)**

| SMART | Resultado | Cuestionario | Resultado |
|---|---|---|---|
| S | **Falla menor:** "sin nombre" no distingue entre campo ausente, `null` o cadena vacía | ¿Dónde? | Cumple |
| M | Cumple: 400 y no crea registro | ¿Campos? | Cumple |
| A | Pendiente ✓ | ¿Validaciones/error? | Cumple: indica el campo faltante |
| R | Cumple | ¿Restricciones? | Cumple: obligatoriedad |
| T | H-T | ¿Única condición? | Cumple: es una regla, probable con dos casos parametrizados |

**Veredicto: Aprobada con observaciones.** Tiene 1 CA con 1 falla menor, dentro del umbral.
**Sugerencia:** en CA2, escribir "con nombre o tipo ausente, `null` o vacío".

---

#### HU-02: Consultar material por identificador

| INVEST | Resultado | Justificación |
|---|---|---|
| Independent | Cumple | El contrato lo define S2 y se puede probar con datos sembrados. |
| Negotiable | Cumple | No fija formato de respuesta más allá de lo que dicen los supuestos. |
| Valuable | Cumple | Permite verificar el registro. |
| Estimable | Pendiente ✓ | Está marcado correctamente. |
| Small | Cumple | Es una lectura. |
| Testable | Cumple | Tiene 200 y 404 observables. |

**CA1 (Éxito)**

| SMART | Resultado | Cuestionario | Resultado |
|---|---|---|---|
| S | Cumple | ¿Dónde? | Cumple: `GET /materiales/{id}` |
| M | Cumple: 200 e igualdad de campos | ¿Campos? | Cumple: nombre y tipo (no define el campo de riesgo, ver X-4) |
| A | Pendiente ✓ | ¿Validaciones/error? | No aplica |
| R | Cumple | ¿Restricciones? | No aplica |
| T | H-T | ¿Única condición? | Cumple |

**CA2 (Falla)**

| SMART | Resultado | Cuestionario | Resultado |
|---|---|---|---|
| S | Cumple | ¿Dónde? | Cumple |
| M | Cumple: 404 y sin datos | ¿Campos? | Cumple: ID |
| A | Pendiente ✓ | ¿Validaciones/error? | **Falla menor:** no describe ningún mensaje de error, solo la ausencia de datos |
| R | Cumple | ¿Restricciones? | Cumple |
| T | H-T | ¿Única condición? | Cumple |

**Veredicto: Aprobada con observaciones.** Tiene 1 CA con 1 falla menor.
**Sugerencia:** indicar que el 404 lleva un mensaje con el ID no encontrado. Además, conviene definir qué pasa con un ID mal formado (¿400 o 404?).

---

#### HU-03: Registrar fórmula con su composición

| INVEST | Resultado | Justificación |
|---|---|---|
| Independent | Cumple | Necesita materiales existentes, pero son datos sembrables, y tiene validaciones propias. |
| Negotiable | Cumple | Unidad y regla de suma quedan abiertas (S4). |
| Valuable | Cumple | Estandariza recetas y tiene fuente citada. |
| Estimable | Pendiente ✓ | Está marcado correctamente. |
| Small | Cumple | Son tres CA sobre una sola operación. |
| Testable | Cumple | Todos los CA tienen un resultado observable. |

**CA1 (Éxito)**

| SMART | Resultado | Cuestionario | Resultado |
|---|---|---|---|
| S | Cumple | ¿Dónde? | Cumple |
| M | Cumple: "guarda exactamente" solo se observa con `GET` (ver X-2) | ¿Campos? | Cumple: nombre, tipo, (ID, proporción) |
| A | Pendiente ✓ | ¿Validaciones/error? | No aplica |
| R | Cumple | ¿Restricciones? | **Falla menor:** no dice si el mismo material puede aparecer dos veces en la composición, y la regla de suma sigue pendiente (S4) |
| T | H-T | ¿Única condición? | Cumple |

**CA2 (Falla)**

| SMART | Resultado | Cuestionario | Resultado |
|---|---|---|---|
| S | Cumple | ¿Dónde? | Cumple |
| M | Cumple: 400 y no crea la fórmula | ¿Campos? | Cumple: ID del componente |
| A | Pendiente ✓ | ¿Validaciones/error? | Cumple: indica el ID no encontrado |
| R | Cumple | ¿Restricciones? | Cumple (sobre el uso de 400, ver X-5) |
| T | H-T | ¿Única condición? | Cumple |

**CA3 (Borde)**

| SMART | Resultado | Cuestionario | Resultado |
|---|---|---|---|
| S | Cumple | ¿Dónde? | Cumple |
| M | Cumple: 400 y no crea la fórmula | ¿Campos? | Cumple: proporción |
| A | Pendiente ✓ | ¿Validaciones/error? | Cumple: indica el componente inválido |
| R | Cumple | ¿Restricciones? | Cumple: el límite ≤ 0 es explícito |
| T | H-T | ¿Única condición? | Cumple: es una regla |

**Veredicto: Aprobada con observaciones.** Tiene 1 CA con 1 falla menor.
**Sugerencia:** en CA1, declarar si se aceptan componentes repetidos. En CA2, indicar si se reportan todos los IDs inexistentes o solo el primero. En CA3, cubrir el caso de una proporción no numérica.

---

#### HU-04: Exponer la composición de una fórmula a Fabricación

| INVEST | Resultado | Justificación |
|---|---|---|
| Independent | **Falla** | Su único comportamiento es leer la composición que crea HU-03, y el propio documento lo reconoce en la Autorrevisión. |
| Negotiable | Cumple | No fija detalles de implementación. |
| Valuable | Cumple | El actor no es humano (una API). Es válido en SOA, pero el beneficiario real es el Equipo 3. |
| Estimable | Pendiente ✓ | Está marcado correctamente. |
| Small | Cumple | Es una lectura. |
| Testable | Cumple | Tiene 200 y 404 observables. |

**CA1 (Éxito)**

| SMART | Resultado | Cuestionario | Resultado |
|---|---|---|---|
| S | Cumple | ¿Dónde? | Cumple |
| M | Cumple: igualdad de la lista | ¿Campos? | Cumple: ID de material y proporción |
| A | Pendiente ✓ | ¿Validaciones/error? | No aplica |
| R | Cumple | ¿Restricciones? | Cumple |
| T | H-T | ¿Única condición? | Cumple |

**CA2 (Falla)**

| SMART | Resultado | Cuestionario | Resultado |
|---|---|---|---|
| S | Cumple | ¿Dónde? | Cumple |
| M | Cumple | ¿Campos? | Cumple |
| A | Pendiente ✓ | ¿Validaciones/error? | Falla menor: sin mensaje de error (igual que HU-02 CA2) |
| R | Cumple | ¿Restricciones? | Cumple |
| T | H-T | ¿Única condición? | Cumple, pero duplica HU-02 CA2 (ver X-1) |

**Veredicto: Rechazada.** Falla Independent, lo que por umbral basta para rechazarla.
**Sugerencia:** convertir el CA1 en un CA de HU-03 ("…y `GET /materiales/{id}` devuelve la composición idéntica"), o ampliar HU-02 a "consultar material o fórmula". La necesidad del Equipo 3 puede quedar registrada como contrato de integración. Falta además un CA de borde: qué recibe Fabricación si consulta el ID de un material base (ver X-6).

---

#### HU-05: Verificar riesgo de patente al registrar una fórmula

| INVEST | Resultado | Justificación |
|---|---|---|
| Independent | Cumple | Tiene lógica propia (invocación, fallback, persistencia), aunque obliga a priorizar HU-03 antes. |
| Negotiable | Cumple | El contrato con el Equipo 2 sigue abierto (S5, S7). |
| Valuable | Cumple | Anticipa riesgos de infracción y tiene fuente citada. |
| Estimable | Pendiente ✓ | Está marcado correctamente. |
| Small | Cumple | Tiene tres CA acotados a un flujo. |
| Testable | Cumple | Con un stub de Patent Sweep todo se puede ejecutar, y el timeout se configura en la prueba. |

**CA1 (Éxito)**

| SMART | Resultado | Cuestionario | Resultado |
|---|---|---|---|
| S | **Falla:** no define qué payload se envía ni a qué endpoint de Patent Sweep | ¿Dónde? | Falla menor: se conoce el disparador (`POST /materiales`), pero no el destino |
| M | Cumple: el dictamen almacenado es igual al devuelto | ¿Campos? | Falla menor: "sus componentes" no tiene estructura definida (misma causa) |
| A | Pendiente ✓ | ¿Validaciones/error? | No aplica |
| R | Cumple | ¿Restricciones? | Cumple (S5 excluye los materiales base, pero el CA no lo dice) |
| T | H-T | ¿Única condición? | Cumple, aunque no indica el código HTTP de la respuesta |

**CA2 (Falla)**

| SMART | Resultado | Cuestionario | Resultado |
|---|---|---|---|
| S | Falla menor: "responde con error" no delimita si son 4xx, 5xx o respuestas malformadas | ¿Dónde? | Cumple |
| M | **Falla:** el timeout no tiene valor, y el documento lo reconoce | ¿Campos? | Cumple: estado de riesgo |
| A | Pendiente ✓ | ¿Validaciones/error? | Cumple: fallback y 201 |
| R | Cumple | ¿Restricciones? | Cumple |
| T | Falla (misma causa que M) | ¿Única condición? | Cumple |

**CA3 (Borde)**

| SMART | Resultado | Cuestionario | Resultado |
|---|---|---|---|
| S | **Falla:** "un dictamen que indica riesgo" depende de valores que aún no están definidos (S5) | ¿Dónde? | Falla menor: el endpoint está implícito |
| M | Falla menor: "no se bloquea" no se traduce a un código HTTP | ¿Campos? | Cumple |
| A | Pendiente ✓ | ¿Validaciones/error? | No aplica |
| R | Queda redundante con CA1 + S6 (ver X-3) | ¿Restricciones? | Cumple |
| T | H-T | ¿Única condición? | Cumple |

**Veredicto: Rechazada.** Tres CA fallan en Specific o Measurable, y el umbral de rechazo es 2. La causa es externa (el contrato con el Equipo 2), no la redacción: la HU no está lista hasta la reunión.
**Sugerencia:** dejarla bloqueada. Después de la reunión, reemplazar "sus componentes" por el payload acordado, fijar el valor del timeout en S7 y fusionar CA3 en CA1 indicando "responde 201".

---

#### HU-06: Consultar el estado de riesgo de una fórmula

| INVEST | Resultado | Justificación |
|---|---|---|
| Independent | **Falla** | Solo expone lo que genera HU-05, y el estado "pendiente" solo existe si HU-05 está implementada. El documento lo reconoce. |
| Negotiable | Cumple | No fija implementación. |
| Valuable | Cumple | Apoya la decisión de revisión. |
| Estimable | Pendiente ✓ | Está marcado correctamente. |
| Small | Cumple | Es una lectura. |
| Testable | Cumple | Tiene resultados observables. |

**Cobertura: falla.** No tiene CA de falla. El CA2 describe un estado válido, no una falla.

**CA1 (Éxito)**

| SMART | Resultado | Cuestionario | Resultado |
|---|---|---|---|
| S | Falla menor: no nombra el campo de la respuesta donde va el estado | ¿Dónde? | Cumple |
| M | Cumple: igualdad con el dictamen | ¿Campos? | Falla menor (misma causa que S) |
| A | Pendiente ✓ | ¿Validaciones/error? | No aplica |
| R | Cumple | ¿Restricciones? | No aplica |
| T | H-T | ¿Única condición? | Cumple, pero duplica HU-05 CA1 (ver X-2) |

**CA2 (Borde)**

| SMART | Resultado | Cuestionario | Resultado |
|---|---|---|---|
| S | Cumple: el valor es literal | ¿Dónde? | Cumple |
| M | Cumple | ¿Campos? | Cumple |
| A | Pendiente ✓ | ¿Validaciones/error? | No aplica |
| R | Cumple | ¿Restricciones? | Cumple |
| T | H-T | ¿Única condición? | Cumple |

**Veredicto: Rechazada.** Falla Independent y le falta un CA de falla; cada condición basta por sí sola.
**Sugerencia:** fusionarla como CA de HU-05 ("…y `GET /materiales/{id}` expone el dictamen en el campo `<nombre>`"). Así también desaparece la falta de un CA de falla.

---

#### Solapamientos y contradicciones entre HU

- **X-1.** HU-02 CA2 y HU-04 CA2 son la misma prueba: mismo endpoint y mismo 404.
- **X-2.** Hay verificaciones duplicadas: HU-03 CA1 con HU-04 CA1, y HU-05 CA1 con HU-06 CA1. Partir en dos HU la escritura y su lectura es lo que causa las dos fallas de Independent.
- **X-3.** HU-05 CA3 queda cubierto por CA1 (almacenar el dictamen tal cual) más S6 (no bloquea).
- **X-4.** Hay un hueco: según S5 los materiales base no se verifican, pero ni HU-02 ni HU-06 definen qué devuelve `GET` en el campo de riesgo para un material base (campo ausente, `null` o "no aplica").
- **X-5.** S8 dice "404 por recurso inexistente", mientras HU-03 CA2 responde 400 cuando el material referenciado no existe. Técnicamente es correcto (404 aplica al recurso de la URL), pero la redacción de S8 admite la lectura contraria. Conviene precisar S8.
- **X-6.** Si Fabricación consulta el ID de un material base, recibe 200 con composición vacía. Ningún CA fija cómo interpretarlo.
- **H-T (transversal).** Ningún CA tiene límite de tiempo de respuesta. Se resuelve con un supuesto S9 de latencia, no CA por CA.

**Verificaciones globales:** las 6 HU citan fuente. Las 6 marcan Estimable y Achievable como pendientes, sin veredicto propio. Solo HU-06 carece de un CA de falla.

#### Resumen

| ID | Veredicto | Principal hallazgo |
|---|---|---|
| HU-01 | Aprobada con observaciones | CA2: "sin nombre" es ambiguo (ausente, `null` o vacío) |
| HU-02 | Aprobada con observaciones | CA2: el 404 no tiene mensaje de error |
| HU-03 | Aprobada con observaciones | CA1: no define componentes repetidos ni regla de suma |
| HU-04 | **Rechazada** | Falla Independent: es solo la lectura de HU-03 y duplica HU-02 CA2 |
| HU-05 | **Rechazada** | Tres CA fallan en Specific o Measurable por el contrato pendiente con Patent Sweep |
| HU-06 | **Rechazada** | Falla Independent y no tiene CA de falla |

Si fusionas HU-04 en HU-03 y HU-06 en HU-05, el backlog queda con 4 HU sin fallas de independencia. La única que seguiría bloqueada es HU-05, hasta que se cierre la agenda con el Equipo 2.
