package co.edu.univalle.patentsweep.repository;

import co.edu.univalle.patentsweep.model.Barrido;
import co.edu.univalle.patentsweep.model.enums.EstadoBarrido;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso a las ejecuciones del barrido (P04 Repository). */
@Repository
public interface BarridoRepository extends JpaRepository<Barrido, Long> {

    /** Permite detectar una ejecución en curso antes de iniciar otra (P13). */
    Optional<Barrido> findByEstado(EstadoBarrido estado);
}
