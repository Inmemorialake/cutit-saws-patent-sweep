package co.edu.univalle.patentsweep.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.univalle.patentsweep.exception.ConfiguracionNoEncontradaException;
import co.edu.univalle.patentsweep.exception.IntegracionExternaException;
import co.edu.univalle.patentsweep.exception.ServicioNoDisponibleException;
import co.edu.univalle.patentsweep.integration.MaterialesClient;
import co.edu.univalle.patentsweep.integration.dto.ComponenteExternoDTO;
import co.edu.univalle.patentsweep.integration.dto.FormulaExternaDTO;
import co.edu.univalle.patentsweep.integration.dto.FormulaResumenDTO;
import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import co.edu.univalle.patentsweep.integration.port.FuentePatentesPort;
import co.edu.univalle.patentsweep.model.Barrido;
import co.edu.univalle.patentsweep.model.Configuracion;
import co.edu.univalle.patentsweep.model.Conflicto;
import co.edu.univalle.patentsweep.model.RevisionFormula;
import co.edu.univalle.patentsweep.model.enums.EstadoBarrido;
import co.edu.univalle.patentsweep.model.enums.EstadoVigenciaPatente;
import co.edu.univalle.patentsweep.model.enums.OrigenRevision;
import co.edu.univalle.patentsweep.model.enums.ResultadoRevision;
import co.edu.univalle.patentsweep.repository.BarridoRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BarridoServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-23T10:15:30Z"), ZoneOffset.UTC);
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 11, 23, 10, 15, 30);

    private final SimpleMeterRegistry metricas = new SimpleMeterRegistry();

    @Mock
    private BarridoRepository barridoRepository;
    @Mock
    private MaterialesClient materialesClient;
    @Mock
    private FuentePatentesPort fuentePatentes;
    @Mock
    private MotorCoincidencias motorCoincidencias;
    @Mock
    private ConfiguracionService configuracionService;
    @Mock
    private ConflictoService conflictoService;
    @Mock
    private HistorialService historialService;
    @Mock
    private EstadoFormulaService estadoFormulaService;

    @Test
    @DisplayName("no inicia otro barrido si ya hay uno en curso")
    void deberiaOmitirUnBarridoEnCurso() {
        Barrido enCurso = Barrido.builder().barridoId(7L).estado(EstadoBarrido.EN_CURSO).build();
        when(barridoRepository.findByEstado(EstadoBarrido.EN_CURSO)).thenReturn(Optional.of(enCurso));

        assertThat(servicio().ejecutar()).isSameAs(enCurso);
        verify(barridoRepository, never()).save(any());
    }

    @Test
    @DisplayName("registra el inicio con la marca de tiempo del reloj inyectable y finaliza vacío")
    void deberiaRegistrarElInicio() {
        permitirGuardado();
        when(materialesClient.listarAprobadas()).thenReturn(List.of());

        Barrido barrido = servicio().ejecutar();

        assertThat(barrido.getEstado()).isEqualTo(EstadoBarrido.COMPLETADO);
        assertThat(barrido.getFechaInicio()).isEqualTo(FIN);
        assertThat(barrido.getMensaje()).isEqualTo("Barrido completado exitosamente");
        verify(materialesClient, never()).obtenerFormula(anyString());
        verifyNoInteractions(configuracionService, fuentePatentes, conflictoService, historialService,
                estadoFormulaService);
    }

    @Test
    @DisplayName("procesa todas las fórmulas y continúa cuando una falla")
    void deberiaProcesarTodasLasFormulas() {
        permitirGuardado();
        when(materialesClient.listarAprobadas()).thenReturn(List.of(
                resumen("form-001"), resumen("form-002"), resumen("form-003"), resumen("form-004")));
        when(configuracionService.criteriosVigentes()).thenReturn(configuracion());
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formula("form-001"));
        when(materialesClient.obtenerFormula("form-002")).thenReturn(formula("form-002"));
        when(materialesClient.obtenerFormula("form-003"))
                .thenThrow(new ServicioNoDisponibleException("timeout de Materiales"));
        when(materialesClient.obtenerFormula("form-004")).thenReturn(formula("form-004"));
        when(fuentePatentes.buscarPatentes(anyList(), anyList(), any()))
                .thenReturn(List.of(patente()))
                .thenReturn(List.of())
                .thenThrow(new ServicioNoDisponibleException("timeout de la fuente"));
        when(motorCoincidencias.terminosDeBusqueda(any())).thenReturn(List.of("niquel"));
        when(motorCoincidencias.filtrarConflictos(any(), any())).thenAnswer(invocacion -> {
            FormulaExternaDTO formula = invocacion.getArgument(0);
            return "form-001".equals(formula.codigo()) ? List.of(patente()) : List.of();
        });
        when(conflictoService.registrarNuevos(any(), eq("SISTEMA"), eq(OrigenRevision.BARRIDO_AUTOMATICO),
                anyList(), anyMap())).thenAnswer(invocacion -> {
                    FormulaExternaDTO formula = invocacion.getArgument(0);
                    return "form-001".equals(formula.codigo()) ? List.of(conflicto()) : List.of();
                });
        when(historialService.registrar(anyString(), anyString(), eq("SISTEMA"),
                eq(OrigenRevision.BARRIDO_AUTOMATICO), eq(ResultadoRevision.FALLO_TECNICO),
                anyMap(), any(), any())).thenReturn(revision());

        Barrido barrido = servicio().ejecutar();

        assertThat(barrido.getEstado()).isEqualTo(EstadoBarrido.COMPLETADO);
        assertThat(barrido.getTotalFormulas()).isEqualTo(4);
        assertThat(barrido.getTotalProcesadas()).isEqualTo(2);
        assertThat(barrido.getTotalConConflicto()).isEqualTo(1);
        assertThat(barrido.getTotalSinConflicto()).isEqualTo(1);
        assertThat(barrido.getNoProcesadas()).isEqualTo(2);
        assertThat(barrido.getMensaje()).isEqualTo("Barrido completado exitosamente");
        assertThat(barrido.getFechaFin()).isEqualTo(FIN);
        assertThat(metricas.get("patentsweep.barrido.formulas").tag("resultado", "con_conflicto")
                .counter().count()).isEqualTo(1.0);
        assertThat(metricas.get("patentsweep.barrido.formulas").tag("resultado", "sin_conflicto")
                .counter().count()).isEqualTo(1.0);
        assertThat(metricas.get("patentsweep.barrido.formulas").tag("resultado", "no_procesado")
                .counter().count()).isEqualTo(2.0);
        assertThat(metricas.find("patentsweep.barrido.duracion").timer()).isNotNull();
        verify(conflictoService, times(2)).registrarNuevos(any(), eq("SISTEMA"),
                eq(OrigenRevision.BARRIDO_AUTOMATICO), anyList(), anyMap());
        verify(historialService, times(2)).registrar(anyString(), anyString(), eq("SISTEMA"),
                eq(OrigenRevision.BARRIDO_AUTOMATICO), eq(ResultadoRevision.FALLO_TECNICO),
                anyMap(), any(), any());
        verify(estadoFormulaService, times(4)).recalcular(anyString());
    }

    @Test
    @DisplayName("marca el barrido como fallido si Materiales rechaza el listado")
    void deberiaFallarSiElListadoEsRechazado() {
        permitirGuardado();
        when(materialesClient.listarAprobadas()).thenThrow(new IntegracionExternaException("Listado inválido"));

        Barrido barrido = servicio().ejecutar();

        assertThat(barrido.getEstado()).isEqualTo(EstadoBarrido.FALLIDO);
        assertThat(barrido.getMensaje()).isEqualTo("Listado inválido");
        assertThat(barrido.getFechaFin()).isEqualTo(FIN);
        verifyNoInteractions(configuracionService, fuentePatentes, conflictoService, historialService,
                estadoFormulaService);
    }

    @Test
    @DisplayName("marca el barrido como fallido si Materiales no está disponible")
    void deberiaFallarSiMaterialesNoResponde() {
        permitirGuardado();
        when(materialesClient.listarAprobadas())
                .thenThrow(new ServicioNoDisponibleException("No se pudo comunicar con el servicio de Materiales"));

        Barrido barrido = servicio().ejecutar();

        assertThat(barrido.getEstado()).isEqualTo(EstadoBarrido.FALLIDO);
        assertThat(barrido.getMensaje())
                .isEqualTo("No se pudo comunicar con el servicio de Materiales/Fórmulas");
        assertThat(barrido.getFechaFin()).isEqualTo(FIN);
        verifyNoInteractions(configuracionService, fuentePatentes, conflictoService, historialService,
                estadoFormulaService);
    }

    @Test
    @DisplayName("marca el barrido como fallido ante una excepción general")
    void deberiaFallarAnteExcepcionGeneral() {
        permitirGuardado();
        when(materialesClient.listarAprobadas()).thenReturn(List.of(resumen("form-001")));
        when(configuracionService.criteriosVigentes()).thenThrow(new IllegalStateException("BD caída"));

        Barrido barrido = servicio().ejecutar();

        assertThat(barrido.getEstado()).isEqualTo(EstadoBarrido.FALLIDO);
        assertThat(barrido.getMensaje()).isEqualTo("Fallo general del barrido: BD caída");
        assertThat(barrido.getFechaFin()).isEqualTo(FIN);
        verifyNoInteractions(fuentePatentes, conflictoService, historialService, estadoFormulaService);
    }

    @Test
    @DisplayName("aplica los criterios iniciales cuando no hay configuración vigente")
    void deberiaAplicarCriteriosIniciales() {
        permitirGuardado();
        when(materialesClient.listarAprobadas()).thenReturn(List.of(resumen("form-001")));
        when(configuracionService.criteriosVigentes()).thenThrow(new ConfiguracionNoEncontradaException());
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formula("form-001"));
        when(fuentePatentes.buscarPatentes(anyList(), anyList(), any())).thenReturn(List.of());
        when(motorCoincidencias.terminosDeBusqueda(any())).thenReturn(List.of("niquel"));
        when(motorCoincidencias.filtrarConflictos(any(), any())).thenReturn(List.of());
        when(conflictoService.registrarNuevos(any(), eq("SISTEMA"), eq(OrigenRevision.BARRIDO_AUTOMATICO),
                anyList(), anyMap())).thenReturn(List.of());

        Barrido barrido = servicio().ejecutar();

        assertThat(barrido.getEstado()).isEqualTo(EstadoBarrido.COMPLETADO);
        verify(fuentePatentes).buscarPatentes(anyList(),
                eq(List.of("B27B 17/00", "B24D 3/00")), eq(LocalDate.of(2021, 11, 23)));
    }

    @Test
    @DisplayName("cuenta como no procesada una fórmula descontinuada")
    void deberiaRegistrarFalloDeFormulaDescontinuada() {
        permitirGuardado();
        when(materialesClient.listarAprobadas()).thenReturn(List.of(resumen("form-001")));
        when(configuracionService.criteriosVigentes()).thenReturn(configuracion());
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formulaDescontinuada("form-001"));
        when(historialService.registrar(anyString(), anyString(), eq("SISTEMA"),
                eq(OrigenRevision.BARRIDO_AUTOMATICO), eq(ResultadoRevision.FALLO_TECNICO),
                anyMap(), any(), any())).thenReturn(revision());

        Barrido barrido = servicio().ejecutar();

        assertThat(barrido.getEstado()).isEqualTo(EstadoBarrido.COMPLETADO);
        assertThat(barrido.getNoProcesadas()).isEqualTo(1);
        assertThat(barrido.getTotalProcesadas()).isZero();
        verifyNoInteractions(fuentePatentes, conflictoService);
    }

    @Test
    @DisplayName("cuenta como no procesada una fórmula cuyo resultado no se puede persistir")
    void deberiaRegistrarFalloAlPersistirElResultado() {
        permitirGuardado();
        when(materialesClient.listarAprobadas()).thenReturn(List.of(resumen("form-001")));
        when(configuracionService.criteriosVigentes()).thenReturn(configuracion());
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formula("form-001"));
        when(fuentePatentes.buscarPatentes(anyList(), anyList(), any())).thenReturn(List.of(patente()));
        when(motorCoincidencias.terminosDeBusqueda(any())).thenReturn(List.of("niquel"));
        when(motorCoincidencias.filtrarConflictos(any(), any())).thenReturn(List.of(patente()));
        when(conflictoService.registrarNuevos(any(), eq("SISTEMA"), eq(OrigenRevision.BARRIDO_AUTOMATICO),
                anyList(), anyMap())).thenThrow(new IllegalStateException("No se pudo guardar el conflicto"));
        when(historialService.registrar(anyString(), anyString(), eq("SISTEMA"),
                eq(OrigenRevision.BARRIDO_AUTOMATICO), eq(ResultadoRevision.FALLO_TECNICO),
                anyMap(), any(), any())).thenReturn(revision());

        Barrido barrido = servicio().ejecutar();

        assertThat(barrido.getEstado()).isEqualTo(EstadoBarrido.COMPLETADO);
        assertThat(barrido.getNoProcesadas()).isEqualTo(1);
        verify(estadoFormulaService).recalcular("form-001");
    }

    @Test
    @DisplayName("guarda el inicio, el cierre y el resultado final del barrido")
    void deberiaGuardarElCicloCompleto() {
        permitirGuardado();
        when(materialesClient.listarAprobadas()).thenReturn(List.of());

        servicio().ejecutar();

        ArgumentCaptor<Barrido> captor = ArgumentCaptor.forClass(Barrido.class);
        verify(barridoRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getEstado()).isEqualTo(EstadoBarrido.EN_CURSO);
        assertThat(captor.getAllValues().get(0).getFechaInicio()).isEqualTo(FIN);
        assertThat(captor.getAllValues().get(1).getEstado()).isEqualTo(EstadoBarrido.COMPLETADO);
        assertThat(captor.getAllValues().get(1).getTotalFormulas()).isZero();
    }

    private BarridoService servicio() {
        return new BarridoService(barridoRepository, materialesClient, fuentePatentes, motorCoincidencias,
                configuracionService, conflictoService, historialService, estadoFormulaService, metricas, RELOJ);
    }

    private void permitirGuardado() {
        when(barridoRepository.findByEstado(EstadoBarrido.EN_CURSO)).thenReturn(Optional.empty());
        when(barridoRepository.save(any())).thenAnswer(invocacion -> copiar(invocacion.getArgument(0)));
    }

    private Barrido copiar(Barrido barrido) {
        return Barrido.builder()
                .barridoId(barrido.getBarridoId())
                .estado(barrido.getEstado())
                .fechaInicio(barrido.getFechaInicio())
                .fechaFin(barrido.getFechaFin())
                .totalFormulas(barrido.getTotalFormulas())
                .totalProcesadas(barrido.getTotalProcesadas())
                .totalConConflicto(barrido.getTotalConConflicto())
                .totalSinConflicto(barrido.getTotalSinConflicto())
                .noProcesadas(barrido.getNoProcesadas())
                .mensaje(barrido.getMensaje())
                .build();
    }

    private FormulaResumenDTO resumen(String codigo) {
        return new FormulaResumenDTO(codigo, "Fórmula " + codigo, "Corte", "APROBADA", null);
    }

    private FormulaExternaDTO formula(String codigo) {
        return new FormulaExternaDTO(codigo, "Fórmula " + codigo, "Corte", "1", "APROBADA",
                List.of(new ComponenteExternoDTO("MAT-1", "Níquel", BigDecimal.valueOf(60))));
    }

    private FormulaExternaDTO formulaDescontinuada(String codigo) {
        return new FormulaExternaDTO(codigo, "Fórmula " + codigo, "Corte", "1", "DESCONTINUADA",
                List.of(new ComponenteExternoDTO("MAT-1", "Níquel", BigDecimal.valueOf(60))));
    }

    private PatenteExternaDTO patente() {
        return new PatenteExternaDTO("PAT-111111", "Aleación con níquel", "Aleación",
                EstadoVigenciaPatente.CONCEDIDA, LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1),
                LocalDate.of(2040, 1, 1), "CO", "Aleación", List.of("Níquel"), List.of("C22C 38/00"));
    }

    private Conflicto conflicto() {
        return Conflicto.builder().conflictoId(1L).build();
    }

    private RevisionFormula revision() {
        return RevisionFormula.builder().revisionId(1L).build();
    }

    private Configuracion configuracion() {
        return Configuracion.builder()
                .frecuenciaValor(1)
                .frecuenciaUnidad("DIAS")
                .frecuenciaHoras(24)
                .rangoCantidad(5)
                .rangoUnidad("ANOS")
                .clasificaciones(List.of("B27B 17/00", "B24D 3/00"))
                .activa(true)
                .vigenteDesde(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();
    }
}
