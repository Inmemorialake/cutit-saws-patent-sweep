package co.edu.univalle.patentsweep.service;

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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BarridoServiceTest {

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-11-23T10:15:30Z"), ZoneOffset.UTC);

    @Mock
    private BarridoRepository barridoRepository;

    @Test
    @DisplayName("no inicia otro barrido si ya hay uno en curso")
    void deberiaOmitirUnBarridoEnCurso() {
        Barrido enCurso = Barrido.builder().barridoId(7L).estado(EstadoBarrido.EN_CURSO).build();
        when(barridoRepository.findByEstado(EstadoBarrido.EN_CURSO)).thenReturn(Optional.of(enCurso));
        BarridoService servicio = new BarridoService(barridoRepository, RELOJ);

        assertThat(servicio.ejecutar()).isSameAs(enCurso);
        verify(barridoRepository, never()).save(any());
    }

    @Test
    @DisplayName("registra el inicio con la marca de tiempo del reloj inyectable")
    void deberiaRegistrarElInicio() {
        when(barridoRepository.findByEstado(EstadoBarrido.EN_CURSO)).thenReturn(Optional.empty());
        when(barridoRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        BarridoService servicio = new BarridoService(barridoRepository, RELOJ);

        Barrido barrido = servicio.ejecutar();

        ArgumentCaptor<Barrido> captor = ArgumentCaptor.forClass(Barrido.class);
        verify(barridoRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoBarrido.EN_CURSO);
        assertThat(captor.getValue().getFechaInicio()).isEqualTo(LocalDateTime.of(2026, 11, 23, 10, 15, 30));
        assertThat(barrido.getEstado()).isEqualTo(EstadoBarrido.EN_CURSO);
    }
}
