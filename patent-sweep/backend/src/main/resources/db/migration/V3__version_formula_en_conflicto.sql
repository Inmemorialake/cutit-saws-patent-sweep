-- DA-17: el conflicto es único por fórmula + patente + versión de la fórmula.
-- Descartar un conflicto aplica solo a esa versión; una versión nueva se evalúa de nuevo.
ALTER TABLE conflicto ADD COLUMN version_formula VARCHAR(20) NOT NULL DEFAULT '1';
ALTER TABLE conflicto ALTER COLUMN version_formula DROP DEFAULT;

ALTER TABLE conflicto DROP CONSTRAINT uq_conflicto_formula_patente;
ALTER TABLE conflicto ADD CONSTRAINT uq_conflicto_formula_patente_version
    UNIQUE (formula_id, patente_id, version_formula);

DROP INDEX ix_conflicto_formula_estado;
CREATE INDEX ix_conflicto_formula_version_estado ON conflicto (formula_id, version_formula, estado);
