package co.edu.univalle.patentsweep.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.univalle.patentsweep.exception.ConfiguracionNoEncontradaException;
import co.edu.univalle.patentsweep.exception.FormulaDescontinuadaException;
import co.edu.univalle.patentsweep.exception.FormulaNoEncontradaException;
import co.edu.univalle.patentsweep.exception.ServicioNoDisponibleException;
import co.edu.univalle.patentsweep.integration.MaterialesClient;
import co.edu.univalle.patentsweep.integration.dto.ComponenteExternoDTO;
import co.edu.univalle.patentsweep.integration.dto.FormulaExternaDTO;
import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import co.edu.univalle.patentsweep.integration.port.FuentePatentesPort;
import co.edu.univalle.patentsweep.model.Conflicto;
import co.edu.univalle.patentsweep.model.Configuracion;
import co.edu.univalle.patentsweep.model.enums.EstadoVigenciaPatente;
import co.edu.univalle.patentsweep.model.enums.OrigenRevision;
import co.edu.univalle.patentsweep.model.enums.ResultadoRevision;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusquedaServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZoneOffset.UTC);
    private static final List<String> CLASIFICACIONES = List.of("B27B 17/00", "B24D 3/00");

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

    private BusquedaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new BusquedaService(materialesClient, fuentePatentes, motorCoincidencias,
                configuracionService, conflictoService, historialService, estadoFormulaService, RELOJ);
    }

    @Test
    @DisplayName("filtra las candidatas y registra los conflictos con el usuario del token")
    void deberiaBuscarFiltrarYRegistrar() {
        FormulaExternaDTO formula = formula("APROBADA");
        PatenteExternaDTO candidata = patente();
        Conflicto conflicto = Conflicto.builder().conflictoId(100001L).build();
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formula);
        when(configuracionService.criteriosVigentes()).thenReturn(configuracion(5, "ANOS"));
        when(motorCoincidencias.terminosDeBusqueda(formula)).thenReturn(List.of("Hierro"));
        when(fuentePatentes.buscarPatentes(List.of("Hierro"), CLASIFICACIONES, LocalDate.of(2021, 10, 27)))
                .thenReturn(List.of(candidata));
        when(motorCoincidencias.filtrarConflictos(formula, List.of(candidata))).thenReturn(List.of(candidata));
        when(conflictoService.registrarNuevos(eq(formula), eq("ingeniera"), eq(List.of(candidata)), anyMap()))
                .thenReturn(List.of(conflicto));

        List<Conflicto> resultado = servicio.buscar("form-001", "ingeniera");

        assertThat(resultado).containsExactly(conflicto);
        verify(estadoFormulaService).recalcular("form-001");
        ArgumentCaptor<Map<String, Object>> parametros = ArgumentCaptor.captor();
        verify(conflictoService).registrarNuevos(eq(formula), eq("ingeniera"), anyList(), parametros.capture());
        assertThat(parametros.getValue())
                .containsEntry("clasificaciones", CLASIFICACIONES)
                .containsEntry("fechaSolicitudDesde", "2021-10-27")
                .containsEntry("terminosBusqueda", List.of("Hierro"))
                .containsEntry("patentesCandidatas", List.of(candidata.patenteId()))
                .containsEntry("formula", formula.foto());
    }

    @Test
    @DisplayName("el rango en MESES se cuenta hacia atrás desde hoy")
    void deberiaCalcularLaFechaDesdeEnMeses() {
        FormulaExternaDTO formula = formula("APROBADA");
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formula);
        when(configuracionService.criteriosVigentes()).thenReturn(configuracion(6, "MESES"));
        when(motorCoincidencias.terminosDeBusqueda(formula)).thenReturn(List.of());
        when(fuentePatentes.buscarPatentes(anyList(), anyList(), any())).thenReturn(List.of());
        when(motorCoincidencias.filtrarConflictos(formula, List.of())).thenReturn(List.of());
        when(conflictoService.registrarNuevos(any(), any(), any(), any())).thenReturn(List.of());

        servicio.buscar("form-001", "ingeniera");

        verify(fuentePatentes).buscarPatentes(List.of(), CLASIFICACIONES, LocalDate.of(2026, 4, 27));
    }

    @Test
    @DisplayName("sin rango configurado la búsqueda no se limita por fecha")
    void noDeberiaLimitarPorFechaCuandoNoHayRango() {
        FormulaExternaDTO formula = formula("APROBADA");
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formula);
        when(configuracionService.criteriosVigentes()).thenReturn(configuracion(null, null));
        when(motorCoincidencias.terminosDeBusqueda(formula)).thenReturn(List.of());
        when(fuentePatentes.buscarPatentes(anyList(), anyList(), isNull())).thenReturn(List.of());
        when(motorCoincidencias.filtrarConflictos(formula, List.of())).thenReturn(List.of());
        when(conflictoService.registrarNuevos(any(), any(), any(), any())).thenReturn(List.of());

        servicio.buscar("form-001", "ingeniera");

        ArgumentCaptor<Map<String, Object>> parametros = ArgumentCaptor.captor();
        verify(conflictoService).registrarNuevos(any(), any(), any(), parametros.capture());
        assertThat(parametros.getValue()).containsEntry("fechaSolicitudDesde", null);
    }

    @Test
    @DisplayName("una cantidad de rango sin unidad se trata como sin rango")
    void noDeberiaLimitarPorFechaCuandoFaltaLaUnidad() {
        FormulaExternaDTO formula = formula("APROBADA");
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formula);
        when(configuracionService.criteriosVigentes()).thenReturn(configuracion(5, null));
        when(motorCoincidencias.terminosDeBusqueda(formula)).thenReturn(List.of());
        when(fuentePatentes.buscarPatentes(anyList(), anyList(), isNull())).thenReturn(List.of());
        when(motorCoincidencias.filtrarConflictos(formula, List.of())).thenReturn(List.of());
        when(conflictoService.registrarNuevos(any(), any(), any(), any())).thenReturn(List.of());

        servicio.buscar("form-001", "ingeniera");

        verify(fuentePatentes).buscarPatentes(anyList(), anyList(), isNull());
    }

    @Test
    @DisplayName("una fórmula DESCONTINUADA se rechaza antes de consultar la fuente")
    void deberiaRechazarUnaFormulaDescontinuada() {
        when(materialesClient.obtenerFormula("form-003")).thenReturn(formula("DESCONTINUADA"));

        assertThatThrownBy(() -> servicio.buscar("form-003", "ingeniera"))
                .isInstanceOf(FormulaDescontinuadaException.class)
                .hasMessage("No se puede buscar patentes para una fórmula DESCONTINUADA");
        verify(fuentePatentes, never()).buscarPatentes(any(), any(), any());
    }

    @Test
    @DisplayName("si Materiales no encuentra la fórmula no se registra historial")
    void deberiaPropagarFormulaNoEncontrada() {
        when(materialesClient.obtenerFormula("form-404")).thenThrow(new FormulaNoEncontradaException());

        assertThatThrownBy(() -> servicio.buscar("form-404", "ingeniera"))
                .isInstanceOf(FormulaNoEncontradaException.class);
        verifyNoInteractions(historialService, conflictoService);
    }

    @Test
    @DisplayName("sin configuración activa se informa el error")
    void deberiaFallarSinConfiguracionActiva() {
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formula("APROBADA"));
        when(configuracionService.criteriosVigentes()).thenThrow(new ConfiguracionNoEncontradaException());

        assertThatThrownBy(() -> servicio.buscar("form-001", "ingeniera"))
                .isInstanceOf(ConfiguracionNoEncontradaException.class);
    }

    @Test
    @DisplayName("si la fuente falla se registra FALLO_TECNICO y se responde 503")
    void deberiaRegistrarFalloTecnicoCuandoLaFuenteFalla() {
        FormulaExternaDTO formula = formula("APROBADA");
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formula);
        when(configuracionService.criteriosVigentes()).thenReturn(configuracion(5, "ANOS"));
        when(motorCoincidencias.terminosDeBusqueda(formula)).thenReturn(List.of("Hierro"));
        when(fuentePatentes.buscarPatentes(anyList(), anyList(), any()))
                .thenThrow(new ServicioNoDisponibleException("sin respuesta"));

        assertThatThrownBy(() -> servicio.buscar("form-001", "ingeniera"))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessage("No se pudo comunicar con la fuente externa de patentes");
        verify(historialService).registrar(eq("form-001"), eq("1"), eq("ingeniera"),
                eq(OrigenRevision.BUSQUEDA_MANUAL), eq(ResultadoRevision.FALLO_TECNICO),
                anyMap(), eq("sin respuesta"), any());
        verifyNoInteractions(conflictoService);
    }

    private static FormulaExternaDTO formula(String estado) {
        return new FormulaExternaDTO("form-001", "Aleación", "Cadena de corte", "1", estado,
                List.of(new ComponenteExternoDTO("SKU-1", "Hierro", BigDecimal.TEN)));
    }

    private static PatenteExternaDTO patente() {
        return new PatenteExternaDTO("PAT-998822", "Aleación", "Aleación metálica", EstadoVigenciaPatente.CONCEDIDA,
                null, null, null, "US", "Resumen", List.of(), List.of("B27B 17/00"));
    }

    private static Configuracion configuracion(Integer cantidad, String unidad) {
        return Configuracion.builder().clasificaciones(CLASIFICACIONES).rangoCantidad(cantidad).rangoUnidad(unidad)
                .activa(true).build();
    }
}
