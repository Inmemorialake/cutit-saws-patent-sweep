package co.edu.univalle.patentsweep.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import co.edu.univalle.patentsweep.model.Formula;
import co.edu.univalle.patentsweep.model.enums.EstadoConflicto;
import co.edu.univalle.patentsweep.model.enums.EstadoFormula;
import co.edu.univalle.patentsweep.model.enums.ResultadoRevision;
import co.edu.univalle.patentsweep.repository.ConflictoRepository;
import co.edu.univalle.patentsweep.repository.FormulaRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EstadoFormulaServiceTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 27, 10, 30, 0);

    @Mock
    private ConflictoRepository conflictoRepository;

    @Mock
    private FormulaRepository formulaRepository;

    @InjectMocks
    private EstadoFormulaService servicio;

    @Test
    @DisplayName("Con al menos un conflicto DETECTADO el estado es CONFLICTO_DETECTADO")
    void deberiaMarcarConflictoDetectadoCuandoHayConflictosVigentes() {
        // Arrange
        Formula formula = formula();
        when(conflictoRepository.countByFormula_FormulaIdAndVersionFormulaAndEstado("form-001", "1",
                EstadoConflicto.DETECTADO))
                .thenReturn(2L);
        when(formulaRepository.save(any(Formula.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Formula resultado = servicio.actualizarEstado(formula, "1", ResultadoRevision.CON_CONFLICTO, FECHA);

        // Assert
        assertThat(resultado.getEstado()).isEqualTo(EstadoFormula.CONFLICTO_DETECTADO);
        assertThat(resultado.getFechaUltimaRevision()).isEqualTo(FECHA);
    }

    @Test
    @DisplayName("Sin conflictos vigentes y con fallo técnico el estado es NO_PROCESADO")
    void deberiaMarcarNoProcesadoCuandoLaUltimaRevisionFalloTecnicamente() {
        // Arrange
        Formula formula = formula();
        when(conflictoRepository.countByFormula_FormulaIdAndVersionFormulaAndEstado("form-001", "1",
                EstadoConflicto.DETECTADO))
                .thenReturn(0L);
        when(formulaRepository.save(any(Formula.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Formula resultado = servicio.actualizarEstado(formula, "1", ResultadoRevision.FALLO_TECNICO, FECHA);

        // Assert
        assertThat(resultado.getEstado()).isEqualTo(EstadoFormula.NO_PROCESADO);
    }

    @Test
    @DisplayName("Sin conflictos vigentes y revisión correcta el estado es LIBRE")
    void deberiaMarcarLibreCuandoNoHayConflictosVigentes() {
        // Arrange
        Formula formula = formula();
        when(conflictoRepository.countByFormula_FormulaIdAndVersionFormulaAndEstado("form-001", "1",
                EstadoConflicto.DETECTADO))
                .thenReturn(0L);
        when(formulaRepository.save(any(Formula.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Formula resultado = servicio.actualizarEstado(formula, "1", ResultadoRevision.SIN_CONFLICTO, FECHA);

        // Assert
        assertThat(resultado.getEstado()).isEqualTo(EstadoFormula.LIBRE);
    }

    private static Formula formula() {
        return Formula.builder().formulaId("form-001").nombre("Aleación").estado(EstadoFormula.NO_PROCESADO).build();
    }
}
