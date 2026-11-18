package co.edu.univalle.patentsweep.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.univalle.patentsweep.exception.PatenteDuplicadaException;
import co.edu.univalle.patentsweep.exception.PatenteNoEncontradaException;
import co.edu.univalle.patentsweep.model.Patente;
import co.edu.univalle.patentsweep.repository.PatenteRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatenteServiceTest {

    @Mock
    private PatenteRepository patenteRepository;

    @InjectMocks
    private PatenteService patenteService;

    @Test
    @DisplayName("Registra exitosamente una patente nueva cuando el código no existe")
    void deberiaRegistrarPatenteExitosamente() {
        // Arrange
        Patente patente = Patente.builder()
                .codigoPatente("PAT-123456")
                .titulo("Aleación de titanio")
                .tipoProducto("Metalurgia")
                .resumen("Resumen de prueba")
                .build();

        when(patenteRepository.findByCodigoPatente("PAT-123456")).thenReturn(Optional.empty());
        when(patenteRepository.save(any(Patente.class))).thenAnswer(invocation -> {
            Patente p = invocation.getArgument(0);
            p.setPatenteId(1L);
            return p;
        });

        // Act
        Patente resultado = patenteService.registrar(patente);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getPatenteId()).isEqualTo(1L);
        assertThat(resultado.getCodigoPatente()).isEqualTo("PAT-123456");
        verify(patenteRepository).save(patente);
    }

    @Test
    @DisplayName("Lanza PatenteDuplicadaException al intentar registrar un código de patente ya existente")
    void deberiaLanzarExcepcionCuandoPatenteYaExiste() {
        // Arrange
        Patente patente = Patente.builder()
                .codigoPatente("PAT-123456")
                .titulo("Aleación duplicada")
                .build();

        when(patenteRepository.findByCodigoPatente("PAT-123456")).thenReturn(Optional.of(patente));

        // Act / Assert
        assertThatThrownBy(() -> patenteService.registrar(patente))
                .isInstanceOf(PatenteDuplicadaException.class)
                .hasMessageContaining("Ya existe una patente registrada con el código: PAT-123456");

        verify(patenteRepository, never()).save(any(Patente.class));
    }

    @Test
    @DisplayName("Obtiene exitosamente una patente por su ID")
    void deberiaObtenerPatentePorIdExitosamente() {
        // Arrange
        Patente patente = Patente.builder()
                .patenteId(1L)
                .codigoPatente("PAT-123456")
                .titulo("Aleación de titanio")
                .build();

        when(patenteRepository.findById(1L)).thenReturn(Optional.of(patente));

        // Act
        Patente resultado = patenteService.obtenerPorId(1L);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getPatenteId()).isEqualTo(1L);
        assertThat(resultado.getTitulo()).isEqualTo("Aleación de titanio");
    }

    @Test
    @DisplayName("Lanza PatenteNoEncontradaException cuando el ID consultado no existe en base de datos")
    void deberiaLanzarExcepcionCuandoIdNoExiste() {
        // Arrange
        when(patenteRepository.findById(99L)).thenReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> patenteService.obtenerPorId(99L))
                .isInstanceOf(PatenteNoEncontradaException.class)
                .hasMessageContaining("Patente no encontrada con ID: 99");
    }
}
