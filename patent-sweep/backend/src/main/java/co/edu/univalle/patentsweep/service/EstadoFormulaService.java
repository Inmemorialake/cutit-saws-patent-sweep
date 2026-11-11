package co.edu.univalle.patentsweep.service;

import co.edu.univalle.patentsweep.model.Formula;
import co.edu.univalle.patentsweep.exception.FormulaNoEncontradaException;
import co.edu.univalle.patentsweep.model.enums.EstadoConflicto;
import co.edu.univalle.patentsweep.model.enums.EstadoFormula;
import co.edu.univalle.patentsweep.model.enums.ResultadoRevision;
import co.edu.univalle.patentsweep.repository.ConflictoRepository;
import co.edu.univalle.patentsweep.repository.FormulaRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * Modelo de lectura precalculado (P16, DA-08): al cerrar cada revisión se recalcula y guarda el
 * estado de la fórmula, de modo que HU-04 responde sin buscar patentes en tiempo real.
 */
@Service
public class EstadoFormulaService {

    private final ConflictoRepository conflictoRepository;
    private final FormulaRepository formulaRepository;

    public EstadoFormulaService(ConflictoRepository conflictoRepository, FormulaRepository formulaRepository) {
        this.conflictoRepository = conflictoRepository;
        this.formulaRepository = formulaRepository;
    }

    /**
     * Regla del PB (HU-04, 3.2): CONFLICTO_DETECTADO si hay al menos un conflicto DETECTADO de la versión evaluada; NO_PROCESADO
     * si la última revisión falló técnicamente; LIBRE en cualquier otro caso (ya fue revisada).
     */
    public Formula actualizarEstado(Formula formula, String versionFormula, ResultadoRevision ultimoResultado,
                                    LocalDateTime fechaRevision) {
        long vigentes = conflictoRepository.countByFormula_FormulaIdAndVersionFormulaAndEstado(
                formula.getFormulaId(), versionFormula, EstadoConflicto.DETECTADO);
        EstadoFormula nuevoEstado;
        if (vigentes > 0) {
            nuevoEstado = EstadoFormula.CONFLICTO_DETECTADO;
        } else if (ultimoResultado == ResultadoRevision.FALLO_TECNICO) {
            nuevoEstado = EstadoFormula.NO_PROCESADO;
        } else {
            nuevoEstado = EstadoFormula.LIBRE;
        }
        formula.setEstado(nuevoEstado);
        formula.setFechaUltimaRevision(fechaRevision);
        return formulaRepository.save(formula);
    }

    /**
     * Consulta el estado precalculado de una fórmula para Fabricación (PB HU-04).
     * Lanza FormulaNoEncontradaException si no existe en la base de datos.
     */
    public Formula consultarEstado(String formulaId) {
        return formulaRepository.findById(formulaId)
                .orElseThrow(FormulaNoEncontradaException::new);
    }
}
