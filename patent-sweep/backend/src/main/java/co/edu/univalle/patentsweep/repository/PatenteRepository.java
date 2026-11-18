package co.edu.univalle.patentsweep.repository;

import co.edu.univalle.patentsweep.model.Patente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso a datos de patentes (P04 Repository). */
@Repository
public interface PatenteRepository extends JpaRepository<Patente, Long> {

    /** Spring Data genera la consulta a partir del nombre del método. */
    Optional<Patente> findByCodigoPatente(String codigoPatente);
}
