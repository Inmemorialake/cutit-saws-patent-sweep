package co.edu.univalle.patentsweep.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.univalle.patentsweep.config.JwtProvider;
import co.edu.univalle.patentsweep.exception.GlobalExceptionHandler;
import co.edu.univalle.patentsweep.model.enums.Rol;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    private static final String RUTA = "/api/v1/auth/login";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("200: credenciales válidas devuelven el token Bearer y el rol")
    void deberiaEmitirElTokenConCredencialesValidas() throws Exception {
        // Arrange
        when(authenticationManager.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))));
        when(jwtProvider.generarToken("admin", Rol.ADMINISTRADOR)).thenReturn("token-firmado");
        when(jwtProvider.getExpiracionMinutos()).thenReturn(60L);

        // Act / Assert
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"admin\",\"clave\":\"clave-valida\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-firmado"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEnMinutos").value(60))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("401: credenciales inválidas")
    void deberiaResponder401ConCredencialesInvalidas() throws Exception {
        // Arrange
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("mal"));

        // Act / Assert
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"admin\",\"clave\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("400: falta el usuario")
    void deberiaResponder400SinUsuario() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"\",\"clave\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El usuario es obligatorio"));
    }
}
