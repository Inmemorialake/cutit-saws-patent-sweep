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
import co.edu.univalle.patentsweep.model.Conflicto;
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
        when(materialesClient.verificarFormula("form-001")).thenReturn(formula("2"));
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formula("2"));
        when(conflictoRepository.findByFormulaIdAndEstadoOrderByFechaDeteccionDesc(anyString(),
                eq(EstadoConflicto.DETECTADO))).thenReturn(List.of());
        when(evaluacionRepository.existsByFormulaIdAndEstadoIn(anyString(), anyCollection())).thenReturn(false);
        when(revisionRepository.findFirstByFormulaIdOrderByFechaRevisionDescRevisionIdDesc("form-001"))
                .thenReturn(Optional.of(revision(ResultadoRevision.SIN_CONFLICTO)));
    }

    @Test
    @DisplayName("Con un conflicto DETECTADO de la versión vigente el estado es CONFLICTO_DETECTADO")
    void deberiaMarcarConflictoDetectadoCuandoHayConflictosVigentes() {
        // Arrange
        List<Conflicto> vigentes = List.of(Conflicto.builder().conflictoId(2L).build(),
                Conflicto.builder().conflictoId(1L).build());
        when(conflictoRepository.findByFormulaIdAndEstadoOrderByFechaDeteccionDesc("form-001",
                EstadoConflicto.DETECTADO)).thenReturn(vigentes);

        // Act
        EstadoFormulaService.EstadoCalculado resultado = servicio.consultarEstado("form-001");

        // Assert
        assertThat(resultado.estado()).isEqualTo(EstadoFormula.CONFLICTO_DETECTADO);
        assertThat(resultado.fechaUltimaRevision()).isEqualTo(FECHA);
        assertThat(resultado.alertasActivas()).isEqualTo(vigentes);
        verify(evaluacionRepository, never()).existsByFormulaIdAndEstadoIn(anyString(), anyCollection());
    }

    @Test
    @DisplayName("recalcular calcula el mismo estado pero consulta a Materiales con la política normal")
    void deberiaRecalcularElMismoEstado() {
        assertThat(servicio.recalcular("form-001")).isEqualTo(servicio.consultarEstado("form-001"));
        verify(materialesClient).obtenerFormula("form-001");
        verify(materialesClient).verificarFormula("form-001");
    }

    @Test
    @DisplayName("Un conflicto detectado con otra versión sigue vigente: la unicidad es por pareja (ADR-012)")
    void deberiaContarConflictosDeOtraVersion() {
        // Arrange
        when(conflictoRepository.findByFormulaIdAndEstadoOrderByFechaDeteccionDesc("form-001",
                EstadoConflicto.DETECTADO))
                .thenReturn(List.of(Conflicto.builder().conflictoId(5L).versionFormula("1").build()));

        // Act
        EstadoFormulaService.EstadoCalculado resultado = servicio.consultarEstado("form-001");

        // Assert
        assertThat(resultado.estado()).isEqualTo(EstadoFormula.CONFLICTO_DETECTADO);
        assertThat(resultado.alertasActivas()).hasSize(1);
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
    @DisplayName("Si la última revisión fue de otra versión, la versión vigente cuenta como no revisada")
    void deberiaMarcarNoProcesadoCuandoLaUltimaRevisionEsDeOtraVersion() {
        // Arrange
        when(revisionRepository.findFirstByFormulaIdOrderByFechaRevisionDescRevisionIdDesc("form-001"))
                .thenReturn(Optional.of(revision(ResultadoRevision.SIN_CONFLICTO, "1")));

        // Act
        EstadoFormulaService.EstadoCalculado resultado = servicio.consultarEstado("form-001");

        // Assert
        assertThat(resultado.estado()).isEqualTo(EstadoFormula.NO_PROCESADO);
        assertThat(resultado.fechaUltimaRevision()).isNull();
    }

    @Test
    @DisplayName("Una revisión anterior sin versión registrada no cubre la versión vigente")
    void deberiaMarcarNoProcesadoCuandoLaRevisionNoTieneVersion() {
        // Arrange
        when(revisionRepository.findFirstByFormulaIdOrderByFechaRevisionDescRevisionIdDesc("form-001"))
                .thenReturn(Optional.of(revision(ResultadoRevision.SIN_CONFLICTO, null)));

        // Act
        EstadoFormulaService.EstadoCalculado resultado = servicio.consultarEstado("form-001");

        // Assert
        assertThat(resultado.estado()).isEqualTo(EstadoFormula.NO_PROCESADO);
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
    @DisplayName("Si Materiales no conoce la fórmula se propaga el 404 sin calcular nada")
    void deberiaPropagarLaFormulaNoEncontrada() {
        // Arrange
        when(materialesClient.verificarFormula("form-404")).thenThrow(new FormulaNoEncontradaException());

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
        return revision(resultado, "2");
    }

    private static RevisionFormula revision(ResultadoRevision resultado, String version) {
        return RevisionFormula.builder().formulaId("form-001").versionFormula(version).fechaRevision(FECHA)
                .resultado(resultado).build();
    }
}
