package co.edu.univalle.patentsweep.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.univalle.patentsweep.service.BarridoService;
import co.edu.univalle.patentsweep.service.ConfiguracionService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TriggerTask;

@ExtendWith(MockitoExtension.class)
class BarridoSchedulerTest {

    private static final Instant AHORA = Instant.parse("2026-11-23T10:15:30Z");

    @Mock
    private BarridoService barridoService;

    @Mock
    private ConfiguracionService configuracionService;

    @Mock
    private BarridoScheduler self;

    private BarridoScheduler scheduler;

    @BeforeEach
    void preparar() {
        scheduler = new BarridoScheduler(barridoService, configuracionService,
                Clock.fixed(AHORA, ZoneOffset.UTC), self);
    }

    @Test
    @DisplayName("el trigger consulta la frecuencia vigente en cada ciclo")
    void deberiaLeeLaFrecuenciaEnCadaCiclo() {
        when(configuracionService.frecuenciaVigente())
                .thenReturn(Duration.ofHours(6), Duration.ofDays(2));
        TriggerTask tarea = registrarTarea();

        Instant primera = tarea.getTrigger().nextExecution(null);
        Instant segunda = tarea.getTrigger().nextExecution(null);

        assertThat(primera).isEqualTo(AHORA.plus(Duration.ofHours(6)));
        assertThat(segunda).isEqualTo(AHORA.plus(Duration.ofDays(2)));
    }

    @Test
    @DisplayName("usa un día si la configuración no define otra frecuencia")
    void deberiaUsarUnDiaPorDefecto() {
        when(configuracionService.frecuenciaVigente()).thenReturn(Duration.ofDays(1));
        TriggerTask tarea = registrarTarea();

        assertThat(tarea.getTrigger().nextExecution(null))
                .isEqualTo(AHORA.plus(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("la tarea pasa por el proxy para aplicar ShedLock")
    void deberiaInvocarElProxyBloqueado() {
        TriggerTask tarea = registrarTarea();

        tarea.getRunnable().run();

        verify(self).ejecutar();
    }

    @Test
    @DisplayName("delega la ejecución en BarridoService")
    void deberiaDelegarEnElServicio() {
        scheduler.ejecutar();

        verify(barridoService).ejecutar();
    }

    private TriggerTask registrarTarea() {
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        scheduler.configureTasks(registrar);
        assertThat(registrar.getTriggerTaskList()).hasSize(1);
        return registrar.getTriggerTaskList().get(0);
    }
}
