package co.edu.univalle.patentsweep.service;

import co.edu.univalle.patentsweep.model.Barrido;
import co.edu.univalle.patentsweep.model.enums.EstadoBarrido;
import co.edu.univalle.patentsweep.repository.BarridoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Orquestación del barrido periódico (HU-01, P08/P13/P14). El scheduler solo delega aquí; las
 * reglas de negocio y los cambios de estado viven en este servicio.
 */
@Slf4j
@Service
public class BarridoService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final BarridoRepository barridoRepository;
    private final Clock reloj;

    public BarridoService(BarridoRepository barridoRepository, Clock reloj) {
        this.barridoRepository = barridoRepository;
        this.reloj = reloj;
    }

    /** Inicia una ejecución única y devuelve el registro `EN_CURSO` creado. */
    public Barrido ejecutar() {
        Optional<Barrido> enCurso = barridoRepository.findByEstado(EstadoBarrido.EN_CURSO);
        if (enCurso.isPresent()) {
            log.warn("Barrido omitido: existe una ejecución en curso");
            return enCurso.get();
        }

        MDC.put("traceId", UUID.randomUUID().toString());
        try {
            LocalDateTime inicio = LocalDateTime.now(reloj);
            Barrido barrido = barridoRepository.save(Barrido.builder()
                    .estado(EstadoBarrido.EN_CURSO)
                    .fechaInicio(inicio)
                    .build());
            log.info("Inicio del barrido: {}", FORMATO_FECHA.format(inicio));
            return barrido;
        } finally {
            MDC.remove("traceId");
        }
    }
}
