package co.edu.univalle.patentsweep.repository;

import co.edu.univalle.patentsweep.model.Notificacion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso a datos de notificaciones (P04 Repository). */
@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Optional<Notificacion> findByConflicto_ConflictoId(Long conflictoId);
}
