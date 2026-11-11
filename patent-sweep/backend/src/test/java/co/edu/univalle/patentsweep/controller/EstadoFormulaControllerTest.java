package co.edu.univalle.patentsweep.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.univalle.patentsweep.config.JacksonConfig;
import co.edu.univalle.patentsweep.config.JwtProvider;
import co.edu.univalle.patentsweep.exception.FormulaNoEncontradaException;
import co.edu.univalle.patentsweep.exception.GlobalExceptionHandler;
import co.edu.univalle.patentsweep.model.enums.EstadoFormula;
import co.edu.univalle.patentsweep.service.EstadoFormulaService;
import java.time.LocalDateTime;
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

/** Criterios de aceptación de HU-04 vistos desde el contrato REST: códigos HTTP y mensajes exactos. */
@WebMvcTest(EstadoFormulaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, JacksonConfig.class})
class EstadoFormulaControllerTest {

    private static final String RUTA = "/api/v1/patentes/estado";
    private static final String MENSAJE_FORMATO =
            "El código de la fórmula es obligatorio y debe tener el formato form-NNN";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EstadoFormulaService estadoFormulaService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("200: devuelve el estado calculado de la fórmula para Fabricación")
    void deberiaResponder200ConEstadoDeFormula() throws Exception {
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 19, 10, 30, 0);
        when(estadoFormulaService.consultarEstado("form-001"))
                .thenReturn(new EstadoFormulaService.EstadoCalculado(EstadoFormula.LIBRE, ahora));

        mockMvc.perform(solicitud("form-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formulaId").value("form-001"))
                .andExpect(jsonPath("$.estado").value("LIBRE"))
                .andExpect(jsonPath("$.mensaje").value("La fórmula se encuentra en estado LIBRE para su uso en fabricación."));
    }

    @Test
    @DisplayName("400: formulaId sin el formato form-NNN")
    void deberiaResponder400ConFormatoInvalido() throws Exception {
        mockMvc.perform(solicitud("FORM-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(MENSAJE_FORMATO));
    }

    @Test
    @DisplayName("400: formulaId vacío")
    void deberiaResponder400ConFormulaVacia() throws Exception {
        mockMvc.perform(solicitud(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(MENSAJE_FORMATO));
    }

    @Test
    @DisplayName("404: la fórmula consultada no existe")
    void deberiaResponder404CuandoLaFormulaNoExiste() throws Exception {
        when(estadoFormulaService.consultarEstado("form-404")).thenThrow(new FormulaNoEncontradaException());

        mockMvc.perform(solicitud("form-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("La fórmula consultada no fue encontrada"));
    }

    @Test
    @DisplayName("500: un fallo interno no expone detalles técnicos")
    void deberiaResponder500ConUnFalloInterno() throws Exception {
        when(estadoFormulaService.consultarEstado("form-001")).thenThrow(new IllegalStateException("detalle interno"));

        mockMvc.perform(solicitud("form-001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("No fue posible procesar la solicitud. Intente nuevamente"));
    }

    private static MockHttpServletRequestBuilder solicitud(String formulaId) {
        return get(RUTA)
                .param("formulaId", formulaId)
                .principal(new UsernamePasswordAuthenticationToken("fabricacion", null))
                .accept(MediaType.APPLICATION_JSON);
    }
}
