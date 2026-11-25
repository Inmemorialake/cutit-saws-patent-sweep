package co.edu.univalle.patentsweep.scheduler;

import co.edu.univalle.patentsweep.service.BarridoService;
import co.edu.univalle.patentsweep.service.ConfiguracionService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

/**
 * Planificador dinámico del barrido (HU-01-1A, P08, P22). El {@link Trigger} consulta la frecuencia
 * vigente en cada ciclo; el cambio aplica en la siguiente ejecución, sin reiniciar la API.
 * Con {@code patentsweep.barrido.al-iniciar} el primer barrido corre poco después de arrancar (D-14) en vez
 * de esperar una frecuencia completa: cada despliegue reinicia la API y, si no, el barrido casi nunca llegaría.
 */
@Slf4j
@Component
public class BarridoScheduler implements SchedulingConfigurer {

    private final BarridoService barridoService;
    private final ConfiguracionService configuracionService;
    private final Clock reloj;
    private final BarridoScheduler self;
    private final boolean alIniciar;
    private final Duration retrasoInicial;

    public BarridoScheduler(BarridoService barridoService, ConfiguracionService configuracionService,
                            Clock reloj, @Lazy BarridoScheduler self,
                            @Value("${patentsweep.barrido.al-iniciar:true}") boolean alIniciar,
                            @Value("${patentsweep.barrido.retraso-inicial:30s}") Duration retrasoInicial) {
        this.barridoService = barridoService;
        this.configuracionService = configuracionService;
        this.reloj = reloj;
        this.self = self;
        this.alIniciar = alIniciar;
        this.retrasoInicial = retrasoInicial;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(self::ejecutar, this::siguienteEjecucion);
    }

    /** Tarea bloqueada con ShedLock para que solo una réplica ejecute el barrido. */
    @SchedulerLock(name = "barrido-patentes", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void ejecutar() {
        barridoService.ejecutar();
    }

    Instant siguienteEjecucion(TriggerContext contexto) {
        if (alIniciar && contexto.lastCompletion() == null) {
            log.info("Primer barrido programado {} s después del arranque", retrasoInicial.toSeconds());
            return Instant.now(reloj).plus(retrasoInicial);
        }
        Duration frecuencia = configuracionService.frecuenciaVigente();
        return Instant.now(reloj).plus(frecuencia);
    }
}
