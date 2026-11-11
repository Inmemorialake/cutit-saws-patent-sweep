# ADR-002: Conflicto único por fórmula, patente y versión

- **Estado:** Aceptada, pendiente de confirmar con el profesor
- **Relacionado con:** DA-17, HU-01, HU-02, HU-09

## Contexto

El PB (HU-01, 6.2 y HU-09, 3.2) dice que un conflicto es único por pareja fórmula-patente y que su descarte es permanente. Si una fórmula cambia de versión y una patente ya descartada vuelve a coincidir, ese descarte antiguo ocultaría un problema nuevo.

## Decisión

El conflicto es único por **fórmula + patente + versión de la fórmula** (migración V3, columna `conflicto.version_formula`). Un descarte vale solo para la versión juzgada; una versión nueva se evalúa de nuevo. El estado de HU-04 cuenta únicamente los conflictos de la versión vigente.

## Consecuencias

- Un descarte antiguo ya no oculta un conflicto en una versión nueva (más seguridad).
- **Se aparta del PB:** hay que confirmarlo con el profesor y dejarlo en la sección de desviaciones del DAS.
- Cuando cambia la versión pueden aparecer conflictos que ya se habían descartado, y hay que revisarlos otra vez.
- Los conflictos anteriores a la migración quedaron con la versión `1`.
