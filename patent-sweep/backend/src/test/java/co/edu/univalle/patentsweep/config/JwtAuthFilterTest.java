package co.edu.univalle.patentsweep.config;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.univalle.patentsweep.model.enums.Rol;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthFilterTest {

    private static final String SECRETO = "clave-solo-para-pruebas-unitarias-0123456789-abc";

    private final JwtProvider proveedor = new JwtProvider(SECRETO, 60,
            Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZoneOffset.UTC));
    private final JwtAuthFilter filtro = new JwtAuthFilter(proveedor);

    @BeforeEach
    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Un token válido autentica la petición con el rol del token")
    void deberiaAutenticarConUnTokenValido() throws Exception {
        // Arrange
        String token = proveedor.generarToken("admin", Rol.ADMINISTRADOR);

        // Act
        ejecutar("Bearer " + token);

        // Assert
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        assertThat(autenticacion.getName()).isEqualTo("admin");
        assertThat(autenticacion.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMINISTRADOR");
    }

    @Test
    @DisplayName("Un token inválido no autentica y la petición sigue su curso")
    void noDeberiaAutenticarConUnTokenInvalido() throws Exception {
        MockFilterChain cadena = ejecutar("Bearer token-invalido");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(cadena.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("Sin encabezado o con otro esquema no se autentica")
    void noDeberiaAutenticarSinBearer() throws Exception {
        ejecutar(null);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        ejecutar("Basic dXN1YXJpbzpjbGF2ZQ==");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockFilterChain ejecutar(String encabezado) throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        if (encabezado != null) {
            peticion.addHeader(HttpHeaders.AUTHORIZATION, encabezado);
        }
        MockFilterChain cadena = new MockFilterChain();
        filtro.doFilter(peticion, new MockHttpServletResponse(), cadena);
        return cadena;
    }
}
