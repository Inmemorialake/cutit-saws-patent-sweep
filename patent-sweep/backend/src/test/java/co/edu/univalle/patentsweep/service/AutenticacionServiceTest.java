package co.edu.univalle.patentsweep.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import co.edu.univalle.patentsweep.config.JwtProvider;
import co.edu.univalle.patentsweep.model.enums.Rol;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class AutenticacionServiceTest {

    private static final String SECRETO = "clave-de-prueba-de-al-menos-32-caracteres";

    @Mock
    private AuthenticationManager authenticationManager;

    private JwtProvider jwtProvider;
    private AutenticacionService servicio;

    @BeforeEach
    void preparar() {
        jwtProvider = new JwtProvider(SECRETO, 60,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        servicio = new AutenticacionService(authenticationManager, jwtProvider);
    }

    @Test
    @DisplayName("autentica al usuario y emite un JWT con su rol y vencimiento")
    void deberiaEmitirTokenParaUsuarioAutenticado() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        "demo", null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))));

        AutenticacionService.ResultadoAutenticacion resultado = servicio.autenticar("demo", "clave");

        assertThat(jwtProvider.esValido(resultado.accessToken())).isTrue();
        assertThat(jwtProvider.obtenerUsuario(resultado.accessToken())).isEqualTo("demo");
        assertThat(jwtProvider.obtenerRol(resultado.accessToken())).isEqualTo("ADMINISTRADOR");
        assertThat(resultado.expiresIn()).isEqualTo(3600);
        assertThat(resultado.rol()).isEqualTo(Rol.ADMINISTRADOR);
    }

    @Test
    @DisplayName("propaga credenciales inválidas para responder 401")
    void deberiaPropagarCredencialesInvalidas() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("credenciales inválidas"));

        assertThatThrownBy(() -> servicio.autenticar("demo", "incorrecta"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("falla explícitamente si la autenticación no incluye un rol")
    void deberiaFallarSiElUsuarioAutenticadoNoTieneRol() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("demo", null, List.of()));

        assertThatThrownBy(() -> servicio.autenticar("demo", "clave"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("El usuario autenticado no tiene un rol asignado");
    }
}
