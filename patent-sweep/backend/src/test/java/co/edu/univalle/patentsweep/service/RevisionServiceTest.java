package co.edu.univalle.patentsweep.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.univalle.patentsweep.integration.dto.ComponenteExternoDTO;
import co.edu.univalle.patentsweep.integration.dto.FormulaExternaDTO;
import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import co.edu.univalle.patentsweep.model.Conflicto;
import co.edu.univalle.patentsweep.model.Formula;
import co.edu.univalle.patentsweep.model.Patente;
import co.edu.univalle.patentsweep.model.RevisionFormula;
import co.edu.univalle.patentsweep.model.Usuario;
import co.edu.univalle.patentsweep.model.enums.EstadoConflicto;
import co.edu.univalle.patentsweep.model.enums.EstadoFormula;
import co.edu.univalle.patentsweep.model.enums.EstadoVigenciaPatente;
import co.edu.univalle.patentsweep.model.enums.OrigenRevision;
import co.edu.univalle.patentsweep.model.enums.ResultadoRevision;
import co.edu.univalle.patentsweep.repository.ConflictoRepository;
import co.edu.univalle.patentsweep.repository.FormulaRepository;
import co.edu.univalle.patentsweep.repository.PatenteRepository;
import co.edu.univalle.patentsweep.repository.RevisionFormulaRepository;
import co.edu.univalle.patentsweep.repository.UsuarioRepository;
import co.edu.univalle.patentsweep.service.event.ConflictoDetectadoEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RevisionServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 27, 15, 0, 0);
    private static final Map<String, Object> PARAMETROS = Map.of("clasificaciones", List.of("B27B 17/00"));

    @Mock
    private FormulaRepository formulaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PatenteRepository patenteRepository;
    @Mock
    private ConflictoRepository conflictoRepository;
    @Mock
    private RevisionFormulaRepository revisionRepository;
    @Mock
    private EstadoFormulaService estadoFormulaService;
    @Mock
    private ApplicationEventPublisher publicador;

    private RevisionService servicio;

    @BeforeEach
    void preparar() {
        servicio = new RevisionService(formulaRepository, usuarioRepository, patenteRepository,
                conflictoRepository, revisionRepository, estadoFormulaService, publicador, RELOJ);
        when(formulaRepository.save(any(Formula.class))).thenAnswer(i -> i.getArgument(0));
        when(revisionRepository.save(any(RevisionFormula.class))).thenAnswer(i -> i.getArgument(0));
        when(patenteRepository.save(any(Patente.class))).thenAnswer(i -> {
            Patente patente = i.getArgument(0);
            patente.setPatenteId(7L);
            return patente;
        });
        when(conflictoRepository.save(any(Conflicto.class))).thenAnswer(i -> {
            Conflicto conflicto = i.getArgument(0);
            conflicto.setConflictoId(100001L);
            return conflicto;
        });
        when(conflictoRepository.countByFormula_FormulaIdAndVersionFormulaAndEstado(anyString(), eq("1"),
                eq(EstadoConflicto.DETECTADO)))
                .thenReturn(0L);
        when(usuarioRepository.findByUsername("ingeniera"))
                .thenReturn(Optional.of(Usuario.builder().username("ingeniera").build()));
        when(formulaRepository.findById(anyString())).thenReturn(Optional.empty());
        when(patenteRepository.findByCodigoPatente(anyString())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Una patente nueva crea la patente, el conflicto DETECTADO, el historial y publica el evento")
    void deberiaRegistrarConflictoNuevoYPublicarEvento() {
        // Arrange
        FormulaExternaDTO formulaExterna = formulaExterna("APROBADA");

        // Act
        List<Conflicto> resultado = servicio.registrarBusqueda(formulaExterna, "ingeniera",
                List.of(patenteExterna("PAT-998822", List.of("1. Una aleación.", "2. Uso en cadenas."))), PARAMETROS);

        // Assert
        assertThat(resultado).hasSize(1);
        Conflicto conflicto = resultado.get(0);
        assertThat(conflicto.getEstado()).isEqualTo(EstadoConflicto.DETECTADO);
        assertThat(conflicto.getFechaDeteccion()).isEqualTo(AHORA);
        assertThat(conflicto.getPatente().getCodigoPatente()).isEqualTo("PAT-998822");
        assertThat(conflicto.getPatente().getReivindicaciones()).isEqualTo("1. Una aleación.\n2. Uso en cadenas.");

        ArgumentCaptor<RevisionFormula> revision = ArgumentCaptor.forClass(RevisionFormula.class);
        verify(revisionRepository).save(revision.capture());
        assertThat(revision.getValue().getOrigen()).isEqualTo(OrigenRevision.BUSQUEDA_MANUAL);
        assertThat(revision.getValue().getResultado()).isEqualTo(ResultadoRevision.CON_CONFLICTO);
        assertThat(revision.getValue().getUsuario().getUsername()).isEqualTo("ingeniera");
        assertThat(revision.getValue().getParametros()).isEqualTo(PARAMETROS);
        assertThat(revision.getValue().getMensajeError()).isNull();

        ArgumentCaptor<ConflictoDetectadoEvent> evento = ArgumentCaptor.forClass(ConflictoDetectadoEvent.class);
        verify(publicador).publishEvent(evento.capture());
        assertThat(evento.getValue().conflictoId()).isEqualTo(100001L);
        assertThat(evento.getValue().formulaId()).isEqualTo("form-001");
        assertThat(evento.getValue().patenteId()).isEqualTo("PAT-998822");
        verify(estadoFormulaService).actualizarEstado(any(Formula.class), eq("1"), eq(ResultadoRevision.CON_CONFLICTO),
                eq(AHORA));
    }

    @Test
    @DisplayName("La fórmula nueva se copia de Materiales y la DESCONTINUADA queda inactiva")
    void deberiaSincronizarLaFormulaDesdeMateriales() {
        // Arrange
        FormulaExternaDTO descontinuada = formulaExterna("DESCONTINUADA");

        // Act
        servicio.registrarBusqueda(descontinuada, "ingeniera", List.of(patenteExterna("PAT-000001", null)), PARAMETROS);

        // Assert
        ArgumentCaptor<Formula> formula = ArgumentCaptor.forClass(Formula.class);
        verify(formulaRepository).save(formula.capture());
        assertThat(formula.getValue().getFormulaId()).isEqualTo("form-001");
        assertThat(formula.getValue().getNombre()).isEqualTo("Aleación Cuchilla Diamante A");
        assertThat(formula.getValue().getDescripcion()).isEqualTo("Cadena de corte");
        assertThat(formula.getValue().isActivo()).isFalse();
        assertThat(formula.getValue().getEstado()).isEqualTo(EstadoFormula.NO_PROCESADO);
    }

    @Test
    @DisplayName("Si el conflicto ya existe y fue DESCARTADO no se duplica y el resultado es SIN_CONFLICTO")
    void noDeberiaDuplicarUnConflictoExistenteDescartado() {
        // Arrange
        Formula formulaLocal = Formula.builder().formulaId("form-001").estado(EstadoFormula.LIBRE).build();
        Patente patente = Patente.builder().patenteId(7L).codigoPatente("PAT-998822").build();
        Conflicto descartado = Conflicto.builder().conflictoId(100050L).patente(patente)
                .estado(EstadoConflicto.DESCARTADO).build();
        when(formulaRepository.findById("form-001")).thenReturn(Optional.of(formulaLocal));
        when(patenteRepository.findByCodigoPatente("PAT-998822")).thenReturn(Optional.of(patente));
        when(conflictoRepository.findByFormula_FormulaIdAndPatente_PatenteIdAndVersionFormula("form-001", 7L, "1"))
                .thenReturn(Optional.of(descartado));

        // Act
        List<Conflicto> resultado = servicio.registrarBusqueda(formulaExterna("APROBADA"), "ingeniera",
                List.of(patenteExterna("PAT-998822", List.of("1. Una aleación."))), PARAMETROS);

        // Assert
        assertThat(resultado).containsExactly(descartado);
        verify(conflictoRepository, never()).save(any(Conflicto.class));
        verify(publicador, never()).publishEvent(any(Object.class));
        verify(patenteRepository, never()).save(any(Patente.class));
        verify(estadoFormulaService).actualizarEstado(formulaLocal, "1", ResultadoRevision.SIN_CONFLICTO, AHORA);
    }

    @Test
    @DisplayName("Si el conflicto existente sigue DETECTADO el resultado es CON_CONFLICTO sin crear otro")
    void deberiaReportarConflictoVigenteExistente() {
        // Arrange
        Patente patente = Patente.builder().patenteId(7L).codigoPatente("PAT-998822").build();
        Conflicto vigente = Conflicto.builder().conflictoId(100050L).patente(patente)
                .estado(EstadoConflicto.DETECTADO).build();
        when(patenteRepository.findByCodigoPatente("PAT-998822")).thenReturn(Optional.of(patente));
        when(conflictoRepository.findByFormula_FormulaIdAndPatente_PatenteIdAndVersionFormula("form-001", 7L, "1"))
                .thenReturn(Optional.of(vigente));
        when(conflictoRepository.countByFormula_FormulaIdAndVersionFormulaAndEstado("form-001", "1",
                EstadoConflicto.DETECTADO))
                .thenReturn(1L);

        // Act
        List<Conflicto> resultado = servicio.registrarBusqueda(formulaExterna("APROBADA"), "ingeniera",
                List.of(patenteExterna("PAT-998822", List.of())), PARAMETROS);

        // Assert
        assertThat(resultado).containsExactly(vigente);
        verify(conflictoRepository, never()).save(any(Conflicto.class));
        verify(estadoFormulaService).actualizarEstado(any(Formula.class), eq("1"), eq(ResultadoRevision.CON_CONFLICTO),
                eq(AHORA));
    }

    @Test
    @DisplayName("Una patente ya guardada sin conflicto para esta fórmula crea solo el conflicto")
    void deberiaCrearElConflictoCuandoLaPatenteYaExisteSinConflicto() {
        // Arrange
        Patente patente = Patente.builder().patenteId(7L).codigoPatente("PAT-998822").build();
        when(patenteRepository.findByCodigoPatente("PAT-998822")).thenReturn(Optional.of(patente));
        when(conflictoRepository.findByFormula_FormulaIdAndPatente_PatenteIdAndVersionFormula(anyString(), anyLong(),
                anyString()))
                .thenReturn(Optional.empty());

        // Act
        List<Conflicto> resultado = servicio.registrarBusqueda(formulaExterna("APROBADA"), "ingeniera",
                List.of(patenteExterna("PAT-998822", List.of())), PARAMETROS);

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getPatente()).isSameAs(patente);
        verify(patenteRepository, never()).save(any(Patente.class));
        verify(publicador).publishEvent(any(ConflictoDetectadoEvent.class));
    }

    @Test
    @DisplayName("La misma patente con otra versión de la fórmula crea un conflicto nuevo (DA-17)")
    void deberiaCrearUnConflictoNuevoParaOtraVersionDeLaFormula() {
        // Arrange
        Patente patente = Patente.builder().patenteId(7L).codigoPatente("PAT-998822").build();
        when(patenteRepository.findByCodigoPatente("PAT-998822")).thenReturn(Optional.of(patente));
        when(conflictoRepository.findByFormula_FormulaIdAndPatente_PatenteIdAndVersionFormula("form-001", 7L, "2"))
                .thenReturn(Optional.empty());

        // Act
        List<Conflicto> resultado = servicio.registrarBusqueda(formulaExterna("APROBADA", "2"), "ingeniera",
                List.of(patenteExterna("PAT-998822", List.of())), PARAMETROS);

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getVersionFormula()).isEqualTo("2");
        assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoConflicto.DETECTADO);
        verify(publicador).publishEvent(any(ConflictoDetectadoEvent.class));
        verify(estadoFormulaService).actualizarEstado(any(Formula.class), eq("2"),
                eq(ResultadoRevision.CON_CONFLICTO), eq(AHORA));
    }

    @Test
    @DisplayName("Una fórmula sin versión se registra con la versión SIN_VERSION")
    void deberiaUsarSinVersionCuandoLaFormulaNoTraeVersion() {
        // Arrange / Act
        List<Conflicto> resultado = servicio.registrarBusqueda(formulaExterna("APROBADA", null), "ingeniera",
                List.of(patenteExterna("PAT-998822", List.of())), PARAMETROS);

        // Assert
        assertThat(resultado.get(0).getVersionFormula()).isEqualTo("SIN_VERSION");
        verify(estadoFormulaService).actualizarEstado(any(Formula.class), eq("SIN_VERSION"),
                eq(ResultadoRevision.CON_CONFLICTO), eq(AHORA));
    }

    @Test
    @DisplayName("Sin coincidencias el resultado es SIN_CONFLICTO y se registra igual el historial")
    void deberiaRegistrarHistorialSinConflictosCuandoNoHayCoincidencias() {
        // Arrange
        FormulaExternaDTO formulaExterna = formulaExterna("APROBADA");

        // Act
        List<Conflicto> resultado = servicio.registrarBusqueda(formulaExterna, "ingeniera", List.of(), PARAMETROS);

        // Assert
        assertThat(resultado).isEmpty();
        verify(estadoFormulaService).actualizarEstado(any(Formula.class), eq("1"), eq(ResultadoRevision.SIN_CONFLICTO),
                eq(AHORA));
        verify(publicador, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Sin coincidencias pero con otro conflicto vigente en la fórmula el resultado es CON_CONFLICTO")
    void deberiaReportarConflictoCuandoLaFormulaTieneOtroConflictoVigente() {
        // Arrange
        when(conflictoRepository.countByFormula_FormulaIdAndVersionFormulaAndEstado("form-001", "1",
                EstadoConflicto.DETECTADO))
                .thenReturn(3L);

        // Act
        servicio.registrarBusqueda(formulaExterna("APROBADA"), "ingeniera", List.of(), PARAMETROS);

        // Assert
        verify(estadoFormulaService).actualizarEstado(any(Formula.class), eq("1"), eq(ResultadoRevision.CON_CONFLICTO),
                eq(AHORA));
    }

    @Test
    @DisplayName("Un usuario desconocido deja el historial sin usuario (se muestra como SISTEMA)")
    void deberiaRegistrarHistorialSinUsuarioCuandoNoExiste() {
        // Arrange
        when(usuarioRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        // Act
        servicio.registrarBusqueda(formulaExterna("APROBADA"), "fantasma", List.of(), PARAMETROS);

        // Assert
        ArgumentCaptor<RevisionFormula> revision = ArgumentCaptor.forClass(RevisionFormula.class);
        verify(revisionRepository).save(revision.capture());
        assertThat(revision.getValue().getUsuario()).isNull();
    }

    @Test
    @DisplayName("Un fallo de la fuente registra FALLO_TECNICO y deja la fórmula en NO_PROCESADO")
    void deberiaRegistrarFalloTecnico() {
        // Arrange
        FormulaExternaDTO formulaExterna = formulaExterna("APROBADA");

        // Act
        servicio.registrarFalloTecnico(formulaExterna, "ingeniera", "La fuente no respondió", PARAMETROS);

        // Assert
        ArgumentCaptor<RevisionFormula> revision = ArgumentCaptor.forClass(RevisionFormula.class);
        verify(revisionRepository).save(revision.capture());
        assertThat(revision.getValue().getResultado()).isEqualTo(ResultadoRevision.FALLO_TECNICO);
        assertThat(revision.getValue().getMensajeError()).isEqualTo("La fuente no respondió");
        verify(estadoFormulaService).actualizarEstado(any(Formula.class), eq("1"), eq(ResultadoRevision.FALLO_TECNICO),
                eq(AHORA));
    }

    private static FormulaExternaDTO formulaExterna(String estado) {
        return formulaExterna(estado, "1");
    }

    private static FormulaExternaDTO formulaExterna(String estado, String version) {
        return new FormulaExternaDTO("form-001", "Aleación Cuchilla Diamante A", "Cadena de corte", version, estado,
                List.of(new ComponenteExternoDTO("SKU-1", "Hierro", BigDecimal.TEN)));
    }

    private static PatenteExternaDTO patenteExterna(String codigo, List<String> reivindicaciones) {
        return new PatenteExternaDTO(codigo, "Aleación para cadenas de corte", "Aleación metálica",
                EstadoVigenciaPatente.CONCEDIDA, LocalDate.of(2023, 3, 15), LocalDate.of(2025, 6, 22),
                LocalDate.of(2043, 3, 15), "US", "Resumen", reivindicaciones, List.of("B27B 17/00"));
    }
}
