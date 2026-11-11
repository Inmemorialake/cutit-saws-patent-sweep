package co.edu.univalle.patentsweep.service;

import co.edu.univalle.patentsweep.integration.MaterialesClient;
import co.edu.univalle.patentsweep.integration.dto.FormulaExternaDTO;
import co.edu.univalle.patentsweep.model.RevisionFormula;
import co.edu.univalle.patentsweep.model.enums.EstadoConflicto;
import co.edu.univalle.patentsweep.model.enums.EstadoFormula;
import co.edu.univalle.patentsweep.model.enums.ResultadoRevision;
import co.edu.univalle.patentsweep.repository.ConflictoRepository;
import co.edu.univalle.patentsweep.repository.EvaluacionFormulaRepository;
import co.edu.univalle.patentsweep.repository.RevisionFormulaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Estado de una fórmula frente a las patentes (HU-04, 3.2). No se guarda: se calcula en cada consulta con
 * los conflictos, las evaluaciones y la última revisión, así que nunca queda desactualizado.
 */
@Service
public class EstadoFormulaService {

    private static final List<String> EVALUACIONES_ABIERTAS = List.of("EN_PROCESO", "PENDIENTE_DE_ENVIO");

    private final MaterialesClient materialesClient;
    private final ConflictoRepository conflictoRepository;
    private final EvaluacionFormulaRepository evaluacionRepository;
    private final RevisionFormulaRepository revisionRepository;

    public EstadoFormulaService(MaterialesClient materialesClient, ConflictoRepository conflictoRepository,
                                EvaluacionFormulaRepository evaluacionRepository,
                                RevisionFormulaRepository revisionRepository) {
        this.materialesClient = materialesClient;
        this.conflictoRepository = conflictoRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.revisionRepository = revisionRepository;
    }

    /** Estado calculado y fecha de la última revisión (nula si la fórmula no se ha revisado o falló). */
    public record EstadoCalculado(EstadoFormula estado, LocalDateTime fechaUltimaRevision) {
    }

    /**
     * Primero verifica en Materiales que la fórmula exista (404 si no) y toma su versión vigente. Después
     * aplica, en orden: conflicto DETECTADO de esa versión, evaluación abierta, nunca revisada o último
     * intento fallido, y en cualquier otro caso LIBRE.
     */
    public EstadoCalculado consultarEstado(String formulaId) {
        FormulaExternaDTO formula = materialesClient.obtenerFormula(formulaId);
        Optional<RevisionFormula> ultima = revisionRepository
                .findFirstByFormulaIdOrderByFechaRevisionDescRevisionIdDesc(formulaId);
        LocalDateTime fecha = ultima.map(RevisionFormula::getFechaRevision).orElse(null);

        if (conflictoRepository.countByFormulaIdAndVersionFormulaAndEstado(
                formulaId, formula.versionRegistrada(), EstadoConflicto.DETECTADO) > 0) {
            return new EstadoCalculado(EstadoFormula.CONFLICTO_DETECTADO, fecha);
        }
        if (evaluacionRepository.existsByFormulaIdAndEstadoIn(formulaId, EVALUACIONES_ABIERTAS)) {
            return new EstadoCalculado(EstadoFormula.PENDIENTE_REVISION, fecha);
        }
        if (ultima.isEmpty() || ultima.get().getResultado() == ResultadoRevision.FALLO_TECNICO) {
            return new EstadoCalculado(EstadoFormula.NO_PROCESADO, null);
        }
        return new EstadoCalculado(EstadoFormula.LIBRE, fecha);
    }
}
