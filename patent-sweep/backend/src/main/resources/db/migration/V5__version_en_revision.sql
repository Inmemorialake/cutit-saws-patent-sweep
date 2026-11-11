-- Versión de la fórmula que evaluó cada revisión (para HU-04 y la auditoría).
-- Las revisiones anteriores quedan sin versión (NULL): HU-04 las trata como no revisadas
-- para la versión vigente.
ALTER TABLE revision_formula ADD COLUMN version_formula VARCHAR(20);
