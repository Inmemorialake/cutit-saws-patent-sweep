# ADR-004: Foto de la fórmula en cada revisión

- **Estado:** Aceptada
- **Relacionado con:** DA-10, HU-02

## Contexto

`revision_formula` es el historial de solo inserción. Sin guardar la versión ni la composición de lo que se evaluó, no se puede demostrar con qué datos se hizo cada revisión, y Materiales solo entrega la versión vigente.

## Decisión

Cada revisión guarda (migración V5) la columna `version_formula` y, en `parametros` (JSON), una **foto** de lo evaluado: código, versión, nombre, aplicación técnica, estado y composición completa (SKU, nombre y porcentaje de cada material), más los términos buscados, los filtros y los códigos de las patentes candidatas. HU-04 usa la versión: si la última revisión es de otra versión que la vigente, el estado es `NO_PROCESADO`.

## Consecuencias

- La revisión sirve como evidencia aunque Materiales cambie la fórmula, sin tablas nuevas ni sincronización.
- La composición exacta es **información sensible**: hay que proteger el acceso a la base y confirmar con el Equipo 1 que se puede copiar.
- Las revisiones anteriores a la migración quedan sin versión y se tratan como no revisadas.
- Limitación conocida: el usuario de la búsqueda aún es fijo en `BusquedaController`; hasta tomarlo del token, la auditoría no dice quién la pidió.
