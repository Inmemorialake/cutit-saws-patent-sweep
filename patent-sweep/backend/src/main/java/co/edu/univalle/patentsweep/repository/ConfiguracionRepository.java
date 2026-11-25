package co.edu.univalle.patentsweep.repository;

import co.edu.univalle.patentsweep.model.Configuracion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso a datos de la configuración del barrido (P04 Repository). */
@Repository
public interface ConfiguracionRepository extends JpaRepository<Configuracion, Long> {

    /** Devuelve la única configuración activa (garantizada por un índice único parcial). */
    Optional<Configuracion> findByActivaTrue();
}
