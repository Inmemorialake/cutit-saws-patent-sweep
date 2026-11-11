package co.edu.univalle.patentsweep.repository;

import co.edu.univalle.patentsweep.model.RevisionFormula;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * Historial de revisiones solo de inserción (P18, DA-10): expone guardar y consultar la última;
 * no hay operaciones de actualización ni de borrado.
 */
@org.springframework.stereotype.Repository
public interface RevisionFormulaRepository extends Repository<RevisionFormula, Long> {

    RevisionFormula save(RevisionFormula revision);

    Optional<RevisionFormula> findFirstByFormulaIdOrderByFechaRevisionDescRevisionIdDesc(String formulaId);
}
