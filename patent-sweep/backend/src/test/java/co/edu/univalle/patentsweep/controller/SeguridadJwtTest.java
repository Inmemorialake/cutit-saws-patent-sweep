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
import co.edu.univalle.patentsweep.model.enums.EstadoFormula;
import co.edu.univalle.patentsweep.model.enums.Rol;
import co.edu.univalle.patentsweep.service.AutenticacionService;
import co.edu.univalle.patentsweep.service.BusquedaService;
import co.edu.univalle.patentsweep.service.EstadoFormulaService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({EstadoFormulaController.class, BusquedaController.class, AutenticacionController.class})
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtProvider.class, ClockConfig.class,
        ConflictoMapper.class})
class SeguridadJwtTest {

    private static final int PUERTO_MANAGEMENT = 9091;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private EstadoFormulaService estadoFormulaService;

    @MockitoBean
    private BusquedaService busquedaService;

    @MockitoBean
    private AutenticacionService autenticacionService;

    @Test
    @DisplayName("401: exige JWT para consultar el estado de una fórmula")
    void deberiaRechazarPeticionSinToken() throws Exception {
        mockMvc.perform(get("/api/v1/patentes/estado").param("formulaId", "form-001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("200: acepta un JWT válido")
    void deberiaAceptarTokenValido() throws Exception {
        when(estadoFormulaService.consultarEstado("form-001"))
                .thenReturn(new EstadoFormulaService.EstadoCalculado(EstadoFormula.LIBRE, null));
        String token = jwtProvider.generarToken("servicio", Rol.SERVICIO);

        mockMvc.perform(get("/api/v1/patentes/estado").param("formulaId", "form-001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formulaId").value("form-001"))
                .andExpect(jsonPath("$.estado").value("LIBRE"));
    }

    @Test
    @DisplayName("401: exige JWT para buscar conflictos")
    void deberiaRechazarBusquedaSinToken() throws Exception {
        mockMvc.perform(post("/api/v1/patentes/busqueda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formulaId\":\"form-001\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("403: el rol SERVICIO no puede buscar conflictos")
    void deberiaRechazarBusquedaConRolServicio() throws Exception {
        String token = jwtProvider.generarToken("servicio", Rol.SERVICIO);

        mockMvc.perform(post("/api/v1/patentes/busqueda")
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

        mockMvc.perform(post("/api/v1/patentes/busqueda")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formulaId\":\"form-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formulaId").value("form-001"));
    }

    @Test
    @DisplayName("200: permite iniciar sesión sin JWT y devuelve el token")
    void deberiaIniciarSesionSinToken() throws Exception {
        when(autenticacionService.autenticar("admin", "clave"))
                .thenReturn(new AutenticacionService.ResultadoAutenticacion(
                        "jwt-demo", 3600, "admin", Rol.ADMINISTRADOR));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"clave\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-demo"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("400: rechaza credenciales vacías")
    void deberiaRechazarCredencialesVacias() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("401: devuelve un error genérico con credenciales inválidas")
    void deberiaRechazarCredencialesInvalidas() throws Exception {
        when(autenticacionService.autenticar("admin", "incorrecta"))
                .thenThrow(new BadCredentialsException("detalle que no debe exponerse"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciales inválidas"));
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
