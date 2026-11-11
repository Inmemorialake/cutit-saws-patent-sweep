package co.edu.univalle.patentsweep.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.univalle.patentsweep.exception.FormulaNoEncontradaException;
import co.edu.univalle.patentsweep.integration.MaterialesClient;
import co.edu.univalle.patentsweep.integration.dto.FormulaExternaDTO;
import co.edu.univalle.patentsweep.model.RevisionFormula;
import co.edu.univalle.patentsweep.model.enums.EstadoConflicto;
import co.edu.univalle.patentsweep.model.enums.EstadoFormula;
import co.edu.univalle.patentsweep.model.enums.ResultadoRevision;
import co.edu.univalle.patentsweep.repository.ConflictoRepository;
import co.edu.univalle.patentsweep.repository.EvaluacionFormulaRepository;
import co.edu.univalle.patentsweep.repository.RevisionFormulaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EstadoFormulaServiceTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 27, 15, 0, 0);

    @Mock
    private MaterialesClient materialesClient;
    @Mock
    private ConflictoRepository conflictoRepository;
    @Mock
    private EvaluacionFormulaRepository evaluacionRepository;
    @Mock
    private RevisionFormulaRepository revisionRepository;

    @InjectMocks
    private EstadoFormulaService servicio;

    @BeforeEach
    void preparar() {
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formula("2"));
        when(conflictoRepository.countByFormulaIdAndVersionFormulaAndEstado(anyString(), anyString(),
                eq(EstadoConflicto.DETECTADO))).thenReturn(0L);
        when(evaluacionRepository.existsByFormulaIdAndEstadoIn(anyString(), anyCollection())).thenReturn(false);
        when(revisionRepository.findFirstByFormulaIdOrderByFechaRevisionDescRevisionIdDesc("form-001"))
                .thenReturn(Optional.of(revision(ResultadoRevision.SIN_CONFLICTO)));
    }

    @Test
    @DisplayName("Con un conflicto DETECTADO de la versión vigente el estado es CONFLICTO_DETECTADO")
    void deberiaMarcarConflictoDetectadoCuandoHayConflictosVigentes() {
        // Arrange
        when(conflictoRepository.countByFormulaIdAndVersionFormulaAndEstado("form-001", "2",
                EstadoConflicto.DETECTADO)).thenReturn(2L);

        // Act
        EstadoFormulaService.EstadoCalculado resultado = servicio.consultarEstado("form-001");

        // Assert
        assertThat(resultado.estado()).isEqualTo(EstadoFormula.CONFLICTO_DETECTADO);
        assertThat(resultado.fechaUltimaRevision()).isEqualTo(FECHA);
        verify(evaluacionRepository, never()).existsByFormulaIdAndEstadoIn(anyString(), anyCollection());
    }

    @Test
    @DisplayName("Un conflicto de otra versión no cuenta: se mira solo la versión vigente (DA-17)")
    void noDeberiaContarConflictosDeOtraVersion() {
        // Arrange
        when(conflictoRepository.countByFormulaIdAndVersionFormulaAndEstado("form-001", "1",
                EstadoConflicto.DETECTADO)).thenReturn(5L);

        // Act
        EstadoFormulaService.EstadoCalculado resultado = servicio.consultarEstado("form-001");

        // Assert
        assertThat(resultado.estado()).isEqualTo(EstadoFormula.LIBRE);
    }

    @Test
    @DisplayName("Sin conflictos y con una evaluación abierta el estado es PENDIENTE_REVISION")
    void deberiaMarcarPendienteRevisionConUnaEvaluacionAbierta() {
        // Arrange
        when(evaluacionRepository.existsByFormulaIdAndEstadoIn("form-001",
                List.of("EN_PROCESO", "PENDIENTE_DE_ENVIO"))).thenReturn(true);

        // Act
        EstadoFormulaService.EstadoCalculado resultado = servicio.consultarEstado("form-001");

        // Assert
        assertThat(resultado.estado()).isEqualTo(EstadoFormula.PENDIENTE_REVISION);
        assertThat(resultado.fechaUltimaRevision()).isEqualTo(FECHA);
    }

    @Test
    @DisplayName("Una fórmula que nunca se revisó es NO_PROCESADO y sin fecha de revisión")
    void deberiaMarcarNoProcesadoCuandoNuncaSeReviso() {
        // Arrange
        when(revisionRepository.findFirstByFormulaIdOrderByFechaRevisionDescRevisionIdDesc("form-001"))
                .thenReturn(Optional.empty());

        // Act
        EstadoFormulaService.EstadoCalculado resultado = servicio.consultarEstado("form-001");

        // Assert
        assertThat(resultado.estado()).isEqualTo(EstadoFormula.NO_PROCESADO);
        assertThat(resultado.fechaUltimaRevision()).isNull();
    }

    @Test
    @DisplayName("Si la última revisión falló técnicamente el estado es NO_PROCESADO y sin fecha")
    void deberiaMarcarNoProcesadoCuandoLaUltimaRevisionFalloTecnicamente() {
        // Arrange
        when(revisionRepository.findFirstByFormulaIdOrderByFechaRevisionDescRevisionIdDesc("form-001"))
                .thenReturn(Optional.of(revision(ResultadoRevision.FALLO_TECNICO)));

        // Act
        EstadoFormulaService.EstadoCalculado resultado = servicio.consultarEstado("form-001");

        // Assert
        assertThat(resultado.estado()).isEqualTo(EstadoFormula.NO_PROCESADO);
        assertThat(resultado.fechaUltimaRevision()).isNull();
    }

    @Test
    @DisplayName("Revisada, sin conflictos ni evaluaciones abiertas el estado es LIBRE")
    void deberiaMarcarLibreCuandoNoHayNadaPendiente() {
        // Act
        EstadoFormulaService.EstadoCalculado resultado = servicio.consultarEstado("form-001");

        // Assert
        assertThat(resultado.estado()).isEqualTo(EstadoFormula.LIBRE);
        assertThat(resultado.fechaUltimaRevision()).isEqualTo(FECHA);
    }

    @Test
    @DisplayName("Una fórmula sin versión se consulta con la versión SIN_VERSION")
    void deberiaConsultarConSinVersionCuandoMaterialesNoLaTrae() {
        // Arrange
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formula(null));
        when(conflictoRepository.countByFormulaIdAndVersionFormulaAndEstado("form-001", "SIN_VERSION",
                EstadoConflicto.DETECTADO)).thenReturn(1L);

        // Act
        EstadoFormulaService.EstadoCalculado resultado = servicio.consultarEstado("form-001");

        // Assert
        assertThat(resultado.estado()).isEqualTo(EstadoFormula.CONFLICTO_DETECTADO);
    }

    @Test
    @DisplayName("Si Materiales no conoce la fórmula se propaga el 404 sin calcular nada")
    void deberiaPropagarLaFormulaNoEncontrada() {
        // Arrange
        when(materialesClient.obtenerFormula("form-404")).thenThrow(new FormulaNoEncontradaException());

        // Act / Assert
        assertThatThrownBy(() -> servicio.consultarEstado("form-404"))
                .isInstanceOf(FormulaNoEncontradaException.class);
        verify(revisionRepository, never())
                .findFirstByFormulaIdOrderByFechaRevisionDescRevisionIdDesc(anyString());
    }

    private static FormulaExternaDTO formula(String version) {
        return new FormulaExternaDTO("form-001", "Aleación", "Cadena de corte", version, "APROBADA", List.of());
    }

    private static RevisionFormula revision(ResultadoRevision resultado) {
        return RevisionFormula.builder().formulaId("form-001").fechaRevision(FECHA).resultado(resultado).build();
    }
}
