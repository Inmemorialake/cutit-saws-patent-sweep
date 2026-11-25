package co.edu.univalle.patentsweep.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.univalle.patentsweep.integration.dto.ComponenteExternoDTO;
import co.edu.univalle.patentsweep.integration.dto.FormulaExternaDTO;
import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import co.edu.univalle.patentsweep.model.Conflicto;
import co.edu.univalle.patentsweep.model.Patente;
import co.edu.univalle.patentsweep.model.RevisionFormula;
import co.edu.univalle.patentsweep.model.enums.EstadoConflicto;
import co.edu.univalle.patentsweep.model.enums.EstadoVigenciaPatente;
import co.edu.univalle.patentsweep.model.enums.OrigenRevision;
import co.edu.univalle.patentsweep.model.enums.ResultadoRevision;
import co.edu.univalle.patentsweep.repository.ConflictoRepository;
import co.edu.univalle.patentsweep.repository.PatenteRepository;
import co.edu.univalle.patentsweep.service.event.ConflictoDetectadoEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ConflictoServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PatenteRepository patenteRepository;
    @Mock
    private ConflictoRepository conflictoRepository;
    @Mock
    private HistorialService historialService;
    @Mock
    private ApplicationEventPublisher publicador;

    private ConflictoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ConflictoService(patenteRepository, conflictoRepository, historialService, publicador, RELOJ);
    }

    @Test
    @DisplayName("no duplica una pareja ya DESCARTADA y registra SIN_CONFLICTO si no queda ninguno vigente")
    void noDeberiaDuplicarPorFormulaYPatente() {
        FormulaExternaDTO formula = formula("2");
        Patente patente = Patente.builder().patenteId(5L).codigoPatente("PAT-998822").titulo("Aleación").build();
        Conflicto existente = Conflicto.builder().conflictoId(100001L).formulaId("form-001")
                .patente(patente).versionFormula("1").estado(EstadoConflicto.DESCARTADO).build();
        when(patenteRepository.findByCodigoPatente("PAT-998822")).thenReturn(Optional.of(patente));
        when(conflictoRepository.findByFormulaIdAndPatente_PatenteId("form-001", 5L))
                .thenReturn(Optional.of(existente));
        when(conflictoRepository.countByFormulaIdAndEstado("form-001", EstadoConflicto.DETECTADO)).thenReturn(0L);
        when(historialService.registrar(eq("form-001"), eq("2"), eq("ingeniera"),
                eq(OrigenRevision.BUSQUEDA_MANUAL), eq(ResultadoRevision.SIN_CONFLICTO),
                anyMap(), isNull(), any())).thenReturn(revision());

        List<Conflicto> resultado = servicio.registrarNuevos(formula, "ingeniera", List.of(patente()), Map.of());

        assertThat(resultado).containsExactly(existente);
        verify(conflictoRepository, never()).save(any());
        verify(publicador, never()).publishEvent(any());
    }

    @Test
    @DisplayName("una pareja existente aún DETECTADA mantiene el resultado CON_CONFLICTO")
    void deberiaMantenerConflictoSiLaParejaSigueDetectada() {
        FormulaExternaDTO formula = formula("2");
        Patente patente = Patente.builder().patenteId(5L).codigoPatente("PAT-998822").titulo("Aleación").build();
        Conflicto existente = Conflicto.builder().conflictoId(100001L).formulaId("form-001")
                .patente(patente).versionFormula("2").estado(EstadoConflicto.DETECTADO).build();
        when(patenteRepository.findByCodigoPatente("PAT-998822")).thenReturn(Optional.of(patente));
        when(conflictoRepository.findByFormulaIdAndPatente_PatenteId("form-001", 5L))
                .thenReturn(Optional.of(existente));
        when(conflictoRepository.countByFormulaIdAndEstado("form-001", EstadoConflicto.DETECTADO)).thenReturn(1L);
        when(historialService.registrar(eq("form-001"), eq("2"), eq("ingeniera"),
                eq(OrigenRevision.BUSQUEDA_MANUAL), eq(ResultadoRevision.CON_CONFLICTO),
                anyMap(), isNull(), any())).thenReturn(revision());

        assertThat(servicio.registrarNuevos(formula, "ingeniera", List.of(patente()), Map.of()))
                .containsExactly(existente);
    }

    @Test
    @DisplayName("crea el conflicto DETECTADO y publica el evento solo para el nuevo")
    void deberiaCrearConflictoNuevo() {
        FormulaExternaDTO formula = formula("2");
        Patente guardada = Patente.builder().patenteId(7L).codigoPatente("PAT-998822").titulo("Aleación").build();
        when(patenteRepository.findByCodigoPatente("PAT-998822")).thenReturn(Optional.empty());
        when(patenteRepository.save(any(Patente.class))).thenReturn(guardada);
        when(conflictoRepository.countByFormulaIdAndEstado("form-001", EstadoConflicto.DETECTADO)).thenReturn(0L);
        when(historialService.registrar(eq("form-001"), eq("2"), eq("ingeniera"),
                eq(OrigenRevision.BUSQUEDA_MANUAL), eq(ResultadoRevision.CON_CONFLICTO),
                anyMap(), isNull(), any())).thenReturn(revision());
        when(conflictoRepository.save(any(Conflicto.class))).thenAnswer(invocacion -> {
            Conflicto conflicto = invocacion.getArgument(0);
            conflicto.setConflictoId(100002L);
            return conflicto;
        });

        List<Conflicto> resultado = servicio.registrarNuevos(formula, "ingeniera", List.of(patente()), Map.of());

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoConflicto.DETECTADO);
        assertThat(resultado.get(0).getVersionFormula()).isEqualTo("2");
        verify(publicador).publishEvent(new ConflictoDetectadoEvent(100002L, "form-001", "PAT-998822"));
    }

    @Test
    @DisplayName("sin coincidencias registra SIN_CONFLICTO")
    void deberiaRegistrarSinConflicto() {
        when(conflictoRepository.countByFormulaIdAndEstado("form-001", EstadoConflicto.DETECTADO)).thenReturn(0L);
        when(historialService.registrar(eq("form-001"), eq("2"), eq("ingeniera"),
                eq(OrigenRevision.BUSQUEDA_MANUAL), eq(ResultadoRevision.SIN_CONFLICTO),
                anyMap(), isNull(), any())).thenReturn(revision());

        assertThat(servicio.registrarNuevos(formula("2"), "ingeniera", List.of(), Map.of())).isEmpty();
        verify(conflictoRepository, never()).save(any());
    }

    @Test
    @DisplayName("con conflictos vigentes previos registra CON_CONFLICTO aunque no haya coincidencias nuevas")
    void deberiaConsiderarConflictosVigentesPrevios() {
        when(conflictoRepository.countByFormulaIdAndEstado("form-001", EstadoConflicto.DETECTADO)).thenReturn(1L);
        when(historialService.registrar(eq("form-001"), eq("2"), eq("ingeniera"),
                eq(OrigenRevision.BUSQUEDA_MANUAL), eq(ResultadoRevision.CON_CONFLICTO),
                anyMap(), isNull(), any())).thenReturn(revision());

        assertThat(servicio.registrarNuevos(formula("2"), "ingeniera", List.of(), Map.of())).isEmpty();
    }

    @Test
    @DisplayName("registra el origen BARRIDO_AUTOMATICO cuando lo indica el barrido")
    void deberiaRegistrarOrigenBarrido() {
        when(conflictoRepository.countByFormulaIdAndEstado("form-001", EstadoConflicto.DETECTADO)).thenReturn(0L);
        when(historialService.registrar(eq("form-001"), eq("2"), eq("SISTEMA"),
                eq(OrigenRevision.BARRIDO_AUTOMATICO), eq(ResultadoRevision.SIN_CONFLICTO),
                anyMap(), isNull(), any())).thenReturn(revision());

        assertThat(servicio.registrarNuevos(formula("2"), "SISTEMA",
                OrigenRevision.BARRIDO_AUTOMATICO, List.of(), Map.of())).isEmpty();

        verify(historialService).registrar(eq("form-001"), eq("2"), eq("SISTEMA"),
                eq(OrigenRevision.BARRIDO_AUTOMATICO), eq(ResultadoRevision.SIN_CONFLICTO),
                anyMap(), isNull(), any());
    }

    @Test
    @DisplayName("la patente externa se persiste con todos sus datos")
    void deberiaConvertirLaPatenteExterna() {
        PatenteExternaDTO externa = new PatenteExternaDTO("PAT-998822", "Aleación", "Cadena",
                EstadoVigenciaPatente.CONCEDIDA, java.time.LocalDate.of(2020, 1, 1),
                java.time.LocalDate.of(2024, 3, 20), java.time.LocalDate.of(2040, 3, 20),
                "US", "Resumen", List.of("R1", "R2"), List.of("B27B 17/00"));
        Patente guardada = Patente.builder().patenteId(7L).codigoPatente("PAT-998822").titulo("Aleación").build();
        when(patenteRepository.findByCodigoPatente("PAT-998822")).thenReturn(Optional.empty());
        when(patenteRepository.save(any(Patente.class))).thenReturn(guardada);
        when(conflictoRepository.countByFormulaIdAndEstado("form-001", EstadoConflicto.DETECTADO)).thenReturn(0L);
        when(historialService.registrar(eq("form-001"), eq("2"), eq("ingeniera"),
                eq(OrigenRevision.BUSQUEDA_MANUAL), eq(ResultadoRevision.CON_CONFLICTO),
                anyMap(), isNull(), any())).thenReturn(revision());
        when(conflictoRepository.save(any(Conflicto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        servicio.registrarNuevos(formula("2"), "ingeniera", List.of(externa), Map.of());

        ArgumentCaptor<Patente> captor = ArgumentCaptor.forClass(Patente.class);
        verify(patenteRepository).save(captor.capture());
        assertThat(captor.getValue().getReivindicaciones()).isEqualTo("R1\nR2");
        assertThat(captor.getValue().getFuente()).isEqualTo("FUENTE_EXTERNA");
        assertThat(captor.getValue().getEstadoVigencia()).isEqualTo(EstadoVigenciaPatente.CONCEDIDA);
    }

    private static FormulaExternaDTO formula(String version) {
        return new FormulaExternaDTO("form-001", "Aleación", "Cadena de corte", version, "APROBADA",
                List.of(new ComponenteExternoDTO("SKU-1", "Hierro", BigDecimal.TEN)));
    }

    private static PatenteExternaDTO patente() {
        return new PatenteExternaDTO("PAT-998822", "Aleación", "Cadena", EstadoVigenciaPatente.CONCEDIDA,
                null, null, null, "US", "Resumen", List.of(), List.of("B27B 17/00"));
    }

    private static RevisionFormula revision() {
        return RevisionFormula.builder().revisionId(9L).formulaId("form-001").build();
    }
}
