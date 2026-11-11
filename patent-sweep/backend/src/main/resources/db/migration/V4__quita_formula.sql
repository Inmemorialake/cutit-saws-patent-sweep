-- La fórmula es de Materiales/Fórmulas: aquí solo se guarda su código (formula_id) como texto.
-- El estado se calcula al consultarlo (HU-04, 3.2), por eso ya no se guarda en una tabla propia.
ALTER TABLE conflicto DROP CONSTRAINT conflicto_formula_id_fkey;
ALTER TABLE revision_formula DROP CONSTRAINT revision_formula_formula_id_fkey;
ALTER TABLE evaluacion_formula DROP CONSTRAINT evaluacion_formula_formula_id_fkey;

DROP TABLE formula;

-- Consultas del estado: última revisión y evaluaciones abiertas de una fórmula.
CREATE INDEX ix_revision_formula_fecha ON revision_formula (formula_id, fecha_revision DESC);
CREATE INDEX ix_evaluacion_formula_estado ON evaluacion_formula (formula_id, estado);
