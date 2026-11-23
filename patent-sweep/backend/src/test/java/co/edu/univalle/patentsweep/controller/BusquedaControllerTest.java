package co.edu.univalle.patentsweep.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import co.edu.univalle.patentsweep.config.JacksonConfig;
import co.edu.univalle.patentsweep.config.JwtProvider;
import co.edu.univalle.patentsweep.controller.mapper.ConflictoMapper;
import co.edu.univalle.patentsweep.exception.FormulaDescontinuadaException;
import co.edu.univalle.patentsweep.exception.FormulaNoEncontradaException;
import co.edu.univalle.patentsweep.exception.GlobalExceptionHandler;
import co.edu.univalle.patentsweep.exception.ServicioNoDisponibleException;
import co.edu.univalle.patentsweep.model.Conflicto;
import co.edu.univalle.patentsweep.model.Patente;
import co.edu.univalle.patentsweep.model.enums.EstadoVigenciaPatente;
import co.edu.univalle.patentsweep.service.BusquedaService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Criterios de aceptación de HU-02 vistos desde el contrato REST: códigos HTTP y mensajes exactos. */
@WebMvcTest(BusquedaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ConflictoMapper.class, GlobalExceptionHandler.class, JacksonConfig.class})
class BusquedaControllerTest {

    private static final String RUTA = "/api/v1/patentes/busqueda";
    private static final String MENSAJE_FORMATO =
            "El código de la fórmula es obligatorio y debe tener el formato form-NNN";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BusquedaService busquedaService;

    // El filtro JWT forma parte del slice web; su dependencia se simula.
    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("200: devuelve la tabla de conflictos con id de 6 dígitos y fechas dd/mm/aaaa")
    void deberiaResponder200ConLosConflictosDeLaBusqueda() throws Exception {
        // Arrange
        Patente patente = Patente.builder().codigoPatente("PAT-998822").titulo("Aleación para cadenas de corte")
                .tipoProducto("Aleación metálica").estadoVigencia(EstadoVigenciaPatente.CONCEDIDA)
                .fechaConcesion(LocalDate.of(2025, 6, 22)).build();
        when(busquedaService.buscar(eq("form-001"), anyString()))
                .thenReturn(List.of(Conflicto.builder().conflictoId(100001L).patente(patente).build()));

        // Act / Assert
        mockMvc.perform(solicitud("{\"formulaId\":\"form-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formulaId").value("form-001"))
                .andExpect(jsonPath("$.conflictos[0].conflictoId").value("100001"))
                .andExpect(jsonPath("$.conflictos[0].tituloPatente").value("Aleación para cadenas de corte"))
                .andExpect(jsonPath("$.conflictos[0].tipoProducto").value("Aleación metálica"))
                .andExpect(jsonPath("$.conflictos[0].estadoPatente").value("CONCEDIDA"))
                .andExpect(jsonPath("$.conflictos[0].fechaConcesion").value("22/06/2025"));
    }

    @Test
    @DisplayName("200: sin coincidencias devuelve la lista vacía y el mensaje del PB")
    void deberiaResponder200ConListaVaciaSinCoincidencias() throws Exception {
        // Arrange
        when(busquedaService.buscar("form-002", "ingeniera")).thenReturn(List.of());

        // Act / Assert
        mockMvc.perform(solicitud("{\"formulaId\":\"form-002\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflictos").isEmpty())
                .andExpect(jsonPath("$.mensaje").value("No se detectaron conflictos relacionados con su búsqueda"));
    }

    @Test
    @DisplayName("400: formulaId sin el formato form-NNN")
    void deberiaResponder400ConFormatoInvalido() throws Exception {
        mockMvc.perform(solicitud("{\"formulaId\":\"FORM-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(MENSAJE_FORMATO));
    }

    @Test
    @DisplayName("400: formulaId vacío")
    void deberiaResponder400ConFormulaVacia() throws Exception {
        mockMvc.perform(solicitud("{\"formulaId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(MENSAJE_FORMATO));
    }

    @Test
    @DisplayName("400: cuerpo ausente o ilegible")
    void deberiaResponder400ConCuerpoIlegible() throws Exception {
        mockMvc.perform(solicitud("no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El cuerpo de la petición no es válido"));
    }

    @Test
    @DisplayName("404: la fórmula no existe en Materiales")
    void deberiaResponder404CuandoLaFormulaNoExiste() throws Exception {
        // Arrange
        when(busquedaService.buscar(eq("form-404"), anyString())).thenThrow(new FormulaNoEncontradaException());
        // Act / Assert
        mockMvc.perform(solicitud("{\"formulaId\":\"form-404\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("La fórmula consultada no fue encontrada"));
    }

    @Test
    @DisplayName("409: la fórmula está DESCONTINUADA")
    void deberiaResponder409CuandoLaFormulaEstaDescontinuada() throws Exception {
        // Arrange
        when(busquedaService.buscar(eq("form-003"), anyString())).thenThrow(new FormulaDescontinuadaException());
        // Act / Assert
        mockMvc.perform(solicitud("{\"formulaId\":\"form-003\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No se puede buscar patentes para una fórmula DESCONTINUADA"));
    }

    @Test
    @DisplayName("503: Materiales o la fuente no responden tras los reintentos")
    void deberiaResponder503CuandoUnServicioExternoNoResponde() throws Exception {
        // Arrange
        when(busquedaService.buscar(eq("form-001"), anyString())).thenThrow(new ServicioNoDisponibleException("timeout"));
        // Act / Assert
        mockMvc.perform(solicitud("{\"formulaId\":\"form-001\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("No fue posible realizar la consulta. Intente nuevamente"));
    }

    @Test
    @DisplayName("500: un fallo interno no expone detalles técnicos")
    void deberiaResponder500ConUnFalloInterno() throws Exception {
        // Arrange
        when(busquedaService.buscar(eq("form-001"), anyString())).thenThrow(new IllegalStateException("detalle interno"));
        // Act / Assert
        mockMvc.perform(solicitud("{\"formulaId\":\"form-001\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("No fue posible procesar la solicitud. Intente nuevamente"));
    }

    private static MockHttpServletRequestBuilder solicitud(String cuerpo) {
        return post(RUTA)
                .principal(new UsernamePasswordAuthenticationToken("ingeniera", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
    }
}
