package co.edu.univalle.patentsweep.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import co.edu.univalle.patentsweep.exception.ConfiguracionNoEncontradaException;
import co.edu.univalle.patentsweep.model.Configuracion;
import co.edu.univalle.patentsweep.repository.ConfiguracionRepository;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfiguracionServiceTest {

    @Mock
    private ConfiguracionRepository configuracionRepository;

    @InjectMocks
    private ConfiguracionService servicio;

    @Test
    @DisplayName("devuelve la configuración activa")
    void deberiaDevolverLaConfiguracionActiva() {
        Configuracion configuracion = Configuracion.builder().activa(true).build();
        when(configuracionRepository.findByActivaTrue()).thenReturn(Optional.of(configuracion));

        assertThat(servicio.criteriosVigentes()).isSameAs(configuracion);
    }

    @Test
    @DisplayName("falla si no hay configuración activa")
    void deberiaFallarSinConfiguracionActiva() {
        when(configuracionRepository.findByActivaTrue()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.criteriosVigentes())
                .isInstanceOf(ConfiguracionNoEncontradaException.class);
    }

    @Test
    @DisplayName("usa un día cuando no hay frecuencia configurada")
    void deberiaUsarUnDiaPorDefecto() {
        when(configuracionRepository.findByActivaTrue()).thenReturn(Optional.empty());

        assertThat(servicio.frecuenciaVigente()).isEqualTo(Duration.ofDays(1));
    }

    @Test
    @DisplayName("interpreta la frecuencia vigente en horas, días y semanas")
    void deberiaLeerLaFrecuenciaVigente() {
        when(configuracionRepository.findByActivaTrue()).thenReturn(Optional.of(
                Configuracion.builder().frecuenciaValor(6).frecuenciaUnidad("HORAS").build()));
        assertThat(servicio.frecuenciaVigente()).isEqualTo(Duration.ofHours(6));

        when(configuracionRepository.findByActivaTrue()).thenReturn(Optional.of(
                Configuracion.builder().frecuenciaValor(2).frecuenciaUnidad("DIAS").build()));
        assertThat(servicio.frecuenciaVigente()).isEqualTo(Duration.ofDays(2));

        when(configuracionRepository.findByActivaTrue()).thenReturn(Optional.of(
                Configuracion.builder().frecuenciaValor(3).frecuenciaUnidad("SEMANAS").build()));
        assertThat(servicio.frecuenciaVigente()).isEqualTo(Duration.ofDays(21));
    }

    @Test
    @DisplayName("una frecuencia inválida cae a un día")
    void deberiaUsarUnDiaAnteUnaUnidadDesconocida() {
        when(configuracionRepository.findByActivaTrue()).thenReturn(Optional.of(
                Configuracion.builder().frecuenciaValor(0).frecuenciaUnidad("OTRA").build()));

        assertThat(servicio.frecuenciaVigente()).isEqualTo(Duration.ofDays(1));
    }
}
