-- ADR-012: el conflicto es único por fórmula + patente, sin importar la versión de la fórmula
-- (HU-01 criterio 6.2, HU-02 criterio 3.3). Revierte el UNIQUE de V3 y alinea la base con ADR-009.

-- No se borra ni se fusiona nada: si ya hay datos repetidos entre versiones, la migración se detiene
-- con un mensaje claro para que se depuren a mano (hay notificaciones que referencian a cada conflicto).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM conflicto GROUP BY formula_id, patente_id HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'V8: hay conflictos repetidos por formula_id + patente_id; depurelos antes de migrar';
    END IF;
END $$;

ALTER TABLE conflicto DROP CONSTRAINT uq_conflicto_formula_patente_version;
ALTER TABLE conflicto ADD CONSTRAINT uq_conflicto_formula_patente UNIQUE (formula_id, patente_id);
