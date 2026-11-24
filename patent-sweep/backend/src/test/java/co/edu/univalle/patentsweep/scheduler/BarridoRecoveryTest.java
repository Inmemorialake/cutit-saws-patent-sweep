package co.edu.univalle.patentsweep.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.univalle.patentsweep.model.Barrido;
import co.edu.univalle.patentsweep.model.enums.EstadoBarrido;
import co.edu.univalle.patentsweep.repository.BarridoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class BarridoRecoveryTest {

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-11-23T10:15:30Z"), ZoneOffset.UTC);

    @Mock
    private BarridoRepository barridoRepository;

    @Test
    @DisplayName("marca como FALLIDO un barrido huérfano al arrancar")
    void deberiaRecuperarUnBarridoHuerfano() {
        Barrido huerfano = Barrido.builder().barridoId(3L).estado(EstadoBarrido.EN_CURSO).build();
        when(barridoRepository.findByEstado(EstadoBarrido.EN_CURSO)).thenReturn(Optional.of(huerfano));

        new BarridoRecovery(barridoRepository, RELOJ).run(new DefaultApplicationArguments());

        assertThat(huerfano.getEstado()).isEqualTo(EstadoBarrido.FALLIDO);
        assertThat(huerfano.getFechaFin()).isEqualTo(LocalDateTime.of(2026, 11, 23, 10, 15, 30));
        assertThat(huerfano.getMensaje()).contains("huérfano");
        verify(barridoRepository).save(huerfano);
    }

    @Test
    @DisplayName("no hace nada si no hay barridos en curso")
    void deberiaNoHacerNadaSinBarridosEnCurso() {
        when(barridoRepository.findByEstado(EstadoBarrido.EN_CURSO)).thenReturn(Optional.empty());

        new BarridoRecovery(barridoRepository, RELOJ).run(new DefaultApplicationArguments());

        verify(barridoRepository, never()).save(any());
    }
}
