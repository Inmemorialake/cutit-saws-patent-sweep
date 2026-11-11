package co.edu.univalle.patentsweep.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import co.edu.univalle.patentsweep.repository.ConfiguracionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
    private ConfiguracionRepository configuracionRepository;
    @Mock
    private RevisionService revisionService;

    private BusquedaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new BusquedaService(materialesClient, fuentePatentes, motorCoincidencias,
                configuracionRepository, revisionService, RELOJ);
    }

    @Test
    @DisplayName("Una búsqueda exitosa filtra las candidatas y registra el resultado con el usuario de la sesión")
    void deberiaBuscarFiltrarYRegistrar() {
        // Arrange
        FormulaExternaDTO formula = formula("APROBADA");
        PatenteExternaDTO candidata = patente();
        Conflicto conflicto = Conflicto.builder().conflictoId(100001L).build();
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formula);
        when(configuracionRepository.findByActivaTrue()).thenReturn(Optional.of(configuracion(5, "ANOS")));
        when(motorCoincidencias.terminosDeBusqueda(formula)).thenReturn(List.of("Hierro"));
        when(fuentePatentes.buscarPatentes(List.of("Hierro"), CLASIFICACIONES, LocalDate.of(2021, 10, 27)))
                .thenReturn(List.of(candidata));
        when(motorCoincidencias.filtrarConflictos(formula, List.of(candidata))).thenReturn(List.of(candidata));
        when(revisionService.registrarBusqueda(eq(formula), eq("ingeniera"), eq(List.of(candidata)), anyMap()))
                .thenReturn(List.of(conflicto));

        // Act
        List<Conflicto> resultado = servicio.buscar("form-001", "ingeniera");

        // Assert
        assertThat(resultado).containsExactly(conflicto);
        ArgumentCaptor<Map<String, Object>> parametros = ArgumentCaptor.captor();
        verify(revisionService).registrarBusqueda(eq(formula), eq("ingeniera"), anyList(), parametros.capture());
        assertThat(parametros.getValue())
                .containsEntry("clasificaciones", CLASIFICACIONES)
                .containsEntry("fechaSolicitudDesde", "2021-10-27")
                .containsEntry("terminosBusqueda", List.of("Hierro"))
                .containsEntry("patentesCandidatas", List.of(candidata.patenteId()))
                .containsEntry("formula", formula.foto());
    }

    @Test
    @DisplayName("El rango en MESES se cuenta hacia atrás desde hoy")
    void deberiaCalcularLaFechaDesdeEnMeses() {
        // Arrange
        FormulaExternaDTO formula = formula("APROBADA");
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formula);
        when(configuracionRepository.findByActivaTrue()).thenReturn(Optional.of(configuracion(6, "MESES")));
        when(motorCoincidencias.terminosDeBusqueda(formula)).thenReturn(List.of());
        when(fuentePatentes.buscarPatentes(anyList(), anyList(), any())).thenReturn(List.of());
        when(motorCoincidencias.filtrarConflictos(formula, List.of())).thenReturn(List.of());
        when(revisionService.registrarBusqueda(any(), any(), any(), any())).thenReturn(List.of());

        // Act
        servicio.buscar("form-001", "ingeniera");

        // Assert
        verify(fuentePatentes).buscarPatentes(List.of(), CLASIFICACIONES, LocalDate.of(2026, 4, 27));
    }

    @Test
    @DisplayName("Sin rango configurado la búsqueda no se limita por fecha")
    void noDeberiaLimitarPorFechaCuandoNoHayRango() {
        // Arrange
        FormulaExternaDTO formula = formula("APROBADA");
        Configuracion sinRango = configuracion(null, null);
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formula);
        when(configuracionRepository.findByActivaTrue()).thenReturn(Optional.of(sinRango));
        when(motorCoincidencias.terminosDeBusqueda(formula)).thenReturn(List.of());
        when(fuentePatentes.buscarPatentes(anyList(), anyList(), isNull())).thenReturn(List.of());
        when(motorCoincidencias.filtrarConflictos(formula, List.of())).thenReturn(List.of());
        when(revisionService.registrarBusqueda(any(), any(), any(), any())).thenReturn(List.of());

        // Act
        servicio.buscar("form-001", "ingeniera");

        // Assert
        ArgumentCaptor<Map<String, Object>> parametros = ArgumentCaptor.captor();
        verify(revisionService).registrarBusqueda(any(), any(), any(), parametros.capture());
        assertThat(parametros.getValue()).containsEntry("fechaSolicitudDesde", null);
    }

    @Test
    @DisplayName("Una cantidad de rango sin unidad se trata como sin rango")
    void noDeberiaLimitarPorFechaCuandoFaltaLaUnidad() {
        // Arrange
        FormulaExternaDTO formula = formula("APROBADA");
        Configuracion incompleta = configuracion(5, null);
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formula);
        when(configuracionRepository.findByActivaTrue()).thenReturn(Optional.of(incompleta));
        when(motorCoincidencias.terminosDeBusqueda(formula)).thenReturn(List.of());
        when(fuentePatentes.buscarPatentes(anyList(), anyList(), isNull())).thenReturn(List.of());
        when(motorCoincidencias.filtrarConflictos(formula, List.of())).thenReturn(List.of());
        when(revisionService.registrarBusqueda(any(), any(), any(), any())).thenReturn(List.of());

        // Act
        servicio.buscar("form-001", "ingeniera");

        // Assert
        verify(fuentePatentes).buscarPatentes(anyList(), anyList(), isNull());
    }

    @Test
    @DisplayName("Una fórmula DESCONTINUADA se rechaza antes de buscar")
    void deberiaRechazarUnaFormulaDescontinuada() {
        // Arrange
        when(materialesClient.obtenerFormula("form-003")).thenReturn(formula("DESCONTINUADA"));

        // Act / Assert
        assertThatThrownBy(() -> servicio.buscar("form-003", "ingeniera"))
                .isInstanceOf(FormulaDescontinuadaException.class)
                .hasMessage("No se puede buscar patentes para una fórmula DESCONTINUADA");
        verify(fuentePatentes, never()).buscarPatentes(any(), any(), any());
    }

    @Test
    @DisplayName("Si la fórmula no existe en Materiales el error se propaga sin registrar historial")
    void deberiaPropagarFormulaNoEncontrada() {
        // Arrange
        when(materialesClient.obtenerFormula("form-404")).thenThrow(new FormulaNoEncontradaException());

        // Act / Assert
        assertThatThrownBy(() -> servicio.buscar("form-404", "ingeniera"))
                .isInstanceOf(FormulaNoEncontradaException.class);
        verify(revisionService, never()).registrarFalloTecnico(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Sin configuración activa se informa el error")
    void deberiaFallarSinConfiguracionActiva() {
        // Arrange
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formula("APROBADA"));
        when(configuracionRepository.findByActivaTrue()).thenReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> servicio.buscar("form-001", "ingeniera"))
                .isInstanceOf(ConfiguracionNoEncontradaException.class);
    }

    @Test
    @DisplayName("Si la fuente falla se registra FALLO_TECNICO y se responde servicio no disponible")
    void deberiaRegistrarFalloTecnicoCuandoLaFuenteFalla() {
        // Arrange
        FormulaExternaDTO formula = formula("APROBADA");
        when(materialesClient.obtenerFormula("form-001")).thenReturn(formula);
        when(configuracionRepository.findByActivaTrue()).thenReturn(Optional.of(configuracion(5, "ANOS")));
        when(motorCoincidencias.terminosDeBusqueda(formula)).thenReturn(List.of("Hierro"));
        when(fuentePatentes.buscarPatentes(anyList(), anyList(), any()))
                .thenThrow(new ServicioNoDisponibleException("sin respuesta"));

        // Act / Assert
        assertThatThrownBy(() -> servicio.buscar("form-001", "ingeniera"))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessage("No se pudo comunicar con la fuente externa de patentes");
        verify(revisionService).registrarFalloTecnico(eq(formula), eq("ingeniera"), eq("sin respuesta"), anyMap());
        verify(revisionService, never()).registrarBusqueda(any(), any(), any(), any());
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

    private static Map<String, Object> anyMap() {
        return org.mockito.ArgumentMatchers.anyMap();
    }
}
