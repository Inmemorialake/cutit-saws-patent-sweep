package co.edu.univalle.patentsweep.repository;

import co.edu.univalle.patentsweep.model.EvaluacionFormula;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso a datos de las evaluaciones de fórmulas (P04 Repository). */
@Repository
public interface EvaluacionFormulaRepository extends JpaRepository<EvaluacionFormula, UUID> {

    boolean existsByFormulaIdAndEstadoIn(String formulaId, Collection<String> estados);
}
