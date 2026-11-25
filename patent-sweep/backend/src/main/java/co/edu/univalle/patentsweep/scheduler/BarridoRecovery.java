package co.edu.univalle.patentsweep.scheduler;

import co.edu.univalle.patentsweep.model.enums.EstadoBarrido;
import co.edu.univalle.patentsweep.repository.BarridoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Recupera un barrido `EN_CURSO` huérfano tras una caída (HU-01-1A, P17). */
@Slf4j
@Component
public class BarridoRecovery implements ApplicationRunner {

    private static final String MENSAJE = "Barrido huérfano marcado como FALLIDO tras el arranque";

    private final BarridoRepository barridoRepository;
    private final Clock reloj;

    public BarridoRecovery(BarridoRepository barridoRepository, Clock reloj) {
        this.barridoRepository = barridoRepository;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        barridoRepository.findByEstado(EstadoBarrido.EN_CURSO).ifPresent(huerfano -> {
            huerfano.setEstado(EstadoBarrido.FALLIDO);
            huerfano.setFechaFin(LocalDateTime.now(reloj));
            huerfano.setMensaje(MENSAJE);
            barridoRepository.save(huerfano);
            log.warn(MENSAJE);
        });
    }
}
