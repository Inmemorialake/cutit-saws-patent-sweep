package co.edu.univalle.patentsweep.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.univalle.patentsweep.config.ClockConfig;
import co.edu.univalle.patentsweep.config.JwtProvider;
import co.edu.univalle.patentsweep.config.SecurityConfig;
import co.edu.univalle.patentsweep.exception.GlobalExceptionHandler;
import co.edu.univalle.patentsweep.model.Patente;
import co.edu.univalle.patentsweep.model.enums.EstadoVigenciaPatente;
import co.edu.univalle.patentsweep.model.enums.Rol;
import co.edu.univalle.patentsweep.service.PatenteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PatenteController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtProvider.class, ClockConfig.class})
class SeguridadJwtTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private PatenteService patenteService;

    @Test
    @DisplayName("401: exige JWT para consultar el estado de una patente")
    void deberiaRechazarPeticionSinToken() throws Exception {
        mockMvc.perform(get("/api/v1/patentes/estado/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("200: acepta un JWT válido")
    void deberiaAceptarTokenValido() throws Exception {
        when(patenteService.obtenerPorId(1L)).thenReturn(Patente.builder()
                .patenteId(1L)
                .codigoPatente("PAT-123456")
                .titulo("Cuchilla láser")
                .estadoVigencia(EstadoVigenciaPatente.EN_TRAMITE)
                .build());
        String token = jwtProvider.generarToken("servicio", Rol.SERVICIO);

        mockMvc.perform(get("/api/v1/patentes/estado/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patenteId").value(1));
    }
}
