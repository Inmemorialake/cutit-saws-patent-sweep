package co.edu.univalle.patentsweep.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.univalle.patentsweep.config.ClockConfig;
import co.edu.univalle.patentsweep.config.JwtProvider;
import co.edu.univalle.patentsweep.config.SecurityConfig;
import co.edu.univalle.patentsweep.controller.mapper.ConflictoMapper;
import co.edu.univalle.patentsweep.exception.GlobalExceptionHandler;
import co.edu.univalle.patentsweep.model.Patente;
import co.edu.univalle.patentsweep.model.enums.EstadoVigenciaPatente;
import co.edu.univalle.patentsweep.model.enums.Rol;
import co.edu.univalle.patentsweep.service.BusquedaService;
import co.edu.univalle.patentsweep.service.PatenteService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({PatenteController.class, BusquedaController.class})
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtProvider.class, ClockConfig.class,
        ConflictoMapper.class})
class SeguridadJwtTest {

    private static final int PUERTO_MANAGEMENT = 9091;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private PatenteService patenteService;

    @MockitoBean
    private BusquedaService busquedaService;

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

    @Test
    @DisplayName("401: exige JWT para buscar conflictos")
    void deberiaRechazarBusquedaSinToken() throws Exception {
        mockMvc.perform(post("/api/v1/patentes/buscar-conflictos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formulaId\":\"form-001\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("403: el rol SERVICIO no puede buscar conflictos")
    void deberiaRechazarBusquedaConRolServicio() throws Exception {
        String token = jwtProvider.generarToken("servicio", Rol.SERVICIO);

        mockMvc.perform(post("/api/v1/patentes/buscar-conflictos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formulaId\":\"form-001\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("200: el rol ADMINISTRADOR busca conflictos con su propio usuario")
    void deberiaAceptarBusquedaConRolAdministrador() throws Exception {
        when(busquedaService.buscar("form-001", "ingeniera")).thenReturn(List.of());
        String token = jwtProvider.generarToken("ingeniera", Rol.ADMINISTRADOR);

        mockMvc.perform(post("/api/v1/patentes/buscar-conflictos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formulaId\":\"form-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formulaId").value("form-001"));
    }

    @Test
    @DisplayName("Métricas: por el puerto de management la seguridad las deja pasar sin JWT")
    void deberiaPermitirMetricasPorPuertoDeManagement() throws Exception {
        mockMvc.perform(get("/actuator/prometheus").with(peticion -> {
                    peticion.setLocalPort(PUERTO_MANAGEMENT);
                    return peticion;
                }))
                // En el test no hay actuator: basta con que la cadena de seguridad no responda 401/403.
                .andExpect(resultado -> assertThat(resultado.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    @DisplayName("401: las métricas por el puerto público exigen JWT")
    void deberiaRechazarMetricasPorPuertoPublico() throws Exception {
        mockMvc.perform(get("/actuator/prometheus").with(peticion -> {
                    peticion.setLocalPort(8085);
                    return peticion;
                }))
                .andExpect(status().isUnauthorized());
    }
}
